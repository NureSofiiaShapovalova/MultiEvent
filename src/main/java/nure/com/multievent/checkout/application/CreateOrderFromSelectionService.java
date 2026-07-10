package nure.com.multievent.checkout.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import nure.com.multievent.catalog.infra.EventCatalogQueryRepository;
import nure.com.multievent.catalog.domain.Event;
import nure.com.multievent.checkout.domain.Order;
import nure.com.multievent.checkout.domain.OrderItem;
import nure.com.multievent.checkout.domain.OrderPricingService;
import nure.com.multievent.checkout.domain.ReservationLock;
import nure.com.multievent.checkout.domain.ReservationPolicy;
import nure.com.multievent.checkout.infra.OrderRepository;
import nure.com.multievent.iam.application.EmailNormalizer;
import nure.com.multievent.iam.domain.User;
import nure.com.multievent.iam.infra.UserRepository;
import nure.com.multievent.shared.exception.BusinessRuleViolationException;
import nure.com.multievent.shared.exception.NotFoundException;
import nure.com.multievent.ticketing.domain.TicketInventory;
import nure.com.multievent.ticketing.domain.TicketType;
import nure.com.multievent.ticketing.infra.TicketInventoryRepository;
import nure.com.multievent.ticketing.infra.TicketingQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateOrderFromSelectionService {

    private static final Duration RESERVATION_HOLD_DURATION = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final EventCatalogQueryRepository eventCatalogQueryRepository;
    private final TicketingQueryRepository ticketingQueryRepository;
    private final TicketInventoryRepository ticketInventoryRepository;
    private final OrderRepository orderRepository;
    private final ReservationPolicy reservationPolicy;
    private final OrderPricingService orderPricingService;
    private final Clock clock;

    public CreateOrderFromSelectionService(
            UserRepository userRepository,
            EventCatalogQueryRepository eventCatalogQueryRepository,
            TicketingQueryRepository ticketingQueryRepository,
            TicketInventoryRepository ticketInventoryRepository,
            OrderRepository orderRepository,
            ReservationPolicy reservationPolicy,
            OrderPricingService orderPricingService,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.eventCatalogQueryRepository = eventCatalogQueryRepository;
        this.ticketingQueryRepository = ticketingQueryRepository;
        this.ticketInventoryRepository = ticketInventoryRepository;
        this.orderRepository = orderRepository;
        this.reservationPolicy = reservationPolicy;
        this.orderPricingService = orderPricingService;
        this.clock = clock;
    }

    @Transactional
    public UUID create(String email, String slug, TicketSelectionCommand command) {
        User customer = findCustomer(email);
        Instant now = clock.instant();
        Map<UUID, Integer> quantityByTicketTypeId = new LinkedHashMap<>();
        for (TicketSelectionCommand.Selection selection : command.selections()) {
            if (selection.quantity() > 0) {
                quantityByTicketTypeId.merge(selection.ticketTypeId(), selection.quantity(), Integer::sum);
            }
        }
        if (quantityByTicketTypeId.isEmpty()) {
            throw new BusinessRuleViolationException("Select at least one ticket.");
        }

        Event event = eventCatalogQueryRepository.findPublishedEventBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Event was not found."));
        List<TicketType> sellableTicketTypes = ticketingQueryRepository.findPublicSellableTicketTypesByEventSlug(slug, now);
        Map<UUID, TicketType> ticketTypesById = new LinkedHashMap<>();
        for (TicketType ticketType : sellableTicketTypes) {
            ticketTypesById.put(ticketType.getId(), ticketType);
        }

        Order order = new Order(customer, event, now.plus(RESERVATION_HOLD_DURATION));
        List<TicketInventory> inventoriesToSave = new ArrayList<>();
        String currency = null;

        for (Map.Entry<UUID, Integer> selection : quantityByTicketTypeId.entrySet()) {
            TicketType ticketType = ticketTypesById.get(selection.getKey());
            if (ticketType == null) {
                throw new NotFoundException("Ticket type was not found.");
            }
            TicketInventory inventory = ticketType.getInventory();
            reservationPolicy.assertCanReserve(ticketType, inventory, selection.getValue(), now);

            if (currency == null) {
                currency = ticketType.getCurrency();
            } else if (!currency.equals(ticketType.getCurrency())) {
                throw new BusinessRuleViolationException("Order currency must be consistent.");
            }

            OrderItem item = new OrderItem(ticketType, selection.getValue());
            ReservationLock reservationLock = new ReservationLock(ticketType, selection.getValue(), order.getExpiresAt());
            order.addItem(item);
            order.addReservationLock(reservationLock);
            inventory.reserve(selection.getValue());
            inventoriesToSave.add(inventory);
        }

        BigDecimal totalAmount = orderPricingService.calculateTotal(order.getItems());
        order.updateTotals(totalAmount, currency);
        orderRepository.save(order);
        ticketInventoryRepository.saveAll(inventoriesToSave);
        return order.getId();
    }

    private User findCustomer(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("Current user was not found."));
    }
}
