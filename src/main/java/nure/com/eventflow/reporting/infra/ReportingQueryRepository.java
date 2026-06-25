package nure.com.eventflow.reporting.infra;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;
import nure.com.eventflow.reporting.application.AdminKpiView;
import nure.com.eventflow.reporting.application.CategoryPopularityRowView;
import nure.com.eventflow.reporting.application.OrganizerReportsView;
import nure.com.eventflow.reporting.application.OrganizerRevenueRowView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class ReportingQueryRepository {

    private final EntityManager entityManager;
    private final String defaultCurrency;

    public ReportingQueryRepository(
            EntityManager entityManager,
            @Value("${eventflow.reporting.default-currency:EUR}") String defaultCurrency) {
        this.entityManager = entityManager;
        this.defaultCurrency = defaultCurrency;
    }

    public AdminKpiView adminKpis() {
        return new AdminKpiView(
                count("SELECT COUNT(*) FROM users"),
                count("""
                        SELECT COUNT(DISTINCT ur.user_id)
                        FROM user_roles ur
                        JOIN roles r ON r.id = ur.role_id
                        WHERE r.name = 'ROLE_ORGANIZER'
                        """),
                count("SELECT COUNT(*) FROM events"),
                count("SELECT COUNT(*) FROM events WHERE status = 'PUBLISHED'"),
                count("SELECT COUNT(*) FROM orders WHERE status = 'PENDING_PAYMENT'"),
                count("SELECT COUNT(*) FROM orders WHERE status = 'PAID'"),
                count("SELECT COUNT(*) FROM payments WHERE status = 'FAILED'"),
                amount("SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE status = 'PAID'"),
                defaultCurrency,
                categoryPopularity());
    }

    public OrganizerReportsView organizerReports(UUID organizerId) {
        List<OrganizerRevenueRowView> rows = organizerRevenueRows(organizerId);
        long paidOrders = rows.stream().mapToLong(OrganizerRevenueRowView::paidOrders).sum();
        long ticketsSold = rows.stream().mapToLong(OrganizerRevenueRowView::ticketsSold).sum();
        BigDecimal revenue = rows.stream()
                .map(OrganizerRevenueRowView::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrganizerReportsView(
                paidOrders,
                ticketsSold,
                revenue,
                rows.stream().map(OrganizerRevenueRowView::currency).findFirst().orElse(defaultCurrency),
                soldOutTicketTypes(organizerId),
                rows);
    }

    private List<CategoryPopularityRowView> categoryPopularity() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT c.name, COUNT(DISTINCT e.id), COUNT(DISTINCT o.id)
                        FROM categories c
                        LEFT JOIN events e ON e.category_id = c.id
                        LEFT JOIN orders o ON o.event_id = e.id AND o.status = 'PAID'
                        GROUP BY c.id, c.name
                        ORDER BY COUNT(DISTINCT e.id) DESC, c.name ASC
                        """)
                .getResultList();
        return rows.stream()
                .map(row -> new CategoryPopularityRowView(
                        String.valueOf(row[0]),
                        toLong(row[1]),
                        toLong(row[2])))
                .toList();
    }

    private List<OrganizerRevenueRowView> organizerRevenueRows(UUID organizerId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT e.id,
                               e.title,
                               COUNT(DISTINCT o.id),
                               COALESCE(SUM(oi.quantity), 0),
                               COALESCE(SUM(oi.line_total), 0),
                               COALESCE(MIN(oi.currency), ?)
                        FROM events e
                        JOIN orders o ON o.event_id = e.id AND o.status = 'PAID'
                        JOIN order_items oi ON oi.order_id = o.id
                        WHERE e.organizer_id = ?
                        GROUP BY e.id, e.title
                        ORDER BY COALESCE(SUM(oi.line_total), 0) DESC, e.title ASC
                        """)
                .setParameter(1, defaultCurrency)
                .setParameter(2, organizerId)
                .getResultList();
        return rows.stream()
                .map(row -> new OrganizerRevenueRowView(
                        toUuid(row[0]),
                        String.valueOf(row[1]),
                        toLong(row[2]),
                        toLong(row[3]),
                        toBigDecimal(row[4]),
                        String.valueOf(row[5])))
                .toList();
    }

    private long soldOutTicketTypes(UUID organizerId) {
        return count("""
                SELECT COUNT(*)
                FROM ticket_inventory ti
                JOIN ticket_types tt ON tt.id = ti.ticket_type_id
                JOIN events e ON e.id = tt.event_id
                WHERE e.organizer_id = ?
                  AND ti.total_quantity <= ti.reserved_quantity + ti.sold_quantity
                """, organizerId);
    }

    private long count(String sql) {
        return toLong(entityManager.createNativeQuery(sql).getSingleResult());
    }

    private long count(String sql, UUID id) {
        return toLong(entityManager.createNativeQuery(sql).setParameter(1, id).getSingleResult());
    }

    private BigDecimal amount(String sql) {
        return toBigDecimal(entityManager.createNativeQuery(sql).getSingleResult());
    }

    private static long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal amount) {
            return amount;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(String.valueOf(value));
    }

    private static UUID toUuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof byte[] bytes) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(), buffer.getLong());
        }
        return UUID.fromString(String.valueOf(value));
    }
}
