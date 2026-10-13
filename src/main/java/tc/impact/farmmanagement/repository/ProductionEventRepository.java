package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.ProductionEvent;

import java.util.List;

@Repository
public interface ProductionEventRepository extends JpaRepository<ProductionEvent, Long> {

    List<ProductionEvent> findByFarmIdAndAnimalIdOrderByDateDesc(Long farmId, Long animalId);

    List<ProductionEvent> findByFarmIdAndAnimalIdAndEventTypeOrderByDateDesc(
            Long farmId, Long animalId, ProductionEvent.EventType eventType);

    long countByFarmIdAndAnimalId(Long farmId, Long animalId);
}