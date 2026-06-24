package nure.com.eventflow.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.shared.domain.BaseEntity;

@Entity
@Table(name = "events")
public class Event extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(name = "short_description", nullable = false, length = 500)
    private String shortDescription;

    @Column(name = "full_description", nullable = false, columnDefinition = "text")
    private String fullDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.DRAFT;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final List<EventImage> images = new ArrayList<>();

    protected Event() {
    }

    public Event(
            User organizer,
            Category category,
            Venue venue,
            String title,
            String slug,
            String shortDescription,
            String fullDescription,
            EventStatus status,
            Instant startsAt,
            Instant endsAt,
            Instant publishedAt
    ) {
        this.organizer = organizer;
        this.category = category;
        this.venue = venue;
        this.title = title;
        this.slug = slug;
        this.shortDescription = shortDescription;
        this.fullDescription = fullDescription;
        this.status = status;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.publishedAt = publishedAt;
    }

    public User getOrganizer() {
        return organizer;
    }

    public Category getCategory() {
        return category;
    }

    public Venue getVenue() {
        return venue;
    }

    public String getTitle() {
        return title;
    }

    public String getSlug() {
        return slug;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public String getFullDescription() {
        return fullDescription;
    }

    public EventStatus getStatus() {
        return status;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public List<EventImage> getImages() {
        return Collections.unmodifiableList(images);
    }

    public boolean isOwnedBy(UUID organizerId) {
        return organizer != null && organizer.getId() != null && organizer.getId().equals(organizerId);
    }

    public void changeDetails(
            String title,
            String slug,
            String shortDescription,
            String fullDescription,
            Instant startsAt,
            Instant endsAt
    ) {
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.slug = Objects.requireNonNull(slug, "slug must not be null");
        this.shortDescription = Objects.requireNonNull(shortDescription, "shortDescription must not be null");
        this.fullDescription = Objects.requireNonNull(fullDescription, "fullDescription must not be null");
        this.startsAt = Objects.requireNonNull(startsAt, "startsAt must not be null");
        this.endsAt = Objects.requireNonNull(endsAt, "endsAt must not be null");
    }

    public void changeCategory(Category category) {
        this.category = Objects.requireNonNull(category, "category must not be null");
    }

    public void changeVenue(Venue venue) {
        this.venue = Objects.requireNonNull(venue, "venue must not be null");
    }

    public void replaceImages(List<EventImage> newImages) {
        images.clear();
        for (EventImage image : newImages) {
            image.attachTo(this);
            images.add(image);
        }
    }

    public void publish(Instant publishedAt) {
        this.status = EventStatus.PUBLISHED;
        this.publishedAt = Objects.requireNonNull(publishedAt, "publishedAt must not be null");
    }

    public void cancel(Instant cancelledAt) {
        this.status = EventStatus.CANCELLED;
    }
}
