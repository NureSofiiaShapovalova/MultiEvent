package nure.com.multievent.checkout.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import nure.com.multievent.checkout.domain.Order;
import nure.com.multievent.checkout.domain.ReservationLock;
import nure.com.multievent.checkout.domain.ReservationPolicy;
import nure.com.multievent.checkout.infra.CheckoutQueryRepository;
import nure.com.multievent.checkout.infra.OrderRepository;
import nure.com.multievent.ticketing.domain.TicketInventory;
import nure.com.multievent.ticketing.infra.TicketInventoryRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpireReservationsService {

    private final CheckoutQueryRepository checkoutQueryRepository;
    private final OrderRepository orderRepository;
    private final TicketInventoryRepository ticketInventoryRepository;
    private final ReservationPolicy reservationPolicy;
    private final Clock clock;

    public ExpireReservationsService(
            CheckoutQueryRepository checkoutQueryRepository,
            OrderRepository orderRepository,
            TicketInventoryRepository ticketInventoryRepository,
            ReservationPolicy reservationPolicy,
            Clock clock
    ) {
        this.checkoutQueryRepository = checkoutQueryRepository;
        this.orderRepository = orderRepository;
        this.ticketInventoryRepository = ticketInventoryRepository;
        this.reservationPolicy = reservationPolicy;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 60000L)
    @Transactional
    public void expireDueReservations() {
        Instant now = clock.instant();
        List<Order> expiredOrders = checkoutQueryRepository.findPendingOrdersToExpire(now);
        if (expiredOrders.isEmpty()) {
            return;
        }

        List<TicketInventory> inventoriesToSave = new ArrayList<>();
        for (Order order : expiredOrders) {
            reservationPolicy.assertCanExpire(order, now);
            for (ReservationLock reservationLock : order.getReservationLocks()) {
                if (reservationLock.isActive()) {
                    reservationLock.expire(now);
                    TicketInventory inventory = reservationLock.getTicketType().getInventory();
                    inventory.release(reservationLock.getReservedQuantity());
                    inventoriesToSave.add(inventory);
                }
            }
            order.markExpired();
        }

        orderRepository.saveAll(expiredOrders);
        ticketInventoryRepository.saveAll(inventoriesToSave);
    }
}
