package nure.com.eventflow.ticketing.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

class TicketPricingPolicyTest {

    private final TicketPricingPolicy policy = new TicketPricingPolicy();

    @Test
    void acceptsPositivePriceAndIsoCurrency() {
        policy.assertValidPrice(BigDecimal.valueOf(25));
        policy.assertValidCurrency("usd");
    }

    @Test
    void rejectsNonPositivePriceAndBadCurrency() {
        assertThatThrownBy(() -> policy.assertValidPrice(BigDecimal.ZERO))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Price must be positive.");

        assertThatThrownBy(() -> policy.assertValidCurrency("US"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Currency must be a 3-letter ISO code.");
    }
}
