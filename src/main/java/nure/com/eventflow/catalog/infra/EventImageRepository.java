package nure.com.eventflow.catalog.infra;

import java.util.UUID;
import nure.com.eventflow.catalog.domain.EventImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventImageRepository extends JpaRepository<EventImage, UUID> {
}
