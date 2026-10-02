package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findBySaleId(Long saleId);

    List<Payment> findByDate(LocalDate date);

    List<Payment> findByDateBetweenOrderByDateAsc(LocalDate from, LocalDate to);

    List<Payment> findBySaleIdOrderByDateAsc(Long saleId);

    // ============ Aggregations ============

    /**
     * Sum of all payments for a sale. Used to recompute Sale.paidAmount.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.sale.id = :saleId")
    BigDecimal sumAmountBySale(@Param("saleId") Long saleId);

    /**
     * Total cash received on a specific date.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.date = :date")
    BigDecimal sumAmountByDate(@Param("date") LocalDate date);

    /**
     * Total cash received in a date range (true cash flow).
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.date BETWEEN :from AND :to")
    BigDecimal sumAmountBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Cash received by payment method in a date range (bkash vs cash analysis).
     */
    @Query("SELECT p.method, SUM(p.amount) FROM Payment p " +
            "WHERE p.date BETWEEN :from AND :to " +
            "GROUP BY p.method")
    List<Object[]> sumAmountByMethodBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}