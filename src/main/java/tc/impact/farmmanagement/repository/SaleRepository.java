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

    List<Sale> findByProductType(Sale.ProductType productType);

    List<Sale> findByCustomerIdAndDateBetweenOrderByDateAsc(Long customerId, LocalDate from, LocalDate to);

    // ============ Dues ============

    /**
     * All sales that still have outstanding dues (dueAmount > 0).
     */
    @Query("SELECT s FROM Sale s WHERE s.dueAmount > 0 ORDER BY s.date ASC")
    List<Sale> findAllWithDues();

    /**
     * All dues for a specific customer.
     */
    @Query("SELECT s FROM Sale s WHERE s.customer.id = :customerId AND s.dueAmount > 0 ORDER BY s.date ASC")
    List<Sale> findDuesByCustomer(@Param("customerId") Long customerId);

    // ============ Revenue aggregations ============

    /**
     * Total sales revenue (gross) on a given day.
     */
    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.date = :date")
    BigDecimal sumRevenueByDate(@Param("date") LocalDate date);

    /**
     * Total sales revenue in a date range.
     */
    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s " +
            "WHERE s.date BETWEEN :from AND :to")
    BigDecimal sumRevenueBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Total cash received (paid) in a date range.
     * Note: uses Sale.paidAmount — does NOT look at Payment.date.
     * For date-accurate cash flow, use PaymentRepository.sumAmountBetween().
     */
    @Query("SELECT COALESCE(SUM(s.paidAmount), 0) FROM Sale s " +
            "WHERE s.date BETWEEN :from AND :to")
    BigDecimal sumPaidBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Total outstanding dues (all customers).
     */
    @Query("SELECT COALESCE(SUM(s.dueAmount), 0) FROM Sale s WHERE s.dueAmount > 0")
    BigDecimal sumAllDues();

    /**
     * Total revenue by product type in a date range (for the report module).
     */
    @Query("SELECT s.productType, SUM(s.totalAmount) FROM Sale s " +
            "WHERE s.date BETWEEN :from AND :to " +
            "GROUP BY s.productType")
    List<Object[]> sumRevenueByProductTypeBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Daily revenue totals for a range — used for the dashboard chart.
     */
    @Query("SELECT s.date, SUM(s.totalAmount) FROM Sale s " +
            "WHERE s.date BETWEEN :from AND :to " +
            "GROUP BY s.date ORDER BY s.date ASC")
    List<Object[]> dailyRevenueBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}