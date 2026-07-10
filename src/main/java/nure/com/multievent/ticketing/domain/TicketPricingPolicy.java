package nure.com.multievent.ticketing.domain;

import java.math.BigDecimal;
import java.util.Locale;
import nure.com.multievent.shared.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Component;

@Component
public class TicketPricingPolicy {

    public void assertValidPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleViolationException("Price must be positive.");
        }
    }

    public void assertValidCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new BusinessRuleViolationException("Currency is required.");
        }
        String normalized = currency.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() != 3) {
            throw new BusinessRuleViolationException("Currency must be a 3-letter ISO code.");
        }
    }
}
