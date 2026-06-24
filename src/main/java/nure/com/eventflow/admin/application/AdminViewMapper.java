package nure.com.eventflow.admin.application;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import nure.com.eventflow.catalog.domain.Category;
import nure.com.eventflow.catalog.domain.Event;
import nure.com.eventflow.catalog.domain.EventImage;
import nure.com.eventflow.iam.domain.Role;
import nure.com.eventflow.iam.domain.User;
import nure.com.eventflow.organizer.domain.AuditLog;
import nure.com.eventflow.payment.domain.Payment;
import org.springframework.stereotype.Component;

@Component
public class AdminViewMapper {

    public AdminUserRowView toUserRowView(User user) {
        return new AdminUserRowView(
                user.getId(),
                user.getEmail(),
                user.getProfile() != null ? user.getProfile().getFirstName() : null,
                user.getProfile() != null ? user.getProfile().getLastName() : null,
                user.getProfile() != null ? user.getProfile().getCity() : null,
                user.isEnabled(),
                user.isAccountNonLocked(),
                user.getRoles().stream()
                        .sorted((left, right) -> left.getName().name().compareTo(right.getName().name()))
                        .map(Role::getName)
                        .collect(Collectors.toCollection(() -> new LinkedHashSet<>())));
    }

    public AdminCategoryRowView toCategoryRowView(Category category, long eventCount) {
        return new AdminCategoryRowView(category.getId(), category.getName(), category.getSlug(), eventCount);
    }

    public AdminEventRowView toEventRowView(Event event) {
        return new AdminEventRowView(
                event.getId(),
                event.getTitle(),
                event.getSlug(),
                event.getStatus(),
                event.getCategory().getName(),
                event.getOrganizer().getEmail(),
                event.getVenue().getCity(),
                event.getStartsAt(),
                event.getEndsAt(),
                event.getPublishedAt(),
                coverImageUrl(event));
    }

    public AdminPaymentIssueRowView toPaymentIssueRowView(Payment payment) {
        return new AdminPaymentIssueRowView(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getOrder().getEvent().getTitle(),
                payment.getCustomer().getEmail(),
                payment.getOrder().getStatus(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getProviderName(),
                payment.getProviderReference(),
                payment.getStartedAt(),
                payment.getCompletedAt(),
                payment.getFailureReason(),
                payment.getAttempts().size());
    }

    public AdminAuditLogRowView toAuditLogRowView(AuditLog auditLog) {
        return new AdminAuditLogRowView(
                auditLog.getId(),
                auditLog.getCreatedAt(),
                auditLog.getActor() != null ? auditLog.getActor().getEmail() : null,
                auditLog.getTargetType(),
                auditLog.getTargetId(),
                auditLog.getAction(),
                auditLog.getDetails());
    }

    private static String coverImageUrl(Event event) {
        return event.getImages().stream()
                .sorted(Comparator.comparingInt(EventImage::getSortOrder))
                .filter(EventImage::isCover)
                .map(EventImage::getImageUrl)
                .findFirst()
                .orElseGet(() -> event.getImages().stream()
                        .sorted(Comparator.comparingInt(EventImage::getSortOrder))
                        .map(EventImage::getImageUrl)
                        .findFirst()
                        .orElse(null));
    }
}
