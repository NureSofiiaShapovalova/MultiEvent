package nure.com.eventflow.payment.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import nure.com.eventflow.checkout.domain.Order;
import nure.com.eventflow.checkout.infra.CheckoutQueryRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StartPaymentService {

    private final UserRepository userRepository;
    private final CheckoutQueryRepository checkoutQueryRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentPolicy paymentPolicy;
    private final PaymentViewMapper paymentViewMapper;
    private final Clock clock;

    public StartPaymentService(
            UserRepository userRepository,
            CheckoutQueryRepository checkoutQueryRepository,
            PaymentRepository paymentRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            PaymentGateway paymentGateway,
            PaymentPolicy paymentPolicy,
            PaymentViewMapper paymentViewMapper,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.checkoutQueryRepository = checkoutQueryRepository;
        this.paymentRepository = paymentRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentGateway = paymentGateway;
        this.paymentPolicy = paymentPolicy;
        this.paymentViewMapper = paymentViewMapper;
        this.clock = clock;
    }

    @Transactional
    public PaymentPageView start(String email, UUID orderId) {
        User customer = findCustomer(email);
        Instant now = clock.instant();
        Order order = checkoutQueryRepository.findUserOrderById(customer.getId(), orderId)
                .orElseThrow(() -> new NotFoundException("Order was not found."));

        Payment payment = paymentRepository.findByOrder_IdAndCustomer_Id(orderId, customer.getId())
                .orElseGet(() -> createPayment(customer, order, now));
        paymentPolicy.assertAmountsMatch(order, payment.getAmount(), payment.getCurrency());
        return paymentViewMapper.toView(payment, now);
    }

    private Payment createPayment(User customer, Order order, Instant now) {
        paymentPolicy.assertCanStart(order, now);
        paymentPolicy.assertAmountsMatch(order, order.getTotalAmount(), order.getCurrency());

        PaymentGatewaySession session = paymentGateway.start(new PaymentGatewayStartRequest(
                order.getId(),
                order.getTotalAmount(),
                order.getCurrency()));
        Payment payment = new Payment(
                customer,
                order,
                session.providerName(),
                session.providerReference(),
                session.approvalUrl(),
                order.getTotalAmount(),
                order.getCurrency(),
                now);
        payment.addAttempt(new PaymentAttempt(
                PaymentAttemptType.START,
                PaymentStatus.PENDING,
                session.providerName(),
                session.providerReference(),
                order.getTotalAmount(),
                order.getCurrency(),
                null));

        try {
            Payment savedPayment = paymentRepository.saveAndFlush(payment);
            paymentTransactionRepository.save(new PaymentTransaction(
                    savedPayment,
                    PaymentTransactionType.ORDER_CREATED,
                    session.providerName(),
                    session.providerReference(),
                    order.getTotalAmount(),
                    order.getCurrency(),
                    PaymentStatus.PENDING.name(),
                    session.rawResponse(),
                    now));
            return savedPayment;
        } catch (DataIntegrityViolationException exception) {
            return paymentRepository.findByOrder_IdAndCustomer_Id(order.getId(), customer.getId())
                    .orElseThrow(() -> exception);
        }
    }

    private User findCustomer(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }
}
