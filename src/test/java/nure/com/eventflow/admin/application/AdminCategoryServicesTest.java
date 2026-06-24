package nure.com.eventflow.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import nure.com.eventflow.catalog.domain.Category;
import nure.com.eventflow.catalog.infra.CategoryRepository;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.organizer.domain.AuditLog;
import nure.com.eventflow.organizer.infra.AuditLogRepository;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import nure.com.eventflow.testsupport.TestEntityFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminCategoryServicesTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private Clock clock;

    private CreateCategoryService createCategoryService;
    private UpdateCategoryService updateCategoryService;

    @BeforeEach
    void setUp() {
        createCategoryService = new CreateCategoryService(userRepository, categoryRepository, auditLogRepository, clock);
        updateCategoryService = new UpdateCategoryService(userRepository, categoryRepository, auditLogRepository, clock);
    }

    @Test
    void createsCategoryAndLogsAction() {
        User admin = TestEntityFactory.user("admin@example.com");
        ReflectionTestUtils.setField(admin, "id", UUID.randomUUID());
        Instant now = Instant.parse("2026-06-23T12:00:00Z");
        Category savedCategory = new Category("Concerts", "concerts");
        ReflectionTestUtils.setField(savedCategory, "id", UUID.randomUUID());

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(categoryRepository.findBySlugIgnoreCase("concerts")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clock.instant()).thenReturn(now);

        UUID categoryId = createCategoryService.create("admin@example.com", new AdminCategoryCommand(" Concerts ", " Concerts "));

        assertThat(categoryId).isEqualTo(savedCategory.getId());
        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        org.mockito.Mockito.verify(categoryRepository).save(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue().getName()).isEqualTo("Concerts");
        assertThat(categoryCaptor.getValue().getSlug()).isEqualTo("concerts");
    }

    @Test
    void rejectsDuplicateCategorySlugOnCreate() {
        User admin = TestEntityFactory.user("admin@example.com");
        ReflectionTestUtils.setField(admin, "id", UUID.randomUUID());
        Category existing = new Category("News", "news");
        ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(categoryRepository.findBySlugIgnoreCase("news")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> createCategoryService.create("admin@example.com", new AdminCategoryCommand("News", "News")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("A category with this slug already exists.");
    }

    @Test
    void updatesCategoryAndLogsAction() {
        User admin = TestEntityFactory.user("admin@example.com");
        ReflectionTestUtils.setField(admin, "id", UUID.randomUUID());
        Category category = new Category("Concerts", "concerts");
        ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
        Instant now = Instant.parse("2026-06-23T13:00:00Z");

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(categoryRepository.findBySlugIgnoreCase("live-music")).thenReturn(Optional.empty());
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clock.instant()).thenReturn(now);

        updateCategoryService.update("admin@example.com", category.getId(), new AdminCategoryCommand(" Live Music ", " Live Music "));

        assertThat(category.getName()).isEqualTo("Live Music");
        assertThat(category.getSlug()).isEqualTo("live-music");
    }
}
