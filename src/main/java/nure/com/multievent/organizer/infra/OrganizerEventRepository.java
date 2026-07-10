package nure.com.multievent.organizer.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.catalog.domain.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizerEventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findByOrganizer_IdOrderByCreatedAtDesc(UUID organizerId, Pageable pageable);

    List<Event> findTop5ByOrganizer_IdOrderByCreatedAtDesc(UUID organizerId);

    long countByOrganizer_Id(UUID organizerId);

    long countByOrganizer_IdAndStatus(UUID organizerId, EventStatus status);

    Optional<Event> findBySlug(String slug);

    @EntityGraph(attributePaths = {"category", "venue", "images", "organizer"})
    Optional<Event> findByIdAndOrganizer_Id(UUID eventId, UUID organizerId);
}
