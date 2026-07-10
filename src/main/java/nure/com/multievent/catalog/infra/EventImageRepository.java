package nure.com.multievent.catalog.infra;

import java.util.UUID;
import nure.com.multievent.catalog.domain.EventImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventImageRepository extends JpaRepository<EventImage, UUID> {
}
