package nure.com.multievent.admin.application;

import java.util.List;
import nure.com.multievent.catalog.infra.CategoryRepository;
import nure.com.multievent.catalog.infra.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListAdminCategoriesService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final AdminViewMapper adminViewMapper;

    public ListAdminCategoriesService(
            CategoryRepository categoryRepository,
            EventRepository eventRepository,
            AdminViewMapper adminViewMapper
    ) {
        this.categoryRepository = categoryRepository;
        this.eventRepository = eventRepository;
        this.adminViewMapper = adminViewMapper;
    }

    @Transactional(readOnly = true)
    public List<AdminCategoryRowView> list() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(category -> adminViewMapper.toCategoryRowView(category, eventRepository.countByCategory_Id(category.getId())))
                .toList();
    }
}
