package nure.com.multievent.checkout.infra;

import java.util.UUID;
import nure.com.multievent.checkout.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
}
