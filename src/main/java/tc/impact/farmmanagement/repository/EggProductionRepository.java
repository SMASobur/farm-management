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

    List<EggProduction> findByAnimalId(Long animalId);

    List<EggProduction> findByFlockId(Long flockId);

    List<EggProduction> findByDate(LocalDate date);

    List<EggProduction> findByDateBetweenOrderByDateAsc(LocalDate from, LocalDate to);

    Optional<EggProduction> findByAnimalIdAndDate(Long animalId, LocalDate date);

    Optional<EggProduction> findByFlockIdAndDate(Long flockId, LocalDate date);

    boolean existsByAnimalIdAndDate(Long animalId, LocalDate date);

    boolean existsByFlockIdAndDate(Long flockId, LocalDate date);

    // ============ Aggregations ============

    @Query("SELECT COALESCE(SUM(e.totalEggs), 0) FROM EggProduction e WHERE e.date = :date")
    Long sumEggsByDate(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(e.totalEggs), 0) FROM EggProduction e " +
            "WHERE e.date BETWEEN :from AND :to")
    Long sumEggsBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT e.date, SUM(e.totalEggs) FROM EggProduction e " +
            "WHERE e.date BETWEEN :from AND :to " +
            "GROUP BY e.date ORDER BY e.date ASC")
    List<Object[]> dailyTotalsBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}