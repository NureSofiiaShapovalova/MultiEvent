package nure.com.eventflow.admin.application;

import java.time.Clock;
import java.util.Locale;
import java.util.UUID;
import nure.com.eventflow.catalog.domain.Category;
import nure.com.eventflow.catalog.infra.CategoryRepository;
import nure.com.eventflow.iam.application.EmailNormalizer;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.iam.infra.UserRepository;
import nure.com.eventflow.organizer.domain.AuditLog;
import nure.com.eventflow.organizer.infra.AuditLogRepository;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import nure.com.eventflow.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateCategoryService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;

    public UpdateCategoryService(
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            AuditLogRepository auditLogRepository,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.auditLogRepository = auditLogRepository;
        this.clock = clock;
    }

    @Transactional
    public void update(String adminEmail, UUID categoryId, AdminCategoryCommand command) {
        User admin = findAdmin(adminEmail);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category was not found."));
        String name = requireText(command.name(), "Category name is required.");
        String slug = normalizeSlug(command.slug());
        assertSlugIsFree(slug, category.getId());

        category.update(name, slug);
        categoryRepository.save(category);
        auditLogRepository.save(new AuditLog(
                admin,
                "Category",
                category.getId(),
                "UPDATE_CATEGORY",
                "Updated category at " + clock.instant()));
    }

    private void assertSlugIsFree(String slug, UUID currentCategoryId) {
        categoryRepository.findBySlugIgnoreCase(slug).ifPresent(existing -> {
            if (!existing.getId().equals(currentCategoryId)) {
                throw new BusinessRuleViolationException("A category with this slug already exists.");
            }
        });
    }

    private User findAdmin(String adminEmail) {
        String normalizedEmail = EmailNormalizer.normalize(adminEmail);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleViolationException(message);
        }
        return value.trim();
    }

    private static String normalizeSlug(String value) {
        String normalized = requireText(value, "Category slug is required.")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s_]+", "-")
                .replaceAll("[^\\p{L}\\p{Nd}-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        if (normalized.isBlank()) {
            throw new BusinessRuleViolationException("Category slug is required.");
        }
        return normalized;
    }
}
