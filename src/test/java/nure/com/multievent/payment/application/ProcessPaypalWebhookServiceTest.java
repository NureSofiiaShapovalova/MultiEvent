package nure.com.multievent.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.catalog.domain.EventStatus;
import nure.com.multievent.checkout.application.FinalizeOrderInventoryService;
import nure.com.multievent.checkout.domain.Order;
import nure.com.multievent.checkout.domain.OrderItem;
import nure.com.multievent.checkout.domain.ReservationLock;
import nure.com.multievent.checkout.domain.ReservationStatus;
import nure.com.multievent.checkout.infra.OrderRepository;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.notifications.domain.OrderPaidEvent;
import nure.com.multievent.payment.domain.Payment;
import nure.com.multievent.payment.domain.PaymentAttempt;
import nure.com.multievent.payment.domain.PaymentAttemptType;
import nure.com.multievent.payment.domain.PaymentPolicy;
import nure.com.multievent.payment.domain.PaymentStatus;
import nure.com.multievent.payment.domain.PaymentWebhookVerificationRequest;
import nure.com.multievent.payment.domain.PaymentWebhookVerificationResult;
import nure.com.multievent.payment.domain.PaymentWebhookVerifier;
import nure.com.multievent.payment.domain.ProviderWebhookLog;
import nure.com.multievent.payment.domain.WebhookProcessingStatus;
import nure.com.multievent.payment.infra.PaymentRepository;
import nure.com.multievent.payment.infra.PaymentTransactionRepository;
import nure.com.multievent.payment.infra.ProviderWebhookLogRepository;
import nure.com.multievent.ticketing.domain.TicketInventory;
import nure.com.multievent.ticketing.infra.TicketInventoryRepository;
import nure.com.multievent.testsupport.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ProcessPaypalWebhookServiceTest {

    @Mock
    private ProviderWebhookLogRepository webhookLogRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentWebhookVerifier webhookVerifier;

    @Mock
    private TicketInventoryRepository ticketInventoryRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private ProcessPaypalWebhookService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-15T00:10:00Z"), ZoneOffset.UTC);
        service = new ProcessPaypalWebhookService(
                webhookLogRepository,
                paymentRepository,
                paymentTransactionRepository,
                orderRepository,
                webhookVerifier,
                new PaymentPolicy(),
                new FinalizeOrderInventoryService(ticketInventoryRepository),
                applicationEventPublisher,
                new ObjectMapper(),
                clock);
    }

    @Test
    void marksOrderPaidWhenVerifiedCaptureCompletedWebhookArrives() {
        User customer = TestEntityFactory.user("buyer@example.com");
        Order order = order(customer);
        Payment payment = new Payment(
                customer,
                order,
                "PAYPAL",
                "PAYPAL-ORDER-123",
                "https://paypal.test/approve",
                order.getTotalAmount(),
                order.getCurrency(),
                Instant.parse("2026-08-15T00:00:00Z"));
        TestEntityFactory.setId(payment, UUID.randomUUID());
        payment.addAttempt(new PaymentAttempt(
                PaymentAttemptType.START,
                PaymentStatus.PENDING,
                "PAYPAL",
                "PAYPAL-ORDER-123",
                order.getTotalAmount(),
                order.getCurrency(),
                null));

        when(webhookLogRepository.findByProviderNameAndEventId("PAYPAL", "WH-EVENT-1")).thenReturn(Optional.empty());
        when(webhookLogRepository.save(any(ProviderWebhookLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(webhookVerifier.verify(any(PaymentWebhookVerificationRequest.class)))
                .thenReturn(new PaymentWebhookVerificationResult(true, null));
        when(paymentRepository.findByProviderNameAndProviderReference("PAYPAL", "PAYPAL-ORDER-123"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketInventoryRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.process(Map.of(), captureCompletedWebhook());

        TicketInventory inventory = order.getReservationLocks().get(0).getTicketType().getInventory();
        assertThat(payment.isPaid()).isTrue();
        assertThat(order.getStatus().name()).isEqualTo("PAID");
        assertThat(order.getReservationLocks().get(0).getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(inventory.getReservedQuantity()).isZero();
        assertThat(inventory.getSoldQuantity()).isEqualTo(2);
        assertThat(payment.getAttempts()).hasSize(2);

        ArgumentCaptor<ProviderWebhookLog> logCaptor = ArgumentCaptor.forClass(ProviderWebhookLog.class);
        verify(webhookLogRepository, org.mockito.Mockito.times(2)).save(logCaptor.capture());
        assertThat(logCaptor.getAllValues().getLast().getProcessingStatus()).isEqualTo(WebhookProcessingStatus.PROCESSED);

        ArgumentCaptor<OrderPaidEvent> eventCaptor = ArgumentCaptor.forClass(OrderPaidEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(order.getId());
        assertThat(eventCaptor.getValue().customerId()).isEqualTo(customer.getId());
    }

    @Test
    void doesNotSellInventoryTwiceWhenCaptureCompletedWebhookArrivesAfterPaymentIsAlreadyPaid() {
        User customer = TestEntityFactory.user("buyer@example.com");
        Order order = order(customer);
        TicketInventory inventory = order.getReservationLocks().get(0).getTicketType().getInventory();
        inventory.markSold(2);
        order.getReservationLocks().get(0).confirm();
        order.markPaid();
        Payment payment = new Payment(
                customer,
                order,
                "PAYPAL",
                "PAYPAL-ORDER-123",
                "https://paypal.test/approve",
                order.getTotalAmount(),
                order.getCurrency(),
                Instant.parse("2026-08-15T00:00:00Z"));
        TestEntityFactory.setId(payment, UUID.randomUUID());
        payment.addAttempt(new PaymentAttempt(
                PaymentAttemptType.START,
                PaymentStatus.PENDING,
                "PAYPAL",
                "PAYPAL-ORDER-123",
                order.getTotalAmount(),
                order.getCurrency(),
                null));
        payment.markPaid(Instant.parse("2026-08-15T00:05:00Z"));

        when(webhookLogRepository.findByProviderNameAndEventId("PAYPAL", "WH-EVENT-1")).thenReturn(Optional.empty());
        when(webhookLogRepository.save(any(ProviderWebhookLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(webhookVerifier.verify(any(PaymentWebhookVerificationRequest.class)))
                .thenReturn(new PaymentWebhookVerificationResult(true, null));
        when(paymentRepository.findByProviderNameAndProviderReference("PAYPAL", "PAYPAL-ORDER-123"))
                .thenReturn(Optional.of(payment));

        service.process(Map.of(), captureCompletedWebhook());

        assertThat(inventory.getReservedQuantity()).isZero();
        assertThat(inventory.getSoldQuantity()).isEqualTo(2);
        assertThat(order.getReservationLocks().get(0).getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(payment.getAttempts()).hasSize(1);
        verify(ticketInventoryRepository, never()).saveAll(any());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    private Order order(User customer) {
        Event event = TestEntityFactory.event(
                TestEntityFactory.user("organizer@example.com"),
                TestEntityFactory.category("Concerts", "concerts"),
                TestEntityFactory.venue("Palace", "Kyiv"),
                "rock-night",
                EventStatus.PUBLISHED);
        var ticketType = TestEntityFactory.ticketType(
                event,
                true,
                100,
                BigDecimal.valueOf(25),
                4,
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"));
        TestEntityFactory.setField(ticketType, "currency", "EUR");
        Order order = new Order(customer, event, Instant.parse("2026-08-15T00:15:00Z"));
        order.addItem(new OrderItem(ticketType, 2));
        order.addReservationLock(new ReservationLock(ticketType, 2, order.getExpiresAt()));
        ticketType.getInventory().reserve(2);
        order.updateTotals(BigDecimal.valueOf(50), "EUR");
        TestEntityFactory.setId(order, UUID.randomUUID());
        return order;
    }

    private String captureCompletedWebhook() {
        return """
                {
                  "id": "WH-EVENT-1",
                  "event_type": "PAYMENT.CAPTURE.COMPLETED",
                  "resource": {
                    "id": "CAPTURE-123",
                    "status": "COMPLETED",
                    "amount": {
                      "currency_code": "EUR",
                      "value": "50.00"
                    },
                    "supplementary_data": {
                      "related_ids": {
                        "order_id": "PAYPAL-ORDER-123"
                      }
                    }
                  }
                }
                """;
    }
}
