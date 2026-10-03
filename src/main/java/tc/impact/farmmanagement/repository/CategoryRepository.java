package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.Category;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByFarmIdAndTypeOrderByDisplayOrderAscNameAsc(Long farmId, Category.CategoryType type);

    List<Category> findByFarmIdAndTypeAndActiveTrueOrderByDisplayOrderAscNameAsc(Long farmId, Category.CategoryType type);

    Optional<Category> findByFarmIdAndCodeAndType(Long farmId, String code, Category.CategoryType type);

    boolean existsByFarmIdAndCodeAndType(Long farmId, String code, Category.CategoryType type);

    List<Category> findByFarmId(Long farmId);
}