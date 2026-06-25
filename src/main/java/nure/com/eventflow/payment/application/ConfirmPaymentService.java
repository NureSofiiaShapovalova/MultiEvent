package nure.com.eventflow.payment.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import nure.com.eventflow.checkout.domain.Order;
import nure.com.eventflow.checkout.application.FinalizeOrderInventoryService;
import nure.com.eventflow.checkout.infra.OrderRepository;
import nure.com.eventflow.iam.application.EmailNormalizer;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.payment.domain.Payment;
import nure.com.eventflow.payment.domain.PaymentAttempt;
import nure.com.eventflow.payment.domain.PaymentAttemptType;
import nure.com.eventflow.payment.domain.PaymentGateway;
import nure.com.eventflow.payment.domain.PaymentGatewayResult;
import nure.com.eventflow.payment.domain.PaymentPolicy;
import nure.com.eventflow.payment.domain.PaymentStatus;
import nure.com.eventflow.payment.domain.PaymentTransaction;
import nure.com.eventflow.payment.domain.PaymentTransactionType;
import nure.com.eventflow.payment.infra.PaymentRepository;
import nure.com.eventflow.payment.infra.PaymentTransactionRepository;
import nure.com.eventflow.notifications.domain.OrderPaidEvent;
import nure.com.eventflow.shared.exception.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfirmPaymentService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentPolicy paymentPolicy;
    private final PaymentViewMapper paymentViewMapper;
    private final FinalizeOrderInventoryService finalizeOrderInventoryService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Clock clock;

    public ConfirmPaymentService(
            UserRepository userRepository,
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            PaymentGateway paymentGateway,
            PaymentPolicy paymentPolicy,
            PaymentViewMapper paymentViewMapper,
            FinalizeOrderInventoryService finalizeOrderInventoryService,
            ApplicationEventPublisher applicationEventPublisher,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentGateway = paymentGateway;
        this.paymentPolicy = paymentPolicy;
        this.paymentViewMapper = paymentViewMapper;
        this.finalizeOrderInventoryService = finalizeOrderInventoryService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.clock = clock;
    }

    @Transactional
    public PaymentPageView confirm(String email, UUID orderId) {
        User customer = findCustomer(email);
        Instant now = clock.instant();
        Payment payment = paymentRepository.findByOrder_IdAndCustomer_Id(orderId, customer.getId())
                .orElseThrow(() -> new NotFoundException("Payment was not found."));
        Order order = payment.getOrder();

        if (payment.isPaid()) {
            return paymentViewMapper.toView(payment, now);
        }

        paymentPolicy.assertCanConfirm(order, payment, now);

        PaymentGatewayResult result = paymentGateway.confirm(payment);
        PaymentStatus resultingStatus = result.status();
        payment.addAttempt(new PaymentAttempt(
                PaymentAttemptType.CONFIRM,
                resultingStatus,
                payment.getProviderName(),
                providerTransactionReference(payment, result),
                payment.getAmount(),
                payment.getCurrency(),
                result.failureReason()));
        paymentTransactionRepository.save(new PaymentTransaction(
                payment,
                PaymentTransactionType.CAPTURE,
                payment.getProviderName(),
                providerTransactionReference(payment, result),
                payment.getAmount(),
                payment.getCurrency(),
                resultingStatus.name(),
                result.rawResponse(),
                now));

        if (result.successful()) {
            finalizeOrderInventoryService.markReservedInventorySold(order);
            payment.markPaid(now);
            order.markPaid();
            orderRepository.save(order);
            applicationEventPublisher.publishEvent(new OrderPaidEvent(order.getId(), customer.getId()));
        } else if (resultingStatus == PaymentStatus.FAILED) {
            payment.markFailed(result.failureReason(), now);
        }

        return paymentViewMapper.toView(paymentRepository.save(payment), now);
    }

    private String providerTransactionReference(Payment payment, PaymentGatewayResult result) {
        return result.providerTransactionReference() == null || result.providerTransactionReference().isBlank()
                ? payment.getProviderReference()
                : result.providerTransactionReference();
    }

    private User findCustomer(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }
}
