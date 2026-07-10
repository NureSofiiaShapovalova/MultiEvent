package nure.com.multievent.admin.application;

import java.time.Clock;
import java.util.Locale;
import java.util.UUID;
import nure.com.multievent.catalog.domain.Category;
import nure.com.multievent.catalog.infra.CategoryRepository;
import nure.com.multievent.iam.application.EmailNormalizer;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.organizer.domain.AuditLog;
import nure.com.multievent.organizer.infra.AuditLogRepository;
import nure.com.multievent.shared.exception.BusinessRuleViolationException;
import nure.com.multievent.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateCategoryService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;

    public CreateCategoryService(
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
    public UUID create(String adminEmail, AdminCategoryCommand command) {
        User admin = findAdmin(adminEmail);
        String name = requireText(command.name(), "Category name is required.");
        String slug = normalizeSlug(command.slug());
        assertSlugIsFree(slug, null);

        Category category = categoryRepository.save(new Category(name, slug));
        auditLogRepository.save(new AuditLog(
                admin,
                "Category",
                category.getId(),
                "CREATE_CATEGORY",
                "Created category at " + clock.instant()));
        return category.getId();
    }

    private void assertSlugIsFree(String slug, UUID currentCategoryId) {
        categoryRepository.findBySlugIgnoreCase(slug).ifPresent(existing -> {
            if (currentCategoryId == null || !existing.getId().equals(currentCategoryId)) {
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
