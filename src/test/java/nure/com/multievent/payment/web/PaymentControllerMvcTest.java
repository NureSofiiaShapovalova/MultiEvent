package nure.com.multievent.payment.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import nure.com.multievent.bootstrap.security.SecurityConfig;
import nure.com.multievent.checkout.application.OrderItemView;
import nure.com.multievent.payment.application.ConfirmPaymentService;
import nure.com.multievent.payment.application.PaymentPageView;
import nure.com.multievent.payment.application.RetryPaymentService;
import nure.com.multievent.payment.application.StartPaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
class PaymentControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StartPaymentService startPaymentService;

    @MockitoBean
    private ConfirmPaymentService confirmPaymentService;

    @MockitoBean
    private RetryPaymentService retryPaymentService;

    @Test
    void showsStartAndResultPages() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID ticketTypeId = UUID.randomUUID();
        when(startPaymentService.start("buyer@example.com", orderId))
                .thenReturn(
                        paymentView(orderId, ticketTypeId, "PENDING", true, false, "Review the order and complete payment when ready."),
                        paymentView(orderId, ticketTypeId, "PAID", false, false, "Payment completed."));

        mockMvc.perform(get("/checkout/orders/{orderId}/payment", orderId)
                        .with(user("buyer@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("payment/start"))
                .andExpect(model().attributeExists("payment"));

        mockMvc.perform(get("/checkout/orders/{orderId}/payment/result", orderId)
                        .with(user("buyer@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("payment/result"))
                .andExpect(model().attributeExists("payment"));
    }

    @Test
    void confirmsAndRetriesPayments() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID ticketTypeId = UUID.randomUUID();
        when(confirmPaymentService.confirm("buyer@example.com", orderId))
                .thenReturn(paymentView(orderId, ticketTypeId, "PAID", false, false, "Payment completed."));
        when(retryPaymentService.retry("buyer@example.com", orderId))
                .thenReturn(paymentView(orderId, ticketTypeId, "PENDING", true, false, "Review the order and complete payment when ready."));

        mockMvc.perform(post("/checkout/orders/{orderId}/payment/confirm", orderId)
                        .with(user("buyer@example.com").roles("USER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/checkout/orders/" + orderId + "/payment/result"));

        mockMvc.perform(post("/checkout/orders/{orderId}/payment/retry", orderId)
                        .with(user("buyer@example.com").roles("USER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/checkout/orders/" + orderId + "/payment"));
    }

    private PaymentPageView paymentView(
            UUID orderId,
            UUID ticketTypeId,
            String status,
            boolean canConfirm,
            boolean canRetry,
            String message
    ) {
        Instant completedAt = "PENDING".equals(status) ? null : Instant.parse("2026-08-15T00:05:00Z");
        String orderStatus = "PAID".equals(status) ? "PAID" : "PENDING_PAYMENT";
        return new PaymentPageView(
                orderId,
                UUID.randomUUID(),
                "Rock Night",
                "rock-night",
                List.of(new OrderItemView(ticketTypeId, "Standard", 2, BigDecimal.valueOf(25), BigDecimal.valueOf(50), "USD")),
                BigDecimal.valueOf(50),
                "USD",
                orderStatus,
                status,
                "LOCAL_MANUAL",
                "local-ref",
                Instant.parse("2026-08-15T00:00:00Z"),
                completedAt,
                Instant.parse("2026-08-15T00:15:00Z"),
                "PAID".equals(status) ? null : ("FAILED".equals(status) ? "Gateway timeout" : null),
                1,
                message,
                canConfirm,
                canRetry);
    }
}
