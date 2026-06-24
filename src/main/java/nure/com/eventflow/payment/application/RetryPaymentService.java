package nure.com.eventflow.payment.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import nure.com.eventflow.checkout.domain.Order;
import nure.com.eventflow.checkout.infra.OrderRepository;
import nure.com.eventflow.iam.application.EmailNormalizer;
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
import nure.com.eventflow.payment.domain.PaymentTransaction;
import nure.com.eventflow.payment.domain.PaymentTransactionType;
import nure.com.eventflow.payment.infra.PaymentRepository;
import nure.com.eventflow.payment.infra.PaymentTransactionRepository;
import nure.com.eventflow.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RetryPaymentService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentPolicy paymentPolicy;
    private final PaymentViewMapper paymentViewMapper;
    private final Clock clock;

    public RetryPaymentService(
            UserRepository userRepository,
            PaymentRepository paymentRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            OrderRepository orderRepository,
            PaymentGateway paymentGateway,
            PaymentPolicy paymentPolicy,
            PaymentViewMapper paymentViewMapper,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
        this.paymentPolicy = paymentPolicy;
        this.paymentViewMapper = paymentViewMapper;
        this.clock = clock;
    }

    @Transactional
    public PaymentPageView retry(String email, UUID orderId) {
        User customer = findCustomer(email);
        Instant now = clock.instant();
        Payment payment = paymentRepository.findByOrder_IdAndCustomer_Id(orderId, customer.getId())
                .orElseThrow(() -> new NotFoundException("Payment was not found."));
        Order order = payment.getOrder();

        paymentPolicy.assertCanRetry(order, payment, now);

        PaymentGatewaySession session = paymentGateway.start(new PaymentGatewayStartRequest(
                order.getId(),
                payment.getAmount(),
                payment.getCurrency()));
        payment.addAttempt(new PaymentAttempt(
                PaymentAttemptType.RETRY,
                PaymentStatus.PENDING,
                session.providerName(),
                session.providerReference(),
                payment.getAmount(),
                payment.getCurrency(),
                null));
        payment.retry(session.providerName(), session.providerReference(), session.approvalUrl(), now);
        orderRepository.save(order);
        paymentTransactionRepository.save(new PaymentTransaction(
                payment,
                PaymentTransactionType.ORDER_CREATED,
                session.providerName(),
                session.providerReference(),
                payment.getAmount(),
                payment.getCurrency(),
                PaymentStatus.PENDING.name(),
                session.rawResponse(),
                now));

        return paymentViewMapper.toView(paymentRepository.save(payment), now);
    }

    private User findCustomer(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }
}
