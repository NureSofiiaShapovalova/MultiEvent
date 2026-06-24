package nure.com.eventflow.catalog.infra;

import java.util.UUID;
import nure.com.eventflow.catalog.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<Venue, UUID> {
}
