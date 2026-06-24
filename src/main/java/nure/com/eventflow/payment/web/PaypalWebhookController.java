package nure.com.eventflow.payment.web;

import java.util.Map;
import nure.com.eventflow.payment.application.ProcessPaypalWebhookService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments/paypal/webhook")
public class PaypalWebhookController {

    private final ProcessPaypalWebhookService processPaypalWebhookService;

    public PaypalWebhookController(ProcessPaypalWebhookService processPaypalWebhookService) {
        this.processPaypalWebhookService = processPaypalWebhookService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> receive(
            @RequestHeader Map<String, String> headers,
            @RequestBody String body
    ) {
        processPaypalWebhookService.process(headers, body);
        return ResponseEntity.ok().build();
    }
}
