package nure.com.eventflow.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import nure.com.eventflow.shared.domain.BaseEntity;

@Entity
@Table(name = "event_images")
public class EventImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "is_cover", nullable = false)
    private boolean cover;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected EventImage() {
    }

    public EventImage(Event event, String imageUrl, boolean cover, int sortOrder) {
        this.event = event;
        this.imageUrl = imageUrl;
        this.cover = cover;
        this.sortOrder = sortOrder;
    }

    public Event getEvent() {
        return event;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isCover() {
        return cover;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    void attachTo(Event event) {
        this.event = event;
    }
}
