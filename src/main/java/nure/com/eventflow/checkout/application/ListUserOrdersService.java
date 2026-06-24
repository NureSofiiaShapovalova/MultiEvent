package nure.com.eventflow.checkout.application;

import java.util.List;
import nure.com.eventflow.checkout.infra.CheckoutQueryRepository;
import nure.com.eventflow.iam.application.EmailNormalizer;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListUserOrdersService {

    private final UserRepository userRepository;
    private final CheckoutQueryRepository checkoutQueryRepository;
    private final CheckoutViewMapper checkoutViewMapper;

    public ListUserOrdersService(
            UserRepository userRepository,
            CheckoutQueryRepository checkoutQueryRepository,
            CheckoutViewMapper checkoutViewMapper
    ) {
        this.userRepository = userRepository;
        this.checkoutQueryRepository = checkoutQueryRepository;
        this.checkoutViewMapper = checkoutViewMapper;
    }

    @Transactional(readOnly = true)
    public List<UserOrderRowView> list(String email) {
        User customer = findCustomer(email);
        return checkoutQueryRepository.findUserOrders(customer.getId())
                .stream()
                .map(checkoutViewMapper::toUserOrderRowView)
                .toList();
    }

    private User findCustomer(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }
}
