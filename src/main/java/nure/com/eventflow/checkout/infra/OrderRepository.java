package nure.com.eventflow.checkout.infra;

import java.util.UUID;
import nure.com.eventflow.checkout.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}
