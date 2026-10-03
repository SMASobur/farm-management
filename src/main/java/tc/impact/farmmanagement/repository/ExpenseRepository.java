package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.Category;
import tc.impact.farmmanagement.entity.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // ============ Lookups ============

    List<Expense> findByDate(LocalDate date);

    List<Expense> findByDateBetweenOrderByDateAsc(LocalDate from, LocalDate to);

    List<Expense> findByCategoryId(Long categoryId);

    List<Expense> findByCategoryIdAndDateBetweenOrderByDateAsc(Long categoryId, LocalDate from, LocalDate to);

    long countByCategoryId(Long categoryId);

    // ============ Aggregations ============

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.date = :date")
    BigDecimal sumAmountByDate(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
            "WHERE e.date BETWEEN :from AND :to")
    BigDecimal sumAmountBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Total expenses grouped by category in a date range.
     * Returns: [Category, BigDecimal]
     */
    @Query("SELECT e.category, SUM(e.amount) FROM Expense e " +
            "WHERE e.date BETWEEN :from AND :to " +
            "GROUP BY e.category")
    List<Object[]> sumByCategoryBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Daily expense totals for a range.
     */
    @Query("SELECT e.date, SUM(e.amount) FROM Expense e " +
            "WHERE e.date BETWEEN :from AND :to " +
            "GROUP BY e.date ORDER BY e.date ASC")
    List<Object[]> dailyTotalsBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}