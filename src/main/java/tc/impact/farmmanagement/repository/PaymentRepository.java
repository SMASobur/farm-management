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

    List<Payment> findByFarmId(Long farmId);

    List<Payment> findByFarmIdAndSaleId(Long farmId, Long saleId);

    List<Payment> findByFarmIdAndSaleIdOrderByDateAsc(Long farmId, Long saleId);

    List<Payment> findByFarmIdAndDate(Long farmId, LocalDate date);

    List<Payment> findByFarmIdAndDateBetweenOrderByDateAsc(Long farmId, LocalDate from, LocalDate to);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.sale.id = :saleId")
    BigDecimal sumAmountBySale(@Param("saleId") Long saleId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.farm.id = :farmId AND p.date = :date")
    BigDecimal sumAmountByFarmAndDate(@Param("farmId") Long farmId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.farm.id = :farmId AND p.date BETWEEN :from AND :to")
    BigDecimal sumAmountByFarmBetween(@Param("farmId") Long farmId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT p.method, SUM(p.amount) FROM Payment p " +
            "WHERE p.farm.id = :farmId AND p.date BETWEEN :from AND :to " +
            "GROUP BY p.method")
    List<Object[]> sumAmountByMethodBetween(@Param("farmId") Long farmId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}