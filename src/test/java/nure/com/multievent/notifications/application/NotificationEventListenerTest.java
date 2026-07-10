package nure.com.multievent.notifications.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.catalog.domain.EventStatus;
import nure.com.multievent.checkout.domain.Order;
import nure.com.multievent.checkout.domain.OrderItem;
import nure.com.multievent.checkout.infra.CheckoutQueryRepository;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.notifications.domain.EventCancelledEvent;
import nure.com.multievent.notifications.domain.EventPublishedEvent;
import nure.com.multievent.notifications.domain.NotificationTemplateCode;
import nure.com.multievent.notifications.domain.OrderPaidEvent;
import nure.com.multievent.notifications.domain.UserRegisteredEvent;
import nure.com.multievent.organizer.infra.OrganizerEventRepository;
import nure.com.multievent.testsupport.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizerEventRepository organizerEventRepository;

    @Mock
    private CheckoutQueryRepository checkoutQueryRepository;

    @Mock
    private SendNotificationService sendNotificationService;

    private NotificationEventListener notificationEventListener;

    @BeforeEach
    void setUp() {
        notificationEventListener = new NotificationEventListener(
                userRepository,
                organizerEventRepository,
                checkoutQueryRepository,
                sendNotificationService);
    }

    @Test
    void sendsWelcomeMailAfterRegistrationCommit() {
        User user = TestEntityFactory.user("buyer@example.com");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        notificationEventListener.onUserRegistered(new UserRegisteredEvent(user.getId()));

        verify(sendNotificationService).send(
                eq(user),
                eq(NotificationTemplateCode.WELCOME),
                any());
    }

    @Test
    void sendsPublishMailAfterEventCommit() {
        User organizer = TestEntityFactory.user("organizer@example.com");
        Event event = TestEntityFactory.event(
                organizer,
                TestEntityFactory.category("Concerts", "concerts"),
                TestEntityFactory.venue("Palace", "Kyiv"),
                "rock-night",
                EventStatus.DRAFT);
        Instant publishedAt = Instant.parse("2026-06-22T12:00:00Z");
        event.publish(publishedAt);

        when(organizerEventRepository.findByIdAndOrganizer_Id(event.getId(), organizer.getId())).thenReturn(Optional.of(event));

        notificationEventListener.onEventPublished(new EventPublishedEvent(event.getId(), organizer.getId()));

        verify(sendNotificationService).send(
                eq(organizer),
                eq(NotificationTemplateCode.EVENT_PUBLISHED),
                any());
    }

    @Test
    void sendsPaymentReceiptAfterPaymentCommit() {
        Instant now = Instant.parse("2026-06-22T12:00:00Z");
        User customer = TestEntityFactory.user("buyer@example.com");
        Event event = TestEntityFactory.event(
                TestEntityFactory.user("organizer@example.com"),
                TestEntityFactory.category("Concerts", "concerts"),
                TestEntityFactory.venue("Palace", "Kyiv"),
                "rock-night",
                EventStatus.PUBLISHED);
        event.publish(now);
        var ticketType = TestEntityFactory.ticketType(
                event,
                true,
                100,
                BigDecimal.valueOf(25),
                4,
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-07-01T00:00:00Z"));
        Order order = new Order(customer, event, now.plusSeconds(900));
        order.addItem(new OrderItem(ticketType, 2));
        order.updateTotals(BigDecimal.valueOf(50), "USD");
        TestEntityFactory.setId(order, UUID.randomUUID());

        when(checkoutQueryRepository.findUserOrderById(customer.getId(), order.getId())).thenReturn(Optional.of(order));

        notificationEventListener.onOrderPaid(new OrderPaidEvent(order.getId(), customer.getId()));

        verify(sendNotificationService).send(
                eq(customer),
                eq(NotificationTemplateCode.ORDER_PAID),
                any());
    }

    @Test
    void groupsCancelledOrdersByCustomerBeforeSendingNotifications() {
        User organizer = TestEntityFactory.user("organizer@example.com");
        Event event = TestEntityFactory.event(
                organizer,
                TestEntityFactory.category("Concerts", "concerts"),
                TestEntityFactory.venue("Palace", "Kyiv"),
                "rock-night",
                EventStatus.PUBLISHED);
        User customerA = TestEntityFactory.user("buyer-a@example.com");
        User customerB = TestEntityFactory.user("buyer-b@example.com");
        Order orderA1 = new Order(customerA, event, Instant.parse("2026-06-22T13:00:00Z"));
        Order orderA2 = new Order(customerA, event, Instant.parse("2026-06-22T13:30:00Z"));
        Order orderB = new Order(customerB, event, Instant.parse("2026-06-22T14:00:00Z"));
        TestEntityFactory.setId(orderA1, UUID.randomUUID());
        TestEntityFactory.setId(orderA2, UUID.randomUUID());
        TestEntityFactory.setId(orderB, UUID.randomUUID());

        when(checkoutQueryRepository.findOrdersByEventId(event.getId())).thenReturn(List.of(orderA1, orderA2, orderB));

        notificationEventListener.onEventCancelled(new EventCancelledEvent(event.getId(), organizer.getId()));

        ArgumentCaptor<User> recipientCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<NotificationTemplateCode> templateCaptor = ArgumentCaptor.forClass(NotificationTemplateCode.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> payloadCaptor = ArgumentCaptor.forClass((Class<Map<String, ?>>) (Class<?>) Map.class);
        verify(sendNotificationService, times(2)).send(recipientCaptor.capture(), templateCaptor.capture(), payloadCaptor.capture());

        assertThat(templateCaptor.getAllValues()).containsOnly(NotificationTemplateCode.EVENT_CANCELLED);

        Map<String, Integer> orderCountsByEmail = new HashMap<>();
        for (int i = 0; i < recipientCaptor.getAllValues().size(); i++) {
            orderCountsByEmail.put(
                    recipientCaptor.getAllValues().get(i).getEmail(),
                    ((Number) payloadCaptor.getAllValues().get(i).get("orderCount")).intValue());
        }
        assertThat(orderCountsByEmail)
                .containsEntry("buyer-a@example.com", 2)
                .containsEntry("buyer-b@example.com", 1);
    }
}
