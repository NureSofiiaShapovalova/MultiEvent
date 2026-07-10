package nure.com.multievent.payment.infra;

import java.util.List;
import java.util.UUID;
import nure.com.multievent.payment.domain.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    List<PaymentTransaction> findByPayment_IdOrderByOccurredAtAsc(UUID paymentId);
}
