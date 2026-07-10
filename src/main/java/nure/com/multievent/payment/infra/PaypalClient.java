package nure.com.multievent.payment.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import nure.com.multievent.payment.domain.Payment;
import nure.com.multievent.payment.domain.PaymentGatewayResult;
import nure.com.multievent.payment.domain.PaymentGatewaySession;
import nure.com.multievent.payment.domain.PaymentGatewayStartRequest;
import nure.com.multievent.payment.domain.PaymentStatus;
import nure.com.multievent.payment.domain.PaymentWebhookVerificationResult;
import nure.com.multievent.shared.exception.BusinessRuleViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PaypalClient {

    private static final String PROVIDER_NAME = "PAYPAL";

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final PaypalPaymentProperties properties;

    public PaypalClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            PaypalPaymentProperties properties
    ) {
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    PaymentGatewaySession createOrder(PaymentGatewayStartRequest request) {
        assertConfiguredForPayment();
        assertCurrencySupported(request.currency());

        String response = restClient().post()
                .uri("/v2/checkout/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(createOrderBody(request))
                .retrieve()
                .body(String.class);

        JsonNode root = readTree(response);
        String paypalOrderId = requiredText(root, "id", "PayPal order id was missing.");
        String approvalUrl = approvalUrl(root);
        if (approvalUrl == null) {
            throw new BusinessRuleViolationException("PayPal approval link was not returned.");
        }
        return new PaymentGatewaySession(PROVIDER_NAME, paypalOrderId, approvalUrl, response);
    }

    PaymentGatewayResult captureOrder(Payment payment) {
        assertConfiguredForPayment();

        String response = restClient().post()
                .uri("/v2/checkout/orders/{orderId}/capture", payment.getProviderReference())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of())
                .retrieve()
                .body(String.class);

        JsonNode root = readTree(response);
        String status = requiredText(root, "status", "PayPal capture status was missing.");
        JsonNode capture = firstCapture(root);
        String captureId = capture == null ? payment.getProviderReference() : requiredText(capture, "id", "PayPal capture id was missing.");
        String captureStatus = capture == null ? status : requiredText(capture, "status", "PayPal capture status was missing.");

        if ("COMPLETED".equalsIgnoreCase(captureStatus) || "COMPLETED".equalsIgnoreCase(status)) {
            return new PaymentGatewayResult(PaymentStatus.PAID, null, captureId, response);
        }
        if ("PENDING".equalsIgnoreCase(captureStatus) || "PENDING".equalsIgnoreCase(status)) {
            return new PaymentGatewayResult(PaymentStatus.PENDING, "PayPal capture is pending.", captureId, response);
        }
        return new PaymentGatewayResult(PaymentStatus.FAILED, "PayPal capture status: " + captureStatus, captureId, response);
    }

    PaymentWebhookVerificationResult verifyWebhookSignature(Map<String, String> headers, String body) {
        if (isBlank(properties.getWebhookId())) {
            return new PaymentWebhookVerificationResult(false, "PAYPAL_WEBHOOK_ID is not configured.");
        }
        assertConfiguredForPayment();

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("auth_algo", requiredHeader(headers, "PAYPAL-AUTH-ALGO"));
        request.put("cert_url", requiredHeader(headers, "PAYPAL-CERT-URL"));
        request.put("transmission_id", requiredHeader(headers, "PAYPAL-TRANSMISSION-ID"));
        request.put("transmission_sig", requiredHeader(headers, "PAYPAL-TRANSMISSION-SIG"));
        request.put("transmission_time", requiredHeader(headers, "PAYPAL-TRANSMISSION-TIME"));
        request.put("webhook_id", properties.getWebhookId());
        request.put("webhook_event", readJsonObject(body));

        String response = restClient().post()
                .uri("/v1/notifications/verify-webhook-signature")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);

        String verificationStatus = readTree(response).path("verification_status").asText("");
        boolean verified = "SUCCESS".equalsIgnoreCase(verificationStatus);
        return new PaymentWebhookVerificationResult(
                verified,
                verified ? null : "PayPal webhook verification status: " + verificationStatus);
    }

    private String accessToken() {
        String response = restClient().post()
                .uri("/v1/oauth2/token")
                .headers(headers -> headers.setBasicAuth(properties.getClientId(), properties.getClientSecret()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials")
                .retrieve()
                .body(String.class);
        return requiredText(readTree(response), "access_token", "PayPal access token was missing.");
    }

    private Map<String, Object> createOrderBody(PaymentGatewayStartRequest request) {
        Map<String, Object> amount = new LinkedHashMap<>();
        amount.put("currency_code", request.currency().toUpperCase(Locale.ROOT));
        amount.put("value", moneyValue(request.amount()));

        Map<String, Object> purchaseUnit = new LinkedHashMap<>();
        purchaseUnit.put("custom_id", request.orderId().toString());
        purchaseUnit.put("amount", amount);

        Map<String, Object> applicationContext = new LinkedHashMap<>();
        applicationContext.put("brand_name", "MultiEvent");
        applicationContext.put("user_action", "PAY_NOW");
        applicationContext.put("return_url", publicUrl("/checkout/orders/" + request.orderId() + "/payment/paypal/return"));
        applicationContext.put("cancel_url", publicUrl("/checkout/orders/" + request.orderId() + "/payment/paypal/cancel"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("intent", "CAPTURE");
        body.put("purchase_units", List.of(purchaseUnit));
        body.put("application_context", applicationContext);
        return body;
    }

    private RestClient restClient() {
        return restClientBuilder.baseUrl(properties.apiBaseUrl()).build();
    }

    private String publicUrl(String path) {
        String baseUrl = properties.getPublicBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BusinessRuleViolationException("APP_PUBLIC_BASE_URL is not configured.");
        }
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBaseUrl + path;
    }

    private String approvalUrl(JsonNode root) {
        JsonNode links = root.path("links");
        if (!links.isArray()) {
            return null;
        }
        for (JsonNode link : links) {
            if ("approve".equalsIgnoreCase(link.path("rel").asText())) {
                return link.path("href").asText(null);
            }
        }
        return null;
    }

    private JsonNode firstCapture(JsonNode root) {
        JsonNode purchaseUnits = root.path("purchase_units");
        if (!purchaseUnits.isArray() || purchaseUnits.isEmpty()) {
            return null;
        }
        JsonNode captures = purchaseUnits.get(0).path("payments").path("captures");
        if (!captures.isArray() || captures.isEmpty()) {
            return null;
        }
        return captures.get(0);
    }

    private JsonNode readTree(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new BusinessRuleViolationException("PayPal response was not valid JSON.");
        }
    }

    private Object readJsonObject(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessRuleViolationException("PayPal webhook payload was not valid JSON.");
        }
    }

    private String requiredText(JsonNode node, String fieldName, String message) {
        String value = node.path(fieldName).asText(null);
        if (isBlank(value)) {
            throw new BusinessRuleViolationException(message);
        }
        return value;
    }

    private String requiredHeader(Map<String, String> headers, String headerName) {
        return headers.entrySet().stream()
                .filter(entry -> headerName.equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue)
                .filter(value -> !isBlank(value))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException(headerName + " header is required."));
    }

    private String moneyValue(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private void assertConfiguredForPayment() {
        if (isBlank(properties.getClientId()) || isBlank(properties.getClientSecret())) {
            throw new BusinessRuleViolationException("PayPal client id and secret must be configured.");
        }
    }

    private void assertCurrencySupported(String currency) {
        if (!properties.getCurrency().equalsIgnoreCase(currency)) {
            throw new BusinessRuleViolationException(
                    "PayPal is configured for " + properties.getCurrency() + ", but order currency is " + currency + ".");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
