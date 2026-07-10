package nure.com.multievent.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.catalog.domain.EventStatus;
import nure.com.multievent.checkout.application.OrderItemView;
import nure.com.multievent.checkout.domain.Order;
import nure.com.multievent.checkout.domain.OrderItem;
import nure.com.multievent.checkout.infra.CheckoutQueryRepository;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.payment.domain.Payment;
import nure.com.multievent.payment.domain.PaymentAttempt;
import nure.com.multievent.payment.domain.PaymentAttemptType;
import nure.com.multievent.payment.domain.PaymentGateway;
import nure.com.multievent.payment.domain.PaymentGatewaySession;
import nure.com.multievent.payment.domain.PaymentGatewayStartRequest;
import nure.com.multievent.payment.domain.PaymentPolicy;
import nure.com.multievent.payment.domain.PaymentStatus;
import nure.com.multievent.payment.infra.PaymentRepository;
import nure.com.multievent.payment.infra.PaymentTransactionRepository;
import nure.com.multievent.testsupport.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class StartPaymentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CheckoutQueryRepository checkoutQueryRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private Clock clock;

    private StartPaymentService startPaymentService;

    @BeforeEach
    void setUp() {
        startPaymentService = new StartPaymentService(
                userRepository,
                checkoutQueryRepository,
                paymentRepository,
                paymentTransactionRepository,
                paymentGateway,
                new PaymentPolicy(),
                new PaymentViewMapper(),
                clock);
    }

    @Test
    void createsPaymentSessionForPendingOrder() {
        Instant now = Instant.parse("2026-08-15T00:00:00Z");
        User customer = TestEntityFactory.user("buyer@example.com");
        Order order = order(customer, now.plusSeconds(900));
        PaymentGatewaySession session = new PaymentGatewaySession("LOCAL_MANUAL", "local-ref");

        when(clock.instant()).thenReturn(now);
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(customer));
        when(checkoutQueryRepository.findUserOrderById(customer.getId(), order.getId())).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrder_IdAndCustomer_Id(order.getId(), customer.getId())).thenReturn(Optional.empty());
        when(paymentGateway.start(any(PaymentGatewayStartRequest.class))).thenReturn(session);
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            TestEntityFactory.setId(payment, UUID.randomUUID());
            return payment;
        });

        PaymentPageView view = startPaymentService.start("buyer@example.com", order.getId());

        assertThat(view.paymentStatus()).isEqualTo("PENDING");
        assertThat(view.canConfirm()).isTrue();
        assertThat(view.canRetry()).isFalse();
        assertThat(view.providerReference()).isEqualTo("local-ref");
        assertThat(view.attemptCount()).isEqualTo(1);
        verify(paymentGateway).start(any(PaymentGatewayStartRequest.class));
    }

    @Test
    void returnsExistingPaymentWithoutCreatingAnother() {
        Instant now = Instant.parse("2026-08-15T00:00:00Z");
        User customer = TestEntityFactory.user("buyer@example.com");
        Order order = order(customer, now.plusSeconds(900));
        Payment payment = pendingPayment(customer, order, now);

        when(clock.instant()).thenReturn(now);
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(customer));
        when(checkoutQueryRepository.findUserOrderById(customer.getId(), order.getId())).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrder_IdAndCustomer_Id(order.getId(), customer.getId())).thenReturn(Optional.of(payment));

        PaymentPageView view = startPaymentService.start("buyer@example.com", order.getId());

        assertThat(view.paymentId()).isEqualTo(payment.getId());
        assertThat(view.paymentStatus()).isEqualTo("PENDING");
        verify(paymentGateway, never()).start(any(), any());
        verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
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
