package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.PoolProductType;
import tc.impact.farmmanagement.entity.Sale;
import java.math.BigDecimal;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findByFarmId(Long farmId);

    List<Sale> findByFarmIdAndCustomerId(Long farmId, Long customerId);

    List<Sale> findByFarmIdAndDate(Long farmId, LocalDate date);

    List<Sale> findByFarmIdAndDateBetweenOrderByDateAsc(Long farmId, LocalDate from, LocalDate to);

    List<Sale> findByFarmIdAndCategoryId(Long farmId, Long categoryId);

    List<Sale> findByFarmIdAndCustomerIdAndDateBetweenOrderByDateAsc(Long farmId, Long customerId, LocalDate from, LocalDate to);

    long countByCategoryId(Long categoryId);

    @Query("SELECT s FROM Sale s WHERE s.farm.id = :farmId AND s.dueAmount > 0 ORDER BY s.date ASC")
    List<Sale> findDuesByFarm(@Param("farmId") Long farmId);

    @Query("SELECT s FROM Sale s WHERE s.farm.id = :farmId AND s.customer.id = :customerId AND s.dueAmount > 0 ORDER BY s.date ASC")
    List<Sale> findDuesByFarmAndCustomer(@Param("farmId") Long farmId, @Param("customerId") Long customerId);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.farm.id = :farmId AND s.date = :date")
    BigDecimal sumRevenueByFarmAndDate(@Param("farmId") Long farmId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s " +
            "WHERE s.farm.id = :farmId AND s.date BETWEEN :from AND :to")
    BigDecimal sumRevenueByFarmBetween(@Param("farmId") Long farmId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(s.paidAmount), 0) FROM Sale s " +
            "WHERE s.farm.id = :farmId AND s.date BETWEEN :from AND :to")
    BigDecimal sumPaidByFarmBetween(@Param("farmId") Long farmId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(s.dueAmount), 0) FROM Sale s WHERE s.farm.id = :farmId AND s.dueAmount > 0")
    BigDecimal sumAllDuesByFarm(@Param("farmId") Long farmId);

    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Sale s " +
            "WHERE s.farm.id = :farmId AND s.poolSpecies.id = :speciesId " +
            "AND s.poolProductType = :productType AND s.date = :date")
    Double sumQuantityByPool(
            @Param("farmId") Long farmId,
            @Param("speciesId") Long speciesId,
            @Param("productType") PoolProductType productType,
            @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Sale s " +
            "WHERE s.farm.id = :farmId AND s.poolSpecies.id = :speciesId " +
            "AND s.poolProductType = :productType AND s.date <= :date")
    Double sumQuantityByPoolUpToDate(
            @Param("farmId") Long farmId,
            @Param("speciesId") Long speciesId,
            @Param("productType") PoolProductType productType,
            @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(s.dueAmount), 0) FROM Sale s " +
            "WHERE s.farm.id = :farmId AND s.date BETWEEN :from AND :to")
    BigDecimal sumOutstandingDuesByFarmBetween(@Param("farmId") Long farmId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT s.category.code, SUM(s.totalAmount) FROM Sale s " +
            "WHERE s.farm.id = :farmId AND s.date BETWEEN :from AND :to " +
            "GROUP BY s.category.code")
    List<Object[]> sumRevenueByCategoryCodeBetween(@Param("farmId") Long farmId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT s.date, SUM(s.totalAmount) FROM Sale s " +
            "WHERE s.farm.id = :farmId AND s.date BETWEEN :from AND :to " +
            "GROUP BY s.date ORDER BY s.date ASC")
    List<Object[]> dailyRevenueBetween(@Param("farmId") Long farmId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT s.category.id, s.category.code, s.category.name, SUM(s.totalAmount) FROM Sale s " +
            "WHERE s.farm.id = :farmId AND s.date BETWEEN :from AND :to " +
            "GROUP BY s.category.id, s.category.code, s.category.name")
    List<Object[]> sumRevenueByCategoryBetween(@Param("farmId") Long farmId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(s.dueAmount), 0) FROM Sale s " +
            "WHERE s.customer.id = :customerId AND s.farm.id = :farmId AND s.dueAmount > 0")
    BigDecimal sumDueByCustomerAndFarm(@Param("customerId") Long customerId,
                                       @Param("farmId") Long farmId);

    @Query("SELECT COUNT(DISTINCT s.customer.id) FROM Sale s " +
            "WHERE s.farm.id = :farmId AND s.dueAmount > 0")
    long countCustomersWithDues(@Param("farmId") Long farmId);

    long countByUnitId(Long unitId);


    List<Sale> findByFarmIdOrderByDateDescIdDesc(Long farmId);

    List<Sale> findByFarmIdAndDateBetweenOrderByDateDescIdDesc(Long farmId, LocalDate from, LocalDate to);

    List<Sale> findByFarmIdAndCustomerIdOrderByDateDescIdDesc(Long farmId, Long customerId);

    List<Sale> findByFarmIdAndCategoryIdOrderByDateDescIdDesc(Long farmId, Long categoryId);

    List<Sale> findByFarmIdAndCustomerIdAndDateBetweenOrderByDateDescIdDesc(Long farmId, Long customerId, LocalDate from, LocalDate to);

    List<Sale> findByFarmIdAndCategoryIdAndDateBetweenOrderByDateDescIdDesc(Long farmId, Long categoryId, LocalDate from, LocalDate to);
}