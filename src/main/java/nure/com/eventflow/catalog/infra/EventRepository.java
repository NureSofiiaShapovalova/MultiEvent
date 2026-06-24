package nure.com.eventflow.catalog.infra;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.catalog.domain.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, UUID> {

    @EntityGraph(attributePaths = {"category", "venue", "images", "organizer"})
    Optional<Event> findBySlugAndStatus(String slug, EventStatus status);

    @EntityGraph(attributePaths = {"category", "venue", "images", "organizer"})
    Page<Event> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"category", "venue", "images", "organizer"})
    List<Event> findTop10ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"category", "venue", "images", "organizer"})
    List<Event> findTop50ByOrderByCreatedAtDesc();

    long countByStatus(EventStatus status);

    long countByStatusIn(Collection<EventStatus> statuses);

    long countByCategory_Id(UUID categoryId);
}
