package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // ============ Lookups ============

    List<Expense> findByFarmId(Long farmId);

    List<Expense> findByFarmIdAndDate(Long farmId, LocalDate date);

    List<Expense> findByFarmIdAndDateBetweenOrderByDateAsc(Long farmId, LocalDate from, LocalDate to);

    List<Expense> findByFarmIdAndCategoryId(Long farmId, Long categoryId);

    List<Expense> findByFarmIdAndCategoryIdAndDateBetweenOrderByDateAsc(Long farmId, Long categoryId, LocalDate from, LocalDate to);

    List<Expense> findByFarmIdAndWorkerIdOrderByDateDesc(Long farmId, Long workerId);

    long countByCategoryId(Long categoryId);

    // ============ Aggregations ============

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.farm.id = :farmId AND e.date = :date")
    BigDecimal sumAmountByFarmAndDate(@Param("farmId") Long farmId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
            "WHERE e.farm.id = :farmId AND e.date BETWEEN :from AND :to")
    BigDecimal sumAmountByFarmBetween(@Param("farmId") Long farmId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Total expenses grouped by category in a date range.
     * Returns: [Category, BigDecimal]
     */

    @Query("SELECT e.category.code, SUM(e.amount) FROM Expense e " +
            "WHERE e.farm.id = :farmId AND e.date BETWEEN :from AND :to " +
            "GROUP BY e.category.code")
    List<Object[]> sumAmountByCategoryCodeBetween(@Param("farmId") Long farmId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Daily expense totals for a range.
     */
    @Query("SELECT e.date, SUM(e.amount) FROM Expense e " +
            "WHERE e.farm.id = :farmId AND e.date BETWEEN :from AND :to " +
            "GROUP BY e.date ORDER BY e.date ASC")
    List<Object[]> dailyTotalsBetween(@Param("farmId") Long farmId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}