package nure.com.eventflow.checkout.infra;

import java.util.UUID;
import nure.com.eventflow.checkout.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
}
