package nure.com.eventflow.ticketing.infra;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import nure.com.eventflow.ticketing.domain.TicketType;
import nure.com.eventflow.catalog.domain.EventStatus;
import org.springframework.stereotype.Repository;

@Repository
public class TicketingQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<TicketType> findOrganizerTicketTypes(UUID organizerId, UUID eventId) {
        return entityManager.createQuery("""
                        select distinct tt
                        from TicketType tt
                        join fetch tt.event e
                        left join fetch tt.inventory inv
                        where e.organizer.id = :organizerId
                          and e.id = :eventId
                        order by tt.createdAt desc
                        """, TicketType.class)
                .setParameter("organizerId", organizerId)
                .setParameter("eventId", eventId)
                .getResultList();
    }

    public Optional<TicketType> findOrganizerTicketType(UUID organizerId, UUID eventId, UUID ticketTypeId) {
        return entityManager.createQuery("""
                        select distinct tt
                        from TicketType tt
                        join fetch tt.event e
                        left join fetch tt.inventory inv
                        where e.organizer.id = :organizerId
                          and e.id = :eventId
                          and tt.id = :ticketTypeId
                        """, TicketType.class)
                .setParameter("organizerId", organizerId)
                .setParameter("eventId", eventId)
                .setParameter("ticketTypeId", ticketTypeId)
                .getResultStream()
                .findFirst();
    }

    public List<TicketType> findPublicTicketTypesByEventSlug(String slug) {
        return findPublicSellableTicketTypesByEventSlug(slug, Instant.now());
    }

    public List<TicketType> findPublicSellableTicketTypesByEventSlug(String slug, Instant now) {
        return entityManager.createQuery("""
                        select distinct tt
                        from TicketType tt
                        join fetch tt.event e
                        join fetch tt.inventory inv
                        where e.slug = :slug
                          and e.status = :published
                          and tt.active = true
                          and tt.salesStart <= :now
                          and tt.salesEnd >= :now
                          and inv.totalQuantity > (inv.reservedQuantity + inv.soldQuantity)
                        order by tt.createdAt asc
                        """, TicketType.class)
                .setParameter("slug", slug)
                .setParameter("published", EventStatus.PUBLISHED)
                .setParameter("now", now)
                .getResultList();
    }

    public Optional<TicketType> findPublicSellableTicketTypeBySlugAndId(String slug, UUID ticketTypeId, Instant now) {
        return entityManager.createQuery("""
                        select distinct tt
                        from TicketType tt
                        join fetch tt.event e
                        join fetch tt.inventory inv
                        where e.slug = :slug
                          and e.status = :published
                          and tt.id = :ticketTypeId
                          and tt.active = true
                          and tt.salesStart <= :now
                          and tt.salesEnd >= :now
                          and inv.totalQuantity > (inv.reservedQuantity + inv.soldQuantity)
                        """, TicketType.class)
                .setParameter("slug", slug)
                .setParameter("ticketTypeId", ticketTypeId)
                .setParameter("published", EventStatus.PUBLISHED)
                .setParameter("now", now)
                .getResultStream()
                .findFirst();
    }
}
