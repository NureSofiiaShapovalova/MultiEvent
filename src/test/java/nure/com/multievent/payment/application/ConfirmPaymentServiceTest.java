package nure.com.multievent.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
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
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.notifications.domain.OrderPaidEvent;
import nure.com.multievent.payment.domain.Payment;
import nure.com.multievent.payment.domain.PaymentAttempt;
import nure.com.multievent.payment.domain.PaymentAttemptType;
import nure.com.multievent.payment.domain.PaymentGateway;
import nure.com.multievent.payment.domain.PaymentGatewayResult;
import nure.com.multievent.payment.domain.PaymentPolicy;
import nure.com.multievent.payment.domain.PaymentStatus;
import nure.com.multievent.payment.infra.PaymentRepository;
import nure.com.multievent.payment.infra.PaymentTransactionRepository;
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
class ConfirmPaymentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private TicketInventoryRepository ticketInventoryRepository;

    @Mock
    private Clock clock;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private ConfirmPaymentService confirmPaymentService;

    @BeforeEach
    void setUp() {
        confirmPaymentService = new ConfirmPaymentService(
                userRepository,
                paymentRepository,
                orderRepository,
                paymentTransactionRepository,
                paymentGateway,
                new PaymentPolicy(),
                new PaymentViewMapper(),
                new FinalizeOrderInventoryService(ticketInventoryRepository),
                applicationEventPublisher,
                clock);
    }

    @Test
    void marksPaymentAndOrderAsPaidOnSuccessfulGatewayConfirmation() {
        Instant now = Instant.parse("2026-08-15T00:00:00Z");
        User customer = TestEntityFactory.user("buyer@example.com");
        Order order = order(customer, now.plusSeconds(900));
        Payment payment = pendingPayment(customer, order, now);

        when(clock.instant()).thenReturn(now);
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(customer));
        when(paymentRepository.findByOrder_IdAndCustomer_Id(order.getId(), customer.getId())).thenReturn(Optional.of(payment));
        when(paymentGateway.confirm(payment)).thenReturn(new PaymentGatewayResult(true, null));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketInventoryRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentPageView view = confirmPaymentService.confirm("buyer@example.com", order.getId());

        TicketInventory inventory = order.getReservationLocks().get(0).getTicketType().getInventory();
        assertThat(view.paymentStatus()).isEqualTo("PAID");
        assertThat(payment.isPaid()).isTrue();
        assertThat(order.getStatus().name()).isEqualTo("PAID");
        assertThat(order.getReservationLocks().get(0).getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(inventory.getReservedQuantity()).isZero();
        assertThat(inventory.getSoldQuantity()).isEqualTo(2);
        assertThat(payment.getAttempts()).hasSize(2);
        verify(paymentGateway).confirm(payment);
        verify(orderRepository).save(order);

        ArgumentCaptor<OrderPaidEvent> eventCaptor = ArgumentCaptor.forClass(OrderPaidEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(order.getId());
        assertThat(eventCaptor.getValue().customerId()).isEqualTo(customer.getId());
    }

    @Test
    void marksPaymentFailedWhenGatewayRejects() {
        Instant now = Instant.parse("2026-08-15T00:00:00Z");
        User customer = TestEntityFactory.user("buyer@example.com");
        Order order = order(customer, now.plusSeconds(900));
        Payment payment = pendingPayment(customer, order, now);

        when(clock.instant()).thenReturn(now);
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(customer));
        when(paymentRepository.findByOrder_IdAndCustomer_Id(order.getId(), customer.getId())).thenReturn(Optional.of(payment));
        when(paymentGateway.confirm(payment)).thenReturn(new PaymentGatewayResult(false, "Gateway timeout"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentPageView view = confirmPaymentService.confirm("buyer@example.com", order.getId());

        assertThat(view.paymentStatus()).isEqualTo("FAILED");
        assertThat(view.message()).isEqualTo("Gateway timeout");
        assertThat(view.canRetry()).isTrue();
        assertThat(payment.isFailed()).isTrue();
        assertThat(order.getStatus().name()).isEqualTo("PENDING_PAYMENT");
        assertThat(order.getReservationLocks().get(0).getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(order.getReservationLocks().get(0).getTicketType().getInventory().getReservedQuantity()).isEqualTo(2);
        assertThat(order.getReservationLocks().get(0).getTicketType().getInventory().getSoldQuantity()).isZero();
        assertThat(payment.getAttempts()).hasSize(2);
        verify(orderRepository, never()).save(any(Order.class));
        verify(ticketInventoryRepository, never()).saveAll(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void isIdempotentWhenPaymentIsAlreadyPaid() {
        Instant now = Instant.parse("2026-08-15T00:00:00Z");
        User customer = TestEntityFactory.user("buyer@example.com");
        Order order = order(customer, now.plusSeconds(900));
        Payment payment = pendingPayment(customer, order, now);
        TicketInventory inventory = order.getReservationLocks().get(0).getTicketType().getInventory();
        inventory.markSold(2);
        order.getReservationLocks().get(0).confirm();
        payment.markPaid(now.plusSeconds(10));
        order.markPaid();

        when(clock.instant()).thenReturn(now);
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(customer));
        when(paymentRepository.findByOrder_IdAndCustomer_Id(order.getId(), customer.getId())).thenReturn(Optional.of(payment));

        PaymentPageView view = confirmPaymentService.confirm("buyer@example.com", order.getId());

        assertThat(view.paymentStatus()).isEqualTo("PAID");
        assertThat(inventory.getReservedQuantity()).isZero();
        assertThat(inventory.getSoldQuantity()).isEqualTo(2);
        verify(paymentGateway, never()).confirm(any(Payment.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(ticketInventoryRepository, never()).saveAll(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    private Order order(User customer, Instant expiresAt) {
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
        Order order = new Order(customer, event, expiresAt);
        order.addItem(new OrderItem(ticketType, 2));
        order.addReservationLock(new ReservationLock(ticketType, 2, expiresAt));
        ticketType.getInventory().reserve(2);
        order.updateTotals(BigDecimal.valueOf(50), "USD");
        TestEntityFactory.setId(order, UUID.randomUUID());
        return order;
    }

    private Payment pendingPayment(User customer, Order order, Instant now) {
        Payment payment = new Payment(
                customer,
                order,
                "LOCAL_MANUAL",
                "local-ref",
                order.getTotalAmount(),
                order.getCurrency(),
                now);
        TestEntityFactory.setId(payment, UUID.randomUUID());
        payment.addAttempt(new PaymentAttempt(
                PaymentAttemptType.START,
                PaymentStatus.PENDING,
                "LOCAL_MANUAL",
                "local-ref",
                order.getTotalAmount(),
                order.getCurrency(),
                null));
        return payment;
    }
}
