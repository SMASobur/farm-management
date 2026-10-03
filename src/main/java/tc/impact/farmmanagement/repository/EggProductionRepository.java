package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.EggProduction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EggProductionRepository extends JpaRepository<EggProduction, Long> {

    // ============ Lookups ============
    List<EggProduction> findByFarmId(Long farmId);

    List<EggProduction> findByFarmIdAndAnimalId(Long farmId, Long animalId);

    List<EggProduction> findByFarmIdAndFlockId(Long farmId, Long flockId);

    List<EggProduction> findByFarmIdAndDate(Long farmId, LocalDate date);

    List<EggProduction> findByFarmIdAndDateBetweenOrderByDateAsc(Long farmId, LocalDate from, LocalDate to);

    Optional<EggProduction> findByFarmIdAndAnimalIdAndDate(Long farmId, Long animalId, LocalDate date);

    Optional<EggProduction> findByFarmIdAndFlockIdAndDate(Long farmId, Long flockId, LocalDate date);

    boolean existsByFarmIdAndAnimalIdAndDate(Long farmId, Long animalId, LocalDate date);

    boolean existsByFarmIdAndFlockIdAndDate(Long farmId, Long flockId, LocalDate date);

    // ============ Aggregations ============

    @Query("SELECT COALESCE(SUM(e.totalEggs), 0) FROM EggProduction e WHERE e.farm.id = :farmId AND e.date = :date")
    Long sumEggsByFarmAndDate(@Param("farmId") Long farmId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(e.totalEggs), 0) FROM EggProduction e " +
            "WHERE e.farm.id = :farmId AND e.date BETWEEN :from AND :to")
    Long sumEggsByFarmBetween(@Param("farmId") Long farmId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT e.date, SUM(e.totalEggs) FROM EggProduction e " +
            "WHERE e.farm.id = :farmId AND e.date BETWEEN :from AND :to " +
            "GROUP BY e.date ORDER BY e.date ASC")
    List<Object[]> dailyTotalsBetween(@Param("farmId") Long farmId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}