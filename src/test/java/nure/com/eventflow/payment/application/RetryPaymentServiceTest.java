package nure.com.eventflow.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import nure.com.eventflow.payment.domain.PaymentGatewaySession;
import nure.com.eventflow.payment.domain.PaymentGatewayStartRequest;
import nure.com.eventflow.payment.domain.PaymentPolicy;
import nure.com.eventflow.payment.domain.PaymentStatus;
import nure.com.eventflow.payment.infra.PaymentRepository;
import nure.com.eventflow.payment.infra.PaymentTransactionRepository;
import nure.com.eventflow.testsupport.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RetryPaymentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private Clock clock;

    private RetryPaymentService retryPaymentService;

    @BeforeEach
    void setUp() {
        retryPaymentService = new RetryPaymentService(
                userRepository,
                paymentRepository,
                paymentTransactionRepository,
                orderRepository,
                paymentGateway,
                new PaymentPolicy(),
                new PaymentViewMapper(),
                clock);
    }

    @Test
    void restartsFailedPaymentWithFreshProviderReference() {
        Instant now = Instant.parse("2026-08-15T00:00:00Z");
        User customer = TestEntityFactory.user("buyer@example.com");
        Order order = order(customer, now.plusSeconds(900));
        Payment payment = failedPayment(customer, order, now);

        when(clock.instant()).thenReturn(now);
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(customer));
        when(paymentRepository.findByOrder_IdAndCustomer_Id(order.getId(), customer.getId())).thenReturn(Optional.of(payment));
        when(paymentGateway.start(any(PaymentGatewayStartRequest.class)))
                .thenReturn(new PaymentGatewaySession("LOCAL_MANUAL", "local-ref-2"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentPageView view = retryPaymentService.retry("buyer@example.com", order.getId());

        assertThat(view.paymentStatus()).isEqualTo("PENDING");
        assertThat(payment.isPending()).isTrue();
        assertThat(payment.getProviderReference()).isEqualTo("local-ref-2");
        assertThat(payment.getAttempts()).hasSize(2);
        verify(paymentGateway).start(any(PaymentGatewayStartRequest.class));
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

    private Payment failedPayment(User customer, Order order, Instant now) {
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
        payment.markFailed("Gateway timeout", now.plusSeconds(10));
        return payment;
    }
}
