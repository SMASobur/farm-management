package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.Customer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhone(String phone);

    boolean existsByPhone(String phone);

    List<Customer> findByType(Customer.CustomerType type);

    List<Customer> findByNameContainingIgnoreCase(String namePart);

    // ============ Aggregation ============

    /**
     * Total outstanding due for a specific customer across all their sales.
     */
    @Query("SELECT COALESCE(SUM(s.dueAmount), 0) FROM Sale s WHERE s.customer.id = :customerId")
    BigDecimal sumDueByCustomer(@Param("customerId") Long customerId);
}