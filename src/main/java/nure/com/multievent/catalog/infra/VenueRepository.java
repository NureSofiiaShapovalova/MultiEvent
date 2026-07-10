package nure.com.multievent.catalog.infra;

import java.util.UUID;
import nure.com.multievent.catalog.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<Venue, UUID> {
}
