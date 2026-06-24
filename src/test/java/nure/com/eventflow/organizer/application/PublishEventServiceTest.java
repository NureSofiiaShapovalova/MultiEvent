package nure.com.eventflow.organizer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.notifications.domain.EventPublishedEvent;
import nure.com.eventflow.organizer.OrganizerTestFixtures;
import nure.com.eventflow.organizer.domain.AuditLog;
import nure.com.eventflow.organizer.domain.EventOwnershipPolicy;
import nure.com.eventflow.organizer.domain.EventPublishAction;
import nure.com.eventflow.organizer.domain.EventPublishHistory;
import nure.com.eventflow.organizer.domain.EventPublishingPolicy;
import nure.com.eventflow.organizer.infra.AuditLogRepository;
import nure.com.eventflow.organizer.infra.EventPublishHistoryRepository;
import nure.com.eventflow.organizer.infra.OrganizerEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PublishEventServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizerEventRepository organizerEventRepository;

    @Mock
    private EventPublishHistoryRepository eventPublishHistoryRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Spy
    private EventOwnershipPolicy eventOwnershipPolicy = new EventOwnershipPolicy();

    @Spy
    private EventPublishingPolicy eventPublishingPolicy = new EventPublishingPolicy();

    @Mock
    private Clock clock;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private PublishEventService publishEventService;

    @Test
    void publishesEventAndRecordsHistory() {
        User organizer = OrganizerTestFixtures.user("organizer@example.com", UUID.randomUUID());
        Event event = OrganizerTestFixtures.event(
                organizer,
                OrganizerTestFixtures.category(),
                OrganizerTestFixtures.venue(),
                EventStatus.DRAFT);
        ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
        Instant now = Instant.parse("2026-06-22T12:00:00Z");

        when(userRepository.findByEmail("organizer@example.com")).thenReturn(Optional.of(organizer));
        when(organizerEventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(eventPublishHistoryRepository.save(any(EventPublishHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clock.instant()).thenReturn(now);

        publishEventService.publish("organizer@example.com", event.getId());

        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isEqualTo(now);

        ArgumentCaptor<EventPublishHistory> historyCaptor = ArgumentCaptor.forClass(EventPublishHistory.class);
        org.mockito.Mockito.verify(eventPublishHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getAction()).isEqualTo(EventPublishAction.PUBLISHED);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        org.mockito.Mockito.verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("PUBLISH_EVENT");

        ArgumentCaptor<EventPublishedEvent> eventCaptor = ArgumentCaptor.forClass(EventPublishedEvent.class);
        org.mockito.Mockito.verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().eventId()).isEqualTo(event.getId());
        assertThat(eventCaptor.getValue().organizerId()).isEqualTo(organizer.getId());
    }
}
