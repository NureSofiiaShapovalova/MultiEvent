package nure.com.multievent.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import nure.com.multievent.admin.domain.ModerationAction;
import nure.com.multievent.admin.domain.ModerationActionType;
import nure.com.multievent.admin.infra.ModerationActionRepository;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.catalog.domain.EventStatus;
import nure.com.multievent.catalog.infra.EventRepository;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.notifications.domain.EventPublishedEvent;
import nure.com.multievent.organizer.domain.AuditLog;
import nure.com.multievent.organizer.domain.EventPublishingPolicy;
import nure.com.multievent.organizer.infra.AuditLogRepository;
import nure.com.multievent.organizer.infra.EventPublishHistoryRepository;
import nure.com.multievent.testsupport.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ModerateEventServiceTest {

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

    private ModerateEventService moderateEventService;

    @BeforeEach
    void setUp() {
        moderateEventService = new ModerateEventService(
                userRepository,
                eventRepository,
                moderationActionRepository,
                eventPublishHistoryRepository,
                auditLogRepository,
                new EventPublishingPolicy(),
                applicationEventPublisher,
                clock);
    }

    @Test
    void publishesEventAndRecordsModerationReason() {
        User admin = TestEntityFactory.user("admin@example.com");
        User organizer = TestEntityFactory.user("organizer@example.com");
        Event event = TestEntityFactory.event(
                organizer,
                TestEntityFactory.category("Concerts", "concerts"),
                TestEntityFactory.venue("Palace", "Kyiv"),
                EventStatus.REVIEW);
        Instant now = Instant.parse("2026-06-23T17:00:00Z");

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(clock.instant()).thenReturn(now);

        moderateEventService.moderate(
                "admin@example.com",
                event.getId(),
                new ModerationCommand(ModerationActionType.PUBLISH, "Approved after review"));

        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isEqualTo(now);
        ArgumentCaptor<ModerationAction> moderationCaptor = ArgumentCaptor.forClass(ModerationAction.class);
        verify(moderationActionRepository).save(moderationCaptor.capture());
        assertThat(moderationCaptor.getValue().getReason()).isEqualTo("Approved after review");
        verify(applicationEventPublisher).publishEvent(any(EventPublishedEvent.class));
    }
}
