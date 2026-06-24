package nure.com.eventflow.checkout.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import nure.com.eventflow.checkout.domain.TicketReservation;
import nure.com.eventflow.checkout.domain.TicketReservationItem;
import nure.com.eventflow.checkout.domain.Order;
import nure.com.eventflow.checkout.domain.OrderItem;
import nure.com.eventflow.ticketing.domain.TicketInventory;
import nure.com.eventflow.ticketing.domain.TicketSalesPolicy;
import nure.com.eventflow.ticketing.domain.TicketType;
import org.springframework.stereotype.Component;

@Component
public class CheckoutViewMapper {

    private final TicketSalesPolicy ticketSalesPolicy;

    public CheckoutViewMapper(TicketSalesPolicy ticketSalesPolicy) {
        this.ticketSalesPolicy = ticketSalesPolicy;
    }

    public TicketCheckoutView toCheckoutView(TicketType ticketType, Instant now) {
        TicketInventory inventory = ticketType.getInventory();
        int availableQuantity = inventory != null ? inventory.availableQuantity() : 0;
        return new TicketCheckoutView(
                ticketType.getId(),
                ticketType.getEvent().getTitle(),
                ticketType.getEvent().getSlug(),
                ticketType.getName(),
                ticketType.getDescription(),
                ticketType.getPrice(),
                ticketType.getCurrency(),
                ticketType.getMaxPerOrder(),
                availableQuantity,
                ticketSalesPolicy.availabilityLabel(ticketType, inventory, now));
    }

    public ReservationView toReservationView(TicketReservation reservation, Instant now) {
        TicketReservationItem item = reservation.getPrimaryItem();
        TicketInventory inventory = item.getTicketType().getInventory();
        int availableQuantity = inventory != null ? inventory.availableQuantity() : 0;
        return new ReservationView(
                reservation.getId(),
                reservation.getEvent().getTitle(),
                reservation.getEvent().getSlug(),
                item.getTicketType().getId(),
                item.getTicketName(),
                item.getTicketType().getDescription(),
                item.getUnitPrice(),
                item.getCurrency(),
                item.getQuantity(),
                item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())),
                availableQuantity,
                reservation.getExpiresAt(),
                reservation.getStatus().name());
    }

    public CheckoutSummaryView toCheckoutSummaryView(Order order, Instant now) {
        return new CheckoutSummaryView(
                order.getId(),
                order.getEvent().getTitle(),
                order.getEvent().getSlug(),
                order.getItems().stream().map(this::toOrderItemView).toList(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getExpiresAt(),
                order.getStatus().name(),
                "Ready to pay");
    }

    public UserOrderRowView toUserOrderRowView(Order order) {
        return new UserOrderRowView(
                order.getId(),
                order.getEvent().getTitle(),
                order.getEvent().getSlug(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getExpiresAt(),
                order.getItems().size(),
                order.getTotalQuantity());
    }

    public UserOrderDetailsView toUserOrderDetailsView(Order order) {
        return new UserOrderDetailsView(
                order.getId(),
                order.getEvent().getTitle(),
                order.getEvent().getSlug(),
                order.getItems().stream().map(this::toOrderItemView).toList(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getExpiresAt(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    public OrderItemView toOrderItemView(OrderItem item) {
        return new OrderItemView(
                item.getTicketType().getId(),
                item.getTicketName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal(),
                item.getCurrency());
    }
}
