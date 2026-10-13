package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.ProductionEvent;

import java.util.List;

@Repository
public interface ProductionEventRepository extends JpaRepository<ProductionEvent, Long> {

    List<ProductionEvent> findByFarmIdAndAnimalIdOrderByDateDesc(Long farmId, Long animalId);

    List<ProductionEvent> findByFarmIdAndAnimalIdAndEventTypeOrderByDateDesc(
            Long farmId, Long animalId, ProductionEvent.EventType eventType);

    long countByFarmIdAndAnimalId(Long farmId, Long animalId);

    @Query("SELECT e.animal.id, MAX(e.date) FROM ProductionEvent e " +
            "WHERE e.farm.id = :farmId " +
            "  AND e.eventType = :eventType " +
            "  AND e.animal.id IS NOT NULL " +
            "GROUP BY e.animal.id")
    List<Object[]> findLatestDatesByEventType(
            @Param("farmId") Long farmId,
            @Param("eventType") ProductionEvent.EventType eventType);
}