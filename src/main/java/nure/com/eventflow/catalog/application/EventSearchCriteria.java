package nure.com.eventflow.catalog.application;

import java.time.LocalDate;
import java.util.UUID;

public record EventSearchCriteria(
        String keyword,
        UUID categoryId,
        String city,
        LocalDate dateFrom,
        LocalDate dateTo,
        String sort
) {
}
