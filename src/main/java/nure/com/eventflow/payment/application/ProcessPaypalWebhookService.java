package nure.com.eventflow.payment.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import nure.com.eventflow.checkout.domain.Order;
import nure.com.eventflow.checkout.infra.OrderRepository;
import nure.com.eventflow.notifications.domain.OrderPaidEvent;
import nure.com.eventflow.payment.domain.Payment;
import nure.com.eventflow.payment.domain.PaymentAttempt;
import nure.com.eventflow.payment.domain.PaymentAttemptType;
import nure.com.eventflow.payment.domain.PaymentPolicy;
import nure.com.eventflow.payment.domain.PaymentStatus;
import nure.com.eventflow.payment.domain.PaymentTransaction;
import nure.com.eventflow.payment.domain.PaymentTransactionType;
import nure.com.eventflow.payment.domain.PaymentWebhookVerificationRequest;
import nure.com.eventflow.payment.domain.PaymentWebhookVerificationResult;
import nure.com.eventflow.payment.domain.PaymentWebhookVerifier;
import nure.com.eventflow.payment.domain.ProviderWebhookLog;
import nure.com.eventflow.payment.infra.PaymentRepository;
import nure.com.eventflow.payment.infra.PaymentTransactionRepository;
import nure.com.eventflow.payment.infra.ProviderWebhookLogRepository;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessPaypalWebhookService {

    private static final String PROVIDER_NAME = "PAYPAL";

    private final ProviderWebhookLogRepository webhookLogRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderRepository orderRepository;
    private final PaymentWebhookVerifier webhookVerifier;
    private final PaymentPolicy paymentPolicy;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ProcessPaypalWebhookService(
            ProviderWebhookLogRepository webhookLogRepository,
            PaymentRepository paymentRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            OrderRepository orderRepository,
            PaymentWebhookVerifier webhookVerifier,
            PaymentPolicy paymentPolicy,
            ApplicationEventPublisher applicationEventPublisher,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.webhookLogRepository = webhookLogRepository;
        this.paymentRepository = paymentRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.orderRepository = orderRepository;
        this.webhookVerifier = webhookVerifier;
        this.paymentPolicy = paymentPolicy;
        this.applicationEventPublisher = applicationEventPublisher;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public void process(Map<String, String> headers, String body) {
        Instant now = clock.instant();
        PaypalWebhookEvent event = parseEvent(body);
        ProviderWebhookLog log = webhookLogRepository.findByProviderNameAndEventId(PROVIDER_NAME, event.eventId())
                .orElseGet(() -> webhookLogRepository.save(new ProviderWebhookLog(
                        PROVIDER_NAME,
                        event.eventId(),
                        event.eventType(),
                        event.resourceId(),
                        body,
                        now)));
        if (log.isFinished()) {
            return;
        }

        PaymentWebhookVerificationResult verification = webhookVerifier.verify(new PaymentWebhookVerificationRequest(headers, body));
        if (!verification.verified()) {
            log.markVerificationFailed(verification.failureReason(), now);
            webhookLogRepository.save(log);
            return;
        }

        log.markVerified();
        try {
            handleVerifiedEvent(event, body, log, now);
        } catch (RuntimeException exception) {
            log.markFailed(exception.getMessage(), now);
        }
        webhookLogRepository.save(log);
    }

    private void handleVerifiedEvent(PaypalWebhookEvent event, String body, ProviderWebhookLog log, Instant now) {
        if ("PAYMENT.CAPTURE.COMPLETED".equals(event.eventType())) {
            completePayment(event, body, log, now);
            return;
        }
        if (isFailureEvent(event.eventType())) {
            failPayment(event, body, log, now);
            return;
        }
        log.markIgnored("PayPal event is recorded but does not change EventFlow payment state.", now);
    }

    private void completePayment(PaypalWebhookEvent event, String body, ProviderWebhookLog log, Instant now) {
        Payment payment = findPayment(event, log, now);
        if (payment == null) {
            return;
        }
        if (payment.isPaid()) {
            log.markProcessed(now);
            return;
        }
        if (!payment.isPending()) {
            log.markIgnored("Payment is not pending.", now);
            return;
        }

        Order order = payment.getOrder();
        if (!order.isPendingPayment()) {
            log.markIgnored("Order is not pending payment.", now);
            return;
        }
        if (event.amount() != null && event.currency() != null) {
            paymentPolicy.assertAmountsMatch(order, event.amount(), event.currency());
        }

        String transactionReference = event.resourceId() == null ? payment.getProviderReference() : event.resourceId();
        payment.addAttempt(new PaymentAttempt(
                PaymentAttemptType.WEBHOOK,
                PaymentStatus.PAID,
                PROVIDER_NAME,
                transactionReference,
                event.amount() == null ? payment.getAmount() : event.amount(),
                event.currency() == null ? payment.getCurrency() : event.currency(),
                null));
        paymentTransactionRepository.save(new PaymentTransaction(
                payment,
                PaymentTransactionType.WEBHOOK,
                PROVIDER_NAME,
                transactionReference,
                event.amount() == null ? payment.getAmount() : event.amount(),
                event.currency() == null ? payment.getCurrency() : event.currency(),
                PaymentStatus.PAID.name(),
                body,
                now));
        payment.markPaid(now);
        order.markPaid();
        orderRepository.save(order);
        paymentRepository.save(payment);
        applicationEventPublisher.publishEvent(new OrderPaidEvent(order.getId(), payment.getCustomer().getId()));
        log.markProcessed(now);
    }

    private void failPayment(PaypalWebhookEvent event, String body, ProviderWebhookLog log, Instant now) {
        Payment payment = findPayment(event, log, now);
        if (payment == null) {
            return;
        }
        if (!payment.isPending()) {
            log.markIgnored("Payment is not pending.", now);
            return;
        }

        String reason = "PayPal event " + event.eventType() + " changed payment state to failed.";
        String transactionReference = event.resourceId() == null ? payment.getProviderReference() : event.resourceId();
        payment.addAttempt(new PaymentAttempt(
                PaymentAttemptType.WEBHOOK,
                PaymentStatus.FAILED,
                PROVIDER_NAME,
                transactionReference,
                event.amount() == null ? payment.getAmount() : event.amount(),
                event.currency() == null ? payment.getCurrency() : event.currency(),
                reason));
        paymentTransactionRepository.save(new PaymentTransaction(
                payment,
                PaymentTransactionType.WEBHOOK,
                PROVIDER_NAME,
                transactionReference,
                event.amount() == null ? payment.getAmount() : event.amount(),
                event.currency() == null ? payment.getCurrency() : event.currency(),
                PaymentStatus.FAILED.name(),
                body,
                now));
        payment.markFailed(reason, now);
        paymentRepository.save(payment);
        log.markProcessed(now);
    }

    private Payment findPayment(PaypalWebhookEvent event, ProviderWebhookLog log, Instant now) {
        String paypalOrderId = event.paypalOrderId();
        if (paypalOrderId == null || paypalOrderId.isBlank()) {
            log.markIgnored("PayPal order id was not present in webhook payload.", now);
            return null;
        }
        return paymentRepository.findByProviderNameAndProviderReference(PROVIDER_NAME, paypalOrderId)
                .orElseGet(() -> {
                    log.markIgnored("No EventFlow payment matches PayPal order " + paypalOrderId + ".", now);
                    return null;
                });
    }

    private boolean isFailureEvent(String eventType) {
        return "PAYMENT.CAPTURE.DECLINED".equals(eventType)
                || "PAYMENT.CAPTURE.REVERSED".equals(eventType)
                || "CHECKOUT.PAYMENT-APPROVAL.REVERSED".equals(eventType);
    }

    private PaypalWebhookEvent parseEvent(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode resource = root.path("resource");
            String eventId = requiredText(root, "id");
            String eventType = requiredText(root, "event_type");
            String resourceId = optionalText(resource, "id");
            String paypalOrderId = optionalText(resource.path("supplementary_data").path("related_ids"), "order_id");
            if (paypalOrderId == null && eventType.startsWith("CHECKOUT.ORDER")) {
                paypalOrderId = resourceId;
            }
            BigDecimal amount = null;
            String currency = null;
            JsonNode amountNode = resource.path("amount");
            if (!amountNode.isMissingNode() && amountNode.hasNonNull("value")) {
                amount = new BigDecimal(amountNode.path("value").asText());
                currency = optionalText(amountNode, "currency_code");
            }
            return new PaypalWebhookEvent(eventId, eventType, resourceId, paypalOrderId, amount, currency);
        } catch (JsonProcessingException | NumberFormatException exception) {
            throw new BusinessRuleViolationException("PayPal webhook payload could not be parsed.");
        }
    }

    private String requiredText(JsonNode node, String fieldName) {
        String value = optionalText(node, fieldName);
        if (value == null) {
            throw new BusinessRuleViolationException("PayPal webhook field is missing: " + fieldName + ".");
        }
        return value;
    }

    private String optionalText(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || !node.hasNonNull(fieldName)) {
            return null;
        }
        String value = node.path(fieldName).asText();
        return value == null || value.isBlank() ? null : value;
    }

    private record PaypalWebhookEvent(
            String eventId,
            String eventType,
            String resourceId,
            String paypalOrderId,
            BigDecimal amount,
            String currency
    ) {
    }
}
