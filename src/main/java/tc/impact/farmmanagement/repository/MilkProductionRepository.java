package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.MilkProduction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MilkProductionRepository extends JpaRepository<MilkProduction, Long> {

    // ============ Basic lookups ============

    List<MilkProduction> findByAnimalId(Long animalId);

    List<MilkProduction> findByDate(LocalDate date);

    Optional<MilkProduction> findByAnimalIdAndDate(Long animalId, LocalDate date);

    boolean existsByAnimalIdAndDate(Long animalId, LocalDate date);

    // ============ Date range queries ============

    List<MilkProduction> findByDateBetweenOrderByDateAsc(LocalDate from, LocalDate to);

    List<MilkProduction> findByAnimalIdAndDateBetweenOrderByDateAsc(
            Long animalId, LocalDate from, LocalDate to);

    // ============ Aggregations ============

    /**
     * Total milk produced on a given day (across all animals).
     */
    @Query("SELECT COALESCE(SUM(m.totalLiters), 0.0) FROM MilkProduction m WHERE m.date = :date")
    Double sumLitersByDate(@Param("date") LocalDate date);

    /**
     * Total milk for a specific animal on a specific day.
     */
    @Query("SELECT COALESCE(SUM(m.totalLiters), 0.0) FROM MilkProduction m " +
            "WHERE m.animal.id = :animalId AND m.date = :date")
    Double sumLitersByAnimalAndDate(@Param("animalId") Long animalId,
                                    @Param("date") LocalDate date);

    /**
     * Total milk for a date range (used in monthly reports).
     */
    @Query("SELECT COALESCE(SUM(m.totalLiters), 0.0) FROM MilkProduction m " +
            "WHERE m.date BETWEEN :from AND :to")
    Double sumLitersBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Daily totals for a date range, grouped by date.
     * Returns: [LocalDate date, Double totalLiters]
     */
    @Query("SELECT m.date, SUM(m.totalLiters) FROM MilkProduction m " +
            "WHERE m.date BETWEEN :from AND :to " +
            "GROUP BY m.date ORDER BY m.date ASC")
    List<Object[]> dailyTotalsBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}