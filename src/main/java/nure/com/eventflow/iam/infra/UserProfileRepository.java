package nure.com.eventflow.iam.infra;

import java.util.Optional;
import java.util.UUID;
import nure.com.eventflow.iam.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUserId(UUID userId);
}
