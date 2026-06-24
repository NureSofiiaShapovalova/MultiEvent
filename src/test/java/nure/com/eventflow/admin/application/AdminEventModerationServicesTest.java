package nure.com.eventflow.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import nure.com.eventflow.admin.domain.ModerationAction;
import nure.com.eventflow.admin.infra.ModerationActionRepository;
import nure.com.eventflow.catalog.domain.Category;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.catalog.domain.Venue;
import nure.com.eventflow.catalog.infra.EventRepository;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.notifications.domain.EventCancelledEvent;
import nure.com.eventflow.notifications.domain.EventPublishedEvent;
import nure.com.eventflow.organizer.domain.AuditLog;
import nure.com.eventflow.organizer.domain.EventPublishingPolicy;
import nure.com.eventflow.organizer.infra.AuditLogRepository;
import nure.com.eventflow.organizer.infra.EventPublishHistoryRepository;
import nure.com.eventflow.testsupport.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminEventModerationServicesTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ModerationActionRepository moderationActionRepository;

    @Mock
    private EventPublishHistoryRepository eventPublishHistoryRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private Clock clock;

    private PublishAdminEventService publishAdminEventService;
    private CancelAdminEventService cancelAdminEventService;

    @BeforeEach
    void setUp() {
        EventPublishingPolicy policy = new EventPublishingPolicy();
        publishAdminEventService = new PublishAdminEventService(
                userRepository,
                eventRepository,
                moderationActionRepository,
                eventPublishHistoryRepository,
                auditLogRepository,
                policy,
                applicationEventPublisher,
                clock);
        cancelAdminEventService = new CancelAdminEventService(
                userRepository,
                eventRepository,
                moderationActionRepository,
                eventPublishHistoryRepository,
                auditLogRepository,
                policy,
                applicationEventPublisher,
                clock);
    }

    @Test
    void publishesEventAndEmitsNotificationEvent() {
        User admin = TestEntityFactory.user("admin@example.com");
        User organizer = TestEntityFactory.user("organizer@example.com");
        ReflectionTestUtils.setField(admin, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(organizer, "id", UUID.randomUUID());
        Event event = TestEntityFactory.event(
                organizer,
                TestEntityFactory.category("Concerts", "concerts"),
                TestEntityFactory.venue("Palace", "Kyiv"),
                EventStatus.DRAFT);
        Instant now = Instant.parse("2026-06-23T14:00:00Z");

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clock.instant()).thenReturn(now);

        publishAdminEventService.publish("admin@example.com", event.getId());

        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isEqualTo(now);

        ArgumentCaptor<EventPublishedEvent> captor = ArgumentCaptor.forClass(EventPublishedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().eventId()).isEqualTo(event.getId());
        assertThat(captor.getValue().organizerId()).isEqualTo(organizer.getId());

        ArgumentCaptor<ModerationAction> moderationCaptor = ArgumentCaptor.forClass(ModerationAction.class);
        verify(moderationActionRepository).save(moderationCaptor.capture());
        assertThat(moderationCaptor.getValue().getAction().name()).isEqualTo("PUBLISH");
    }

    @Test
    void cancelsEventAndEmitsNotificationEvent() {
        User admin = TestEntityFactory.user("admin@example.com");
        User organizer = TestEntityFactory.user("organizer@example.com");
        ReflectionTestUtils.setField(admin, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(organizer, "id", UUID.randomUUID());
        Event event = TestEntityFactory.event(
                organizer,
                TestEntityFactory.category("Concerts", "concerts"),
                TestEntityFactory.venue("Palace", "Kyiv"),
                EventStatus.PUBLISHED);
        Instant now = Instant.parse("2026-06-23T15:00:00Z");

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clock.instant()).thenReturn(now);

        cancelAdminEventService.cancel("admin@example.com", event.getId());

        assertThat(event.getStatus()).isEqualTo(EventStatus.CANCELLED);

        ArgumentCaptor<EventCancelledEvent> captor = ArgumentCaptor.forClass(EventCancelledEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().eventId()).isEqualTo(event.getId());
        assertThat(captor.getValue().organizerId()).isEqualTo(organizer.getId());

        ArgumentCaptor<ModerationAction> moderationCaptor = ArgumentCaptor.forClass(ModerationAction.class);
        verify(moderationActionRepository).save(moderationCaptor.capture());
        assertThat(moderationCaptor.getValue().getAction().name()).isEqualTo("CANCEL");
    }
}
