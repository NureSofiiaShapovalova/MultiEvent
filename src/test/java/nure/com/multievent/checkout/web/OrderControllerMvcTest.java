package nure.com.multievent.checkout.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import nure.com.multievent.bootstrap.security.SecurityConfig;
import nure.com.multievent.checkout.application.GetUserOrderDetailsService;
import nure.com.multievent.checkout.application.ListUserOrdersService;
import nure.com.multievent.checkout.application.OrderItemView;
import nure.com.multievent.checkout.application.UserOrderDetailsView;
import nure.com.multievent.checkout.application.UserOrderRowView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListUserOrdersService listUserOrdersService;

    @MockitoBean
    private GetUserOrderDetailsService getUserOrderDetailsService;

    @Test
    void showsOrderListAndDetailsPages() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(listUserOrdersService.list("buyer@example.com")).thenReturn(List.of(orderRow(orderId)));
        when(getUserOrderDetailsService.getById("buyer@example.com", orderId)).thenReturn(orderDetails(orderId));

        mockMvc.perform(get("/orders").with(user("buyer@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/list"))
                .andExpect(model().attributeExists("orders"));

        mockMvc.perform(get("/orders/{orderId}", orderId).with(user("buyer@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/details"))
                .andExpect(model().attributeExists("order"));
    }

    private UserOrderRowView orderRow(UUID orderId) {
        return new UserOrderRowView(
                orderId,
                "Rock Night",
                "rock-night",
                BigDecimal.valueOf(50),
                "USD",
                "PENDING_PAYMENT",
                Instant.parse("2026-08-15T00:01:00Z"),
                Instant.parse("2026-08-15T00:15:00Z"),
                1,
                2);
    }

    private UserOrderDetailsView orderDetails(UUID orderId) {
        return new UserOrderDetailsView(
                orderId,
                "Rock Night",
                "rock-night",
                List.of(new OrderItemView(UUID.randomUUID(), "Standard", 2, BigDecimal.valueOf(25), BigDecimal.valueOf(50), "USD")),
                BigDecimal.valueOf(50),
                "USD",
                Instant.parse("2026-08-15T00:15:00Z"),
                "PENDING_PAYMENT",
                Instant.parse("2026-08-15T00:01:00Z"),
                Instant.parse("2026-08-15T00:02:00Z"));
    }
}
