package nure.com.eventflow.payment.infra;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import nure.com.eventflow.payment.domain.Payment;
import nure.com.eventflow.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrder_IdAndCustomer_Id(UUID orderId, UUID customerId);

    Optional<Payment> findByIdAndCustomer_Id(UUID paymentId, UUID customerId);

    Optional<Payment> findByProviderNameAndProviderReference(String providerName, String providerReference);

    List<Payment> findTop20ByStatusInOrderByStartedAtDesc(Collection<PaymentStatus> statuses);

    long countByStatus(PaymentStatus status);

    long countByStatusIn(Collection<PaymentStatus> statuses);
}
