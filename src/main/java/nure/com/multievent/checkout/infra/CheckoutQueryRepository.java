package nure.com.multievent.checkout.infra;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import nure.com.multievent.checkout.domain.Order;
import nure.com.multievent.checkout.domain.OrderStatus;
import org.springframework.stereotype.Repository;

@Repository
public class CheckoutQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<Order> findUserOrderById(UUID customerId, UUID orderId) {
        List<Order> orders = entityManager.createQuery("""
                        select distinct o
                        from CheckoutOrder o
                        join fetch o.customer c
                        join fetch o.event e
                        left join fetch o.items items
                        left join fetch items.ticketType itemTicketType
                        where c.id = :customerId
                          and o.id = :orderId
                        """, Order.class)
                .setParameter("customerId", customerId)
                .setParameter("orderId", orderId)
                .getResultList();
        if (orders.isEmpty()) {
            return Optional.empty();
        }

        entityManager.createQuery("""
                        select distinct o
                        from CheckoutOrder o
                        left join fetch o.reservationLocks locks
                        left join fetch locks.ticketType lockTicketType
                        left join fetch lockTicketType.inventory lockInventory
                        where o.id = :orderId
                        """, Order.class)
                .setParameter("orderId", orderId)
                .getResultList();

        return Optional.of(orders.get(0));
    }

    public List<Order> findUserOrders(UUID customerId) {
        return entityManager.createQuery("""
                        select distinct o
                        from CheckoutOrder o
                        join fetch o.customer c
                        join fetch o.event e
                        left join fetch o.items items
                        left join fetch items.ticketType itemTicketType
                        where c.id = :customerId
                        order by o.createdAt desc
                        """, Order.class)
                .setParameter("customerId", customerId)
                .getResultList();
    }

    public List<Order> findOrdersByEventId(UUID eventId) {
        return entityManager.createQuery("""
                        select distinct o
                        from CheckoutOrder o
                        join fetch o.customer c
                        join fetch o.event e
                        where e.id = :eventId
                          and o.status in :statuses
                        order by o.createdAt asc
                        """, Order.class)
                .setParameter("eventId", eventId)
                .setParameter("statuses", List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID))
                .getResultList();
    }

    public List<Order> findPendingOrdersToExpire(Instant now) {
        return entityManager.createQuery("""
                        select distinct o
                        from CheckoutOrder o
                        join fetch o.customer c
                        join fetch o.event e
                        left join fetch o.reservationLocks locks
                        left join fetch locks.ticketType lockTicketType
                        left join fetch lockTicketType.inventory lockInventory
                        where o.status = :status
                          and o.expiresAt <= :now
                        """, Order.class)
                .setParameter("status", OrderStatus.PENDING_PAYMENT)
                .setParameter("now", now)
                .getResultList();
    }
}
