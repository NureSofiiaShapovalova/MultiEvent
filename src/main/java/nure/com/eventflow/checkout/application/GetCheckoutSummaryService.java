package nure.com.eventflow.checkout.application;

import java.time.Clock;
import java.util.UUID;
import nure.com.eventflow.checkout.infra.CheckoutQueryRepository;
import nure.com.eventflow.iam.application.EmailNormalizer;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCheckoutSummaryService {

    private final UserRepository userRepository;
    private final CheckoutQueryRepository checkoutQueryRepository;
    private final CheckoutViewMapper checkoutViewMapper;
    private final Clock clock;

    public GetCheckoutSummaryService(
            UserRepository userRepository,
            CheckoutQueryRepository checkoutQueryRepository,
            CheckoutViewMapper checkoutViewMapper,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.checkoutQueryRepository = checkoutQueryRepository;
        this.checkoutViewMapper = checkoutViewMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CheckoutSummaryView getById(String email, UUID orderId) {
        User customer = findCustomer(email);
        return checkoutQueryRepository.findUserOrderById(customer.getId(), orderId)
                .map(order -> checkoutViewMapper.toCheckoutSummaryView(order, clock.instant()))
                .orElseThrow(() -> new NotFoundException("Order was not found."));
    }

    private User findCustomer(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }
}
