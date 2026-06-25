package nure.com.eventflow.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import nure.com.eventflow.checkout.domain.OrderStatus;
import nure.com.eventflow.checkout.infra.CheckoutQueryRepository;
import nure.com.eventflow.checkout.infra.OrderRepository;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.payment.domain.Payment;
import nure.com.eventflow.payment.domain.PaymentTransaction;
import nure.com.eventflow.payment.domain.PaymentTransactionType;
import nure.com.eventflow.payment.domain.RefundRequest;
import nure.com.eventflow.payment.domain.RefundStatus;
import nure.com.eventflow.payment.infra.PaymentRepository;
import nure.com.eventflow.payment.infra.PaymentTransactionRepository;
import nure.com.eventflow.payment.infra.RefundRequestRepository;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import nure.com.eventflow.testsupport.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequestRefundServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CheckoutQueryRepository checkoutQueryRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private RefundRequestRepository refundRequestRepository;

    @Mock
    private Clock clock;

    private RequestRefundService requestRefundService;

    @BeforeEach
    void setUp() {
        requestRefundService = new RequestRefundService(
                userRepository,
                checkoutQueryRepository,
                orderRepository,
                paymentRepository,
                paymentTransactionRepository,
                refundRequestRepository,
                clock);
    }

    @Test
    void createsRefundRequestForPaidOwnedOrder() {
        Instant now = Instant.parse("2026-08-15T10:00:00Z");
        UUID refundRequestId = UUID.randomUUID();
        User customer = TestEntityFactory.user("buyer@example.com");
        Order order = paidOrder(customer, now.plusSeconds(900));
        Payment payment = payment(customer, order, now);

        when(clock.instant()).thenReturn(now);
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(customer));
        when(checkoutQueryRepository.findUserOrderById(customer.getId(), order.getId())).thenReturn(Optional.of(order));
        when(refundRequestRepository.existsByOrder_IdAndStatus(order.getId(), RefundStatus.REQUESTED)).thenReturn(false);
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> {
            RefundRequest refundRequest = invocation.getArgument(0);
            TestEntityFactory.setId(refundRequest, refundRequestId);
            return refundRequest;
        });
        when(orderRepository.save(order)).thenReturn(order);
        when(paymentRepository.findByOrder_Id(order.getId())).thenReturn(Optional.of(payment));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UUID result = requestRefundService.request("buyer@example.com", order.getId(), "I cannot attend.");

        assertThat(result).isEqualTo(refundRequestId);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND_PENDING);

        ArgumentCaptor<RefundRequest> refundRequestCaptor = ArgumentCaptor.forClass(RefundRequest.class);
        verify(refundRequestRepository).save(refundRequestCaptor.capture());
        assertThat(refundRequestCaptor.getValue().getStatus()).isEqualTo(RefundStatus.REQUESTED);
        assertThat(refundRequestCaptor.getValue().getReason()).isEqualTo("I cannot attend.");
        assertThat(refundRequestCaptor.getValue().getRequestedAt()).isEqualTo(now);

        ArgumentCaptor<PaymentTransaction> transactionCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getTransactionType()).isEqualTo(PaymentTransactionType.REFUND_REQUEST);
        assertThat(transactionCaptor.getValue().getStatus()).isEqualTo(RefundStatus.REQUESTED.name());
    }

    @Test
    void rejectsRefundRequestForOrderThatIsNotPaid() {
        Instant now = Instant.parse("2026-08-15T10:00:00Z");
        User customer = TestEntityFactory.user("buyer@example.com");
        Order order = pendingOrder(customer, now.plusSeconds(900));

        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(customer));
        when(checkoutQueryRepository.findUserOrderById(customer.getId(), order.getId())).thenReturn(Optional.of(order));
        when(refundRequestRepository.existsByOrder_IdAndStatus(order.getId(), RefundStatus.REQUESTED)).thenReturn(false);

        assertThatThrownBy(() -> requestRefundService.request("buyer@example.com", order.getId(), "Changed plans."))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Only paid orders can be submitted for refund.");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        verify(refundRequestRepository, never()).save(any(RefundRequest.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
    }

    private Order paidOrder(User customer, Instant expiresAt) {
        Order order = pendingOrder(customer, expiresAt);
        order.markPaid();
        return order;
    }

    private Order pendingOrder(User customer, Instant expiresAt) {
        Event event = TestEntityFactory.event(
                TestEntityFactory.user("organizer@example.com"),
                TestEntityFactory.category("Concerts", "concerts"),
                TestEntityFactory.venue("Palace", "Kyiv"),
                "rock-night",
                EventStatus.PUBLISHED);
        Order order = new Order(customer, event, expiresAt);
        order.updateTotals(BigDecimal.valueOf(50), "EUR");
        TestEntityFactory.setId(order, UUID.randomUUID());
        return order;
    }

    private Payment payment(User customer, Order order, Instant now) {
        Payment payment = new Payment(
                customer,
                order,
                "PAYPAL",
                "paypal-ref",
                order.getTotalAmount(),
                order.getCurrency(),
                now);
        TestEntityFactory.setId(payment, UUID.randomUUID());
        payment.markPaid(now.plusSeconds(30));
        return payment;
    }
}
