package nure.com.multievent.catalog.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import nure.com.multievent.catalog.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findAllByOrderByNameAsc();

    Optional<Category> findBySlugIgnoreCase(String slug);
}
