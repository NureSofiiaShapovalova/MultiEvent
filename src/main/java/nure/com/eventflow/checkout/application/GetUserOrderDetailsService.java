package nure.com.eventflow.checkout.application;

import java.util.UUID;
import nure.com.eventflow.checkout.infra.CheckoutQueryRepository;
import nure.com.eventflow.iam.application.EmailNormalizer;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetUserOrderDetailsService {

    private final UserRepository userRepository;
    private final CheckoutQueryRepository checkoutQueryRepository;
    private final CheckoutViewMapper checkoutViewMapper;

    public GetUserOrderDetailsService(
            UserRepository userRepository,
            CheckoutQueryRepository checkoutQueryRepository,
            CheckoutViewMapper checkoutViewMapper
    ) {
        this.userRepository = userRepository;
        this.checkoutQueryRepository = checkoutQueryRepository;
        this.checkoutViewMapper = checkoutViewMapper;
    }

    @Transactional(readOnly = true)
    public UserOrderDetailsView getById(String email, UUID orderId) {
        User customer = findCustomer(email);
        return checkoutQueryRepository.findUserOrderById(customer.getId(), orderId)
                .map(checkoutViewMapper::toUserOrderDetailsView)
                .orElseThrow(() -> new NotFoundException("Order was not found."));
    }

    private User findCustomer(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }
}
