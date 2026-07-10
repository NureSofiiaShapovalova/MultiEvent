package nure.com.multievent.checkout.application;

import java.util.ArrayList;
import java.util.List;
import nure.com.multievent.checkout.domain.Order;
import nure.com.multievent.checkout.domain.ReservationLock;
import nure.com.multievent.ticketing.domain.TicketInventory;
import nure.com.multievent.ticketing.infra.TicketInventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinalizeOrderInventoryService {

    private final TicketInventoryRepository ticketInventoryRepository;

    public FinalizeOrderInventoryService(TicketInventoryRepository ticketInventoryRepository) {
        this.ticketInventoryRepository = ticketInventoryRepository;
    }

    @Transactional
    public void markReservedInventorySold(Order order) {
        List<TicketInventory> inventoriesToSave = new ArrayList<>();
        for (ReservationLock reservationLock : order.getReservationLocks()) {
            if (reservationLock.isActive()) {
                TicketInventory inventory = reservationLock.getTicketType().getInventory();
                inventory.markSold(reservationLock.getReservedQuantity());
                reservationLock.confirm();
                inventoriesToSave.add(inventory);
            }
        }

        if (!inventoriesToSave.isEmpty()) {
            ticketInventoryRepository.saveAll(inventoriesToSave);
        }
    }
}
