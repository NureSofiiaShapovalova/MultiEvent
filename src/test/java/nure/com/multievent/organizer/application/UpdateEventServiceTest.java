package nure.com.multievent.organizer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import nure.com.multievent.catalog.domain.Category;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.catalog.domain.EventImage;
import nure.com.multievent.catalog.domain.EventStatus;
import nure.com.multievent.catalog.domain.Venue;
import nure.com.multievent.catalog.infra.CategoryRepository;
import nure.com.multievent.catalog.infra.VenueRepository;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.organizer.OrganizerTestFixtures;
import nure.com.multievent.organizer.domain.AuditLog;
import nure.com.multievent.organizer.domain.EventOwnershipPolicy;
import nure.com.multievent.organizer.infra.AuditLogRepository;
import nure.com.multievent.organizer.infra.OrganizerEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UpdateEventServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private OrganizerEventRepository organizerEventRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Spy
    private EventOwnershipPolicy eventOwnershipPolicy = new EventOwnershipPolicy();

    @InjectMocks
    private UpdateEventService updateEventService;

    @Test
    void updatesOwnedEventAndRecordsAuditLog() {
        User organizer = OrganizerTestFixtures.user("organizer@example.com", UUID.randomUUID());
        Category category = OrganizerTestFixtures.category();
        Venue venue = OrganizerTestFixtures.venue();
        Event event = new Event(
                organizer,
                category,
                venue,
                "Rock Night",
                "rock-night",
                "Short description",
                "Full description",
                EventStatus.DRAFT,
                Instant.parse("2026-09-01T18:00:00Z"),
                Instant.parse("2026-09-01T21:00:00Z"),
                null);
        ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
        event.replaceImages(List.of(new EventImage(event, "https://example.com/old.jpg", true, 0)));

        Category jazz = new Category("Jazz", "jazz");
        ReflectionTestUtils.setField(jazz, "id", UUID.randomUUID());

        when(userRepository.findByEmail("organizer@example.com")).thenReturn(Optional.of(organizer));
        when(organizerEventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(organizerEventRepository.findBySlug("jazz-night")).thenReturn(Optional.empty());
        when(categoryRepository.findById(jazz.getId())).thenReturn(Optional.of(jazz));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        updateEventService.update("organizer@example.com", event.getId(), new UpdateEventCommand(
                "Jazz Night",
                "jazz-night",
                "Fresh short description",
                "Fresh full description",
                jazz.getId(),
                "Opera Hall",
                "Lviv",
                "Prospekt 2",
                "79000",
                "Ukraine",
                Instant.parse("2026-10-01T18:00:00Z"),
                Instant.parse("2026-10-01T21:00:00Z"),
                List.of("https://example.com/new-cover.jpg")));

        assertThat(event.getTitle()).isEqualTo("Jazz Night");
        assertThat(event.getSlug()).isEqualTo("jazz-night");
        assertThat(event.getVenue().getName()).isEqualTo("Opera Hall");
        assertThat(event.getImages()).hasSize(1);
        assertThat(event.getImages().get(0).getImageUrl()).isEqualTo("https://example.com/new-cover.jpg");

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        org.mockito.Mockito.verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("UPDATE_EVENT");
    }
}
