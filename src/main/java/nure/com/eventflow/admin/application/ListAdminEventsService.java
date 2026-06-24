package nure.com.eventflow.admin.application;

import java.util.List;
import nure.com.eventflow.catalog.infra.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListAdminEventsService {

    private final EventRepository eventRepository;
    private final AdminViewMapper adminViewMapper;

    public ListAdminEventsService(EventRepository eventRepository, AdminViewMapper adminViewMapper) {
        this.eventRepository = eventRepository;
        this.adminViewMapper = adminViewMapper;
    }

    @Transactional(readOnly = true)
    public List<AdminEventRowView> list() {
        return eventRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(adminViewMapper::toEventRowView)
                .toList();
    }
}
