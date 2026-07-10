package nure.com.multievent.organizer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
import nure.com.multievent.organizer.infra.AuditLogRepository;
import nure.com.multievent.organizer.infra.OrganizerEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CreateEventServiceTest {

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

    @Mock
    private Clock clock;

    @InjectMocks
    private CreateEventService createEventService;

    @Test
    void createsDraftEventAndRecordsAuditLog() {
        User organizer = OrganizerTestFixtures.user("organizer@example.com", UUID.randomUUID());
        Category category = OrganizerTestFixtures.category();
        Instant now = Instant.parse("2026-06-22T10:00:00Z");

        when(userRepository.findByEmail("organizer@example.com")).thenReturn(Optional.of(organizer));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(organizerEventRepository.findBySlug("rock-night")).thenReturn(Optional.empty());
        when(venueRepository.save(any(Venue.class))).thenAnswer(invocation -> {
            Venue venue = invocation.getArgument(0);
            ReflectionTestUtils.setField(venue, "id", UUID.randomUUID());
            return venue;
        });
        when(organizerEventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
            return event;
        });
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clock.instant()).thenReturn(now);

        UUID eventId = createEventService.create("organizer@example.com", new CreateEventCommand(
                "Rock Night",
                "rock-night",
                "Short description",
                "Full description",
                category.getId(),
                "Palace",
                "Kyiv",
                "Main street 1",
                "01001",
                "Ukraine",
                Instant.parse("2026-09-01T18:00:00Z"),
                Instant.parse("2026-09-01T21:00:00Z"),
                java.util.List.of("https://example.com/cover.jpg", "https://example.com/gallery.jpg")));

        assertThat(eventId).isNotNull();

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        org.mockito.Mockito.verify(organizerEventRepository).save(eventCaptor.capture());
        Event savedEvent = eventCaptor.getValue();
        assertThat(savedEvent.getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(savedEvent.getImages()).hasSize(2);
        assertThat(savedEvent.getImages().get(0).isCover()).isTrue();
        assertThat(savedEvent.getImages().get(1).getSortOrder()).isEqualTo(1);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        org.mockito.Mockito.verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("CREATE_EVENT");
        assertThat(auditCaptor.getValue().getTargetId()).isEqualTo(eventId);
    }
}
