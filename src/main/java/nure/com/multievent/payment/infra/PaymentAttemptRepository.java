package nure.com.multievent.payment.infra;

import java.util.List;
import java.util.UUID;
import nure.com.multievent.payment.domain.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {

    List<PaymentAttempt> findByPayment_IdOrderByCreatedAtAsc(UUID paymentId);
}
