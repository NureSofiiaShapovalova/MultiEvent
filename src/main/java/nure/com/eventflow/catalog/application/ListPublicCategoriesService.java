package nure.com.eventflow.catalog.application;

import java.util.List;
import nure.com.eventflow.catalog.infra.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListPublicCategoriesService {

    private final CategoryRepository categoryRepository;
    private final CatalogViewMapper catalogViewMapper;

    public ListPublicCategoriesService(CategoryRepository categoryRepository, CatalogViewMapper catalogViewMapper) {
        this.categoryRepository = categoryRepository;
        this.catalogViewMapper = catalogViewMapper;
    }

    @Transactional(readOnly = true)
    public List<CategoryOptionView> list() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(catalogViewMapper::toCategoryOptionView)
                .toList();
    }
}
