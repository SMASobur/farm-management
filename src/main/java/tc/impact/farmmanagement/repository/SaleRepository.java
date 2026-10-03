package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.Sale;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    // ============ Lookups ============

    List<Sale> findByCustomerId(Long customerId);

    List<Sale> findByDate(LocalDate date);

    List<Sale> findByDateBetweenOrderByDateAsc(LocalDate from, LocalDate to);

    List<Sale> findByCategoryId(Long categoryId);

    List<Sale> findByCustomerIdAndDateBetweenOrderByDateAsc(Long customerId, LocalDate from, LocalDate to);

    long countByCategoryId(Long categoryId);

    // ============ Dues ============

    @Query("SELECT s FROM Sale s WHERE s.dueAmount > 0 ORDER BY s.date ASC")
    List<Sale> findAllWithDues();

    @Query("SELECT s FROM Sale s WHERE s.customer.id = :customerId AND s.dueAmount > 0 ORDER BY s.date ASC")
    List<Sale> findDuesByCustomer(@Param("customerId") Long customerId);

    // ============ Revenue aggregations ============

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.date = :date")
    BigDecimal sumRevenueByDate(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s " +
            "WHERE s.date BETWEEN :from AND :to")
    BigDecimal sumRevenueBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(s.paidAmount), 0) FROM Sale s " +
            "WHERE s.date BETWEEN :from AND :to")
    BigDecimal sumPaidBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(s.dueAmount), 0) FROM Sale s WHERE s.dueAmount > 0")
    BigDecimal sumAllDues();

    @Query("SELECT COALESCE(SUM(s.dueAmount), 0) FROM Sale s " +
            "WHERE s.date BETWEEN :from AND :to")
    BigDecimal sumOutstandingDuesBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Revenue grouped by category in a date range.
     * Returns: [Category, BigDecimal]
     */
    @Query("SELECT s.category, SUM(s.totalAmount) FROM Sale s " +
            "WHERE s.date BETWEEN :from AND :to " +
            "GROUP BY s.category")
    List<Object[]> sumRevenueByCategoryBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT s.date, SUM(s.totalAmount) FROM Sale s " +
            "WHERE s.date BETWEEN :from AND :to " +
            "GROUP BY s.date ORDER BY s.date ASC")
    List<Object[]> dailyRevenueBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}