package nure.com.multievent.checkout.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import nure.com.multievent.bootstrap.security.SecurityConfig;
import nure.com.multievent.checkout.application.AdjustTicketReservationService;
import nure.com.multievent.checkout.application.ConfirmTicketReservationService;
import nure.com.multievent.checkout.application.CreateTicketReservationService;
import nure.com.multievent.checkout.application.GetReservationService;
import nure.com.multievent.checkout.application.GetTicketCheckoutService;
import nure.com.multievent.checkout.application.ReleaseTicketReservationService;
import nure.com.multievent.checkout.application.ReservationView;
import nure.com.multievent.checkout.application.TicketCheckoutView;
import nure.com.multievent.checkout.application.TicketCheckoutWebMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TicketCheckoutController.class)
@Import({SecurityConfig.class, TicketCheckoutWebMapper.class})
class TicketCheckoutControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetTicketCheckoutService getTicketCheckoutService;

    @MockitoBean
    private GetReservationService getReservationService;

    @MockitoBean
    private CreateTicketReservationService createTicketReservationService;

    @MockitoBean
    private AdjustTicketReservationService adjustTicketReservationService;

    @MockitoBean
    private ReleaseTicketReservationService releaseTicketReservationService;

    @MockitoBean
    private ConfirmTicketReservationService confirmTicketReservationService;

    @Test
    void showsCheckoutAndReservationPages() throws Exception {
        UUID ticketTypeId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        when(getTicketCheckoutService.getBySlugAndTicketTypeId("rock-night", ticketTypeId)).thenReturn(checkoutView(ticketTypeId));
        when(getReservationService.getById("buyer@example.com", reservationId)).thenReturn(reservationView(reservationId, ticketTypeId));

        mockMvc.perform(get("/checkout/events/{slug}/tickets/{ticketTypeId}", "rock-night", ticketTypeId)
                        .with(user("buyer@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout/ticket"))
                .andExpect(model().attributeExists("checkoutView", "checkoutForm"));

        mockMvc.perform(get("/checkout/reservations/{reservationId}", reservationId)
                        .with(user("buyer@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout/reservation"))
                .andExpect(model().attributeExists("reservation", "checkoutForm"));
    }

    @Test
    void rejectsInvalidCheckoutQuantity() throws Exception {
        UUID ticketTypeId = UUID.randomUUID();
        when(getTicketCheckoutService.getBySlugAndTicketTypeId("rock-night", ticketTypeId)).thenReturn(checkoutView(ticketTypeId));

        mockMvc.perform(post("/checkout/events/{slug}/tickets/{ticketTypeId}", "rock-night", ticketTypeId)
                        .with(user("buyer@example.com").roles("USER"))
                        .with(csrf())
                        .param("quantity", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout/ticket"))
                .andExpect(model().attributeHasFieldErrors("checkoutForm", "quantity"));
    }

    private TicketCheckoutView checkoutView(UUID ticketTypeId) {
        return new TicketCheckoutView(
                ticketTypeId,
                "Rock Night",
                "rock-night",
                "Standard",
                "Entry ticket",
                BigDecimal.valueOf(25),
                "USD",
                4,
                100,
                "On sale");
    }

    private ReservationView reservationView(UUID reservationId, UUID ticketTypeId) {
        return new ReservationView(
                reservationId,
                "Rock Night",
                "rock-night",
                ticketTypeId,
                "Standard",
                "Entry ticket",
                BigDecimal.valueOf(25),
                "USD",
                2,
                BigDecimal.valueOf(50),
                98,
                Instant.parse("2026-08-15T00:15:00Z"),
                "PENDING");
    }
}
