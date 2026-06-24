package nure.com.eventflow.payment.application;

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
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.checkout.domain.Order;
import nure.com.eventflow.checkout.domain.OrderItem;
import nure.com.eventflow.checkout.infra.OrderRepository;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.payment.domain.Payment;
import nure.com.eventflow.payment.domain.PaymentAttempt;
import nure.com.eventflow.payment.domain.PaymentAttemptType;
import nure.com.eventflow.payment.domain.PaymentGateway;
import nure.com.eventflow.payment.domain.PaymentGatewayResult;
import nure.com.eventflow.payment.domain.PaymentPolicy;
import nure.com.eventflow.payment.domain.PaymentStatus;
import nure.com.eventflow.payment.infra.PaymentRepository;
import nure.com.eventflow.payment.infra.PaymentTransactionRepository;
import nure.com.eventflow.notifications.domain.OrderPaidEvent;
import nure.com.eventflow.testsupport.TestEntityFactory;
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

        PaymentPageView view = confirmPaymentService.confirm("buyer@example.com", order.getId());

        assertThat(view.paymentStatus()).isEqualTo("PAID");
        assertThat(payment.isPaid()).isTrue();
        assertThat(order.getStatus().name()).isEqualTo("PAID");
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
        assertThat(payment.getAttempts()).hasSize(2);
        verify(orderRepository, never()).save(any(Order.class));
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void isIdempotentWhenPaymentIsAlreadyPaid() {
        Instant now = Instant.parse("2026-08-15T00:00:00Z");
        User customer = TestEntityFactory.user("buyer@example.com");
        Order order = order(customer, now.plusSeconds(900));
        Payment payment = pendingPayment(customer, order, now);
        payment.markPaid(now.plusSeconds(10));
        order.markPaid();

        when(clock.instant()).thenReturn(now);
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(customer));
        when(paymentRepository.findByOrder_IdAndCustomer_Id(order.getId(), customer.getId())).thenReturn(Optional.of(payment));

        PaymentPageView view = confirmPaymentService.confirm("buyer@example.com", order.getId());

        assertThat(view.paymentStatus()).isEqualTo("PAID");
        verify(paymentGateway, never()).confirm(any(Payment.class));
        verify(orderRepository, never()).save(any(Order.class));
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
