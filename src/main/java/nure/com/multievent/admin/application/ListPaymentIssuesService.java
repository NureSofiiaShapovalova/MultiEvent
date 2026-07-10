package nure.com.multievent.admin.application;

import java.util.List;
import nure.com.multievent.payment.domain.PaymentStatus;
import nure.com.multievent.payment.infra.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListPaymentIssuesService {

    private final PaymentRepository paymentRepository;
    private final AdminViewMapper adminViewMapper;

    public ListPaymentIssuesService(PaymentRepository paymentRepository, AdminViewMapper adminViewMapper) {
        this.paymentRepository = paymentRepository;
        this.adminViewMapper = adminViewMapper;
    }

    @Transactional(readOnly = true)
    public List<AdminPaymentIssueRowView> list() {
        return paymentRepository.findTop20ByStatusInOrderByStartedAtDesc(List.of(PaymentStatus.FAILED)).stream()
                .map(adminViewMapper::toPaymentIssueRowView)
                .toList();
    }
}
