package nure.com.eventflow.catalog.infra;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import nure.com.eventflow.catalog.application.EventSearchCriteria;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.catalog.domain.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class EventCatalogQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Page<Event> searchPublishedEvents(EventSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Event> query = cb.createQuery(Event.class);
        Root<Event> event = query.from(Event.class);
        event.fetch("category");
        event.fetch("venue");

        List<Predicate> predicates = buildPredicates(criteria, cb, event);
        query.select(event).distinct(true).where(predicates.toArray(Predicate[]::new));
        applySorting(criteria.sort(), cb, query, event);

        TypedQuery<Event> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        long total = countPublishedEvents(criteria);
        return new PageImpl<>(typedQuery.getResultList(), pageable, total);
    }

    public List<Event> findUpcomingPublishedEvents(Instant now, int limit) {
        return entityManager.createQuery("""
                        select e
                        from Event e
                        join fetch e.category
                        join fetch e.venue
                        where e.status = :status
                        and e.startsAt >= :now
                        order by e.startsAt asc
                        """, Event.class)
                .setParameter("status", EventStatus.PUBLISHED)
                .setParameter("now", now)
                .setMaxResults(limit)
                .getResultList();
    }

    public List<Event> findNewestPublishedEvents(int limit) {
        return entityManager.createQuery("""
                        select e
                        from Event e
                        join fetch e.category
                        join fetch e.venue
                        where e.status = :status
                        order by e.createdAt desc
                        """, Event.class)
                .setParameter("status", EventStatus.PUBLISHED)
                .setMaxResults(limit)
                .getResultList();
    }

    public Optional<Event> findPublishedEventBySlug(String slug) {
        return entityManager.createQuery("""
                        select distinct e
                        from Event e
                        join fetch e.category
                        join fetch e.venue
                        join fetch e.organizer
                        left join fetch e.images
                        where e.slug = :slug and e.status = :status
                        order by e.startsAt asc
                        """, Event.class)
                .setParameter("slug", slug)
                .setParameter("status", EventStatus.PUBLISHED)
                .getResultStream()
                .findFirst();
    }

    private List<Predicate> buildPredicates(EventSearchCriteria criteria, CriteriaBuilder cb, Root<Event> event) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(event.get("status"), EventStatus.PUBLISHED));

        if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
            String keyword = "%" + criteria.keyword().trim().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(event.get("title")), keyword),
                    cb.like(cb.lower(event.get("shortDescription")), keyword),
                    cb.like(cb.lower(event.get("fullDescription")), keyword)));
        }

        if (criteria.categoryId() != null) {
            predicates.add(cb.equal(event.get("category").get("id"), criteria.categoryId()));
        }

        if (criteria.city() != null && !criteria.city().isBlank()) {
            predicates.add(cb.equal(cb.lower(event.get("venue").get("city")), criteria.city().trim().toLowerCase()));
        }

        if (criteria.dateFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(event.get("startsAt"), criteria.dateFrom().atStartOfDay().toInstant(ZoneOffset.UTC)));
        }

        if (criteria.dateTo() != null) {
            LocalDate exclusiveDateTo = criteria.dateTo().plusDays(1);
            predicates.add(cb.lessThan(event.get("startsAt"), exclusiveDateTo.atStartOfDay().toInstant(ZoneOffset.UTC)));
        }

        return predicates;
    }

    private void applySorting(
            String sort,
            CriteriaBuilder cb,
            CriteriaQuery<Event> query,
            Root<Event> event
    ) {
        if ("startsAtDesc".equals(sort)) {
            query.orderBy(cb.desc(event.get("startsAt")));
            return;
        }

        if ("newest".equals(sort)) {
            query.orderBy(cb.desc(event.get("createdAt")));
            return;
        }

        query.orderBy(cb.asc(event.get("startsAt")));
    }

    private long countPublishedEvents(EventSearchCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Event> event = countQuery.from(Event.class);
        countQuery.select(cb.countDistinct(event));
        countQuery.where(buildPredicates(criteria, cb, event).toArray(Predicate[]::new));
        return entityManager.createQuery(countQuery).getSingleResult();
    }
}
