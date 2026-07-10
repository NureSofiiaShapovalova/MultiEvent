package nure.com.multievent.checkout.infra;

import java.util.UUID;
import nure.com.multievent.checkout.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}
