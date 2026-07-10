package nure.com.multievent.payment.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import nure.com.multievent.checkout.domain.Order;
import nure.com.multievent.checkout.infra.CheckoutQueryRepository;
import nure.com.multievent.checkout.infra.OrderRepository;
import nure.com.multievent.iam.application.EmailNormalizer;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.payment.domain.Payment;
import nure.com.multievent.payment.domain.PaymentTransaction;
import nure.com.multievent.payment.domain.PaymentTransactionType;
import nure.com.multievent.payment.domain.RefundRequest;
import nure.com.multievent.payment.domain.RefundStatus;
import nure.com.multievent.payment.infra.PaymentRepository;
import nure.com.multievent.payment.infra.PaymentTransactionRepository;
import nure.com.multievent.payment.infra.RefundRequestRepository;
import nure.com.multievent.shared.exception.BusinessRuleViolationException;
import nure.com.multievent.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestRefundService {

    private final UserRepository userRepository;
    private final CheckoutQueryRepository checkoutQueryRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final Clock clock;

    public RequestRefundService(
            UserRepository userRepository,
            CheckoutQueryRepository checkoutQueryRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            RefundRequestRepository refundRequestRepository,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.checkoutQueryRepository = checkoutQueryRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.refundRequestRepository = refundRequestRepository;
        this.clock = clock;
    }

    @Transactional
    public UUID request(String email, UUID orderId, String reason) {
        User customer = findCustomer(email);
        Order order = checkoutQueryRepository.findUserOrderById(customer.getId(), orderId)
                .orElseThrow(() -> new NotFoundException("Order was not found."));

        if (refundRequestRepository.existsByOrder_IdAndStatus(order.getId(), RefundStatus.REQUESTED)) {
            throw new BusinessRuleViolationException("A refund request for this order already exists.");
        }
        if (!order.isPaid()) {
            throw new BusinessRuleViolationException("Only paid orders can be submitted for refund.");
        }

        Instant now = clock.instant();
        RefundRequest refundRequest = refundRequestRepository.save(new RefundRequest(order, customer, reason, now));
        order.markRefundPending();
        orderRepository.save(order);
        recordRefundRequestTransaction(order, reason, now);
        return refundRequest.getId();
    }

    private void recordRefundRequestTransaction(Order order, String reason, Instant now) {
        paymentRepository.findByOrder_Id(order.getId()).ifPresent(payment -> paymentTransactionRepository.save(new PaymentTransaction(
                payment,
                PaymentTransactionType.REFUND_REQUEST,
                payment.getProviderName(),
                payment.getProviderReference(),
                payment.getAmount(),
                payment.getCurrency(),
                RefundStatus.REQUESTED.name(),
                reason,
                now)));
    }

    private User findCustomer(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }
}
