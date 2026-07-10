package nure.com.multievent.notifications.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import nure.com.multievent.checkout.domain.Order;
import nure.com.multievent.checkout.infra.CheckoutQueryRepository;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.notifications.domain.EventCancelledEvent;
import nure.com.multievent.notifications.domain.EventPublishedEvent;
import nure.com.multievent.notifications.domain.NotificationTemplateCode;
import nure.com.multievent.notifications.domain.OrderPaidEvent;
import nure.com.multievent.notifications.domain.UserRegisteredEvent;
import nure.com.multievent.organizer.infra.OrganizerEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final UserRepository userRepository;
    private final OrganizerEventRepository organizerEventRepository;
    private final CheckoutQueryRepository checkoutQueryRepository;
    private final SendNotificationService sendNotificationService;

    public NotificationEventListener(
            UserRepository userRepository,
            OrganizerEventRepository organizerEventRepository,
            CheckoutQueryRepository checkoutQueryRepository,
            SendNotificationService sendNotificationService
    ) {
        this.userRepository = userRepository;
        this.organizerEventRepository = organizerEventRepository;
        this.checkoutQueryRepository = checkoutQueryRepository;
        this.sendNotificationService = sendNotificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        userRepository.findById(event.userId())
                .ifPresentOrElse(
                        user -> sendSafely(user, NotificationTemplateCode.WELCOME, Map.of(
                                "email", user.getEmail())),
                        () -> log.warn("Skipping welcome notification because user {} was not found.", event.userId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventPublished(EventPublishedEvent event) {
        organizerEventRepository.findByIdAndOrganizer_Id(event.eventId(), event.organizerId())
                .ifPresentOrElse(
                        organizerEvent -> sendSafely(organizerEvent.getOrganizer(), NotificationTemplateCode.EVENT_PUBLISHED, Map.of(
                                "eventTitle", organizerEvent.getTitle(),
                                "eventSlug", organizerEvent.getSlug(),
                                "publishedAt", organizerEvent.getPublishedAt())),
                        () -> log.warn(
                                "Skipping publish notification because event {} for organizer {} was not found.",
                                event.eventId(),
                                event.organizerId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPaid(OrderPaidEvent event) {
        checkoutQueryRepository.findUserOrderById(event.customerId(), event.orderId())
                .ifPresentOrElse(
                        order -> sendSafely(order.getCustomer(), NotificationTemplateCode.ORDER_PAID, Map.of(
                                "eventTitle", order.getEvent().getTitle(),
                                "orderId", order.getId().toString(),
                                "totalAmount", order.getTotalAmount(),
                                "currency", order.getCurrency())),
                        () -> log.warn(
                                "Skipping payment receipt because order {} for customer {} was not found.",
                                event.orderId(),
                                event.customerId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventCancelled(EventCancelledEvent event) {
        List<Order> orders = checkoutQueryRepository.findOrdersByEventId(event.eventId());
        if (orders.isEmpty()) {
            return;
        }

        Map<UUID, List<Order>> ordersByCustomer = orders.stream()
                .collect(Collectors.groupingBy(order -> order.getCustomer().getId(), LinkedHashMap::new, Collectors.toList()));
        for (List<Order> customerOrders : ordersByCustomer.values()) {
            Order representativeOrder = customerOrders.get(0);
            sendSafely(representativeOrder.getCustomer(), NotificationTemplateCode.EVENT_CANCELLED, Map.of(
                    "eventTitle", representativeOrder.getEvent().getTitle(),
                    "eventSlug", representativeOrder.getEvent().getSlug(),
                    "orderCount", customerOrders.size()));
        }
    }

    private void sendSafely(User recipient, NotificationTemplateCode templateCode, Map<String, ?> payload) {
        try {
            sendNotificationService.send(recipient, templateCode, payload);
        } catch (RuntimeException exception) {
            log.warn(
                    "Notification {} for recipient {} failed after commit.",
                    templateCode,
                    recipient.getEmail(),
                    exception);
        }
    }
}
