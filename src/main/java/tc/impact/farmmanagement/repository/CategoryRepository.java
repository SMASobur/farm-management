package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.Category;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByTypeOrderByDisplayOrderAscNameAsc(Category.CategoryType type);

    List<Category> findByTypeAndActiveTrueOrderByDisplayOrderAscNameAsc(Category.CategoryType type);

    Optional<Category> findByCodeAndType(String code, Category.CategoryType type);

    boolean existsByCodeAndType(String code, Category.CategoryType type);
}