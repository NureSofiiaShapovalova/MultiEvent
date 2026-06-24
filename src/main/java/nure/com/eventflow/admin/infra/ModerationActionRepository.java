package nure.com.eventflow.admin.infra;

import java.util.List;
import java.util.UUID;
import nure.com.eventflow.admin.domain.ModerationAction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModerationActionRepository extends JpaRepository<ModerationAction, UUID> {

    List<ModerationAction> findTop50ByOrderByCreatedAtDesc();
}
