package nure.com.multievent.checkout.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import nure.com.multievent.bootstrap.security.SecurityConfig;
import nure.com.multievent.catalog.application.EventDetailsView;
import nure.com.multievent.checkout.application.CheckoutSummaryView;
import nure.com.multievent.checkout.application.CheckoutWebMapper;
import nure.com.multievent.checkout.application.CreateOrderFromSelectionService;
import nure.com.multievent.checkout.application.GetCheckoutSummaryService;
import nure.com.multievent.checkout.application.StartCheckoutService;
import nure.com.multievent.checkout.application.TicketSelectionCommand;
import nure.com.multievent.checkout.application.OrderItemView;
import nure.com.multievent.ticketing.application.TicketOptionView;
import nure.com.multievent.payment.application.PaymentPageView;
import nure.com.multievent.payment.application.StartPaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CheckoutController.class)
@Import({SecurityConfig.class, CheckoutWebMapper.class})
class CheckoutControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StartCheckoutService startCheckoutService;

    @MockitoBean
    private CreateOrderFromSelectionService createOrderFromSelectionService;

    @MockitoBean
    private GetCheckoutSummaryService getCheckoutSummaryService;

    @MockitoBean
    private StartPaymentService startPaymentService;

    @Test
    void showsCheckoutAndSummaryPages() throws Exception {
        UUID ticketTypeId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        when(startCheckoutService.getBySlug("rock-night")).thenReturn(eventDetails(ticketTypeId));
        when(getCheckoutSummaryService.getById("buyer@example.com", orderId)).thenReturn(summary(orderId, ticketTypeId));

        mockMvc.perform(get("/events/{slug}/checkout", "rock-night")
                        .with(user("buyer@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout/select-tickets"))
                .andExpect(model().attributeExists("event", "checkoutForm"));

        mockMvc.perform(get("/checkout/orders/{orderId}/summary", orderId)
                        .with(user("buyer@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout/summary"))
                .andExpect(model().attributeExists("summary"));
    }

    @Test
    void createsOrderFromSelectedTickets() throws Exception {
        UUID ticketTypeId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        when(startCheckoutService.getBySlug("rock-night")).thenReturn(eventDetails(ticketTypeId));
        when(createOrderFromSelectionService.create(eq("buyer@example.com"), eq("rock-night"), any(TicketSelectionCommand.class)))
                .thenReturn(orderId);

        mockMvc.perform(post("/events/{slug}/checkout", "rock-night")
                        .with(user("buyer@example.com").roles("USER"))
                        .with(csrf())
                        .param("selections[0].ticketTypeId", ticketTypeId.toString())
                        .param("selections[0].quantity", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/checkout/orders/*/summary"));
    }

    @Test
    void startsPaymentFromSummaryPage() throws Exception {
        UUID ticketTypeId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        when(startPaymentService.start("buyer@example.com", orderId)).thenReturn(paymentView(orderId, ticketTypeId));

        mockMvc.perform(post("/checkout/orders/{orderId}/confirm", orderId)
                        .with(user("buyer@example.com").roles("USER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/checkout/orders/" + orderId + "/payment"));
    }

    @Test
    void rejectsEmptySelectionPayload() throws Exception {
        UUID ticketTypeId = UUID.randomUUID();
        when(startCheckoutService.getBySlug("rock-night")).thenReturn(eventDetails(ticketTypeId));

        mockMvc.perform(post("/events/{slug}/checkout", "rock-night")
                        .with(user("buyer@example.com").roles("USER"))
                        .with(csrf())
                        .param("selections[0].ticketTypeId", ticketTypeId.toString())
                        .param("selections[0].quantity", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout/select-tickets"))
                .andExpect(model().attributeHasFieldErrors("checkoutForm", "selectionChosen"));
    }

    private EventDetailsView eventDetails(UUID ticketTypeId) {
        TicketOptionView ticketOption = new TicketOptionView(
                ticketTypeId,
                "Standard",
                "Entry ticket",
                BigDecimal.valueOf(25),
                "USD",
                4,
                100,
                true,
                "On sale");
        return new EventDetailsView(
                UUID.randomUUID(),
                "Rock Night",
                "rock-night",
                "Short description",
                "Full description",
                "Concerts",
                "Palace",
                "Kyiv",
                "Main street 1",
                "01001",
                "Ukraine",
                Instant.parse("2026-09-01T18:00:00Z"),
                Instant.parse("2026-09-01T21:00:00Z"),
                Instant.parse("2026-08-01T10:00:00Z"),
                "organizer@example.com",
                "https://example.com/cover.jpg",
                List.of("https://example.com/cover.jpg"),
                List.of(ticketOption));
    }

    private CheckoutSummaryView summary(UUID orderId, UUID ticketTypeId) {
        return new CheckoutSummaryView(
                orderId,
                "Rock Night",
                "rock-night",
                List.of(new OrderItemView(ticketTypeId, "Standard", 2, BigDecimal.valueOf(25), BigDecimal.valueOf(50), "USD")),
                BigDecimal.valueOf(50),
                "USD",
                Instant.parse("2026-08-15T00:15:00Z"),
                "PENDING_PAYMENT",
                "Payment step pending");
    }

    private PaymentPageView paymentView(UUID orderId, UUID ticketTypeId) {
        return new PaymentPageView(
                orderId,
                UUID.randomUUID(),
                "Rock Night",
                "rock-night",
                List.of(new OrderItemView(ticketTypeId, "Standard", 2, BigDecimal.valueOf(25), BigDecimal.valueOf(50), "USD")),
                BigDecimal.valueOf(50),
                "USD",
                "PENDING_PAYMENT",
                "PENDING",
                "LOCAL_MANUAL",
                "local-ref",
                Instant.parse("2026-08-15T00:00:00Z"),
                null,
                Instant.parse("2026-08-15T00:15:00Z"),
                null,
                1,
                "Review the order and complete payment when ready.",
                true,
                false);
    }
}
