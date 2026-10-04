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

    List<Customer> findByFarmId(Long farmId);

    Optional<Customer> findByFarmIdAndPhone(Long farmId, String phone);

    boolean existsByFarmIdAndPhone(Long farmId, String phone);

    List<Customer> findByFarmIdAndType(Long farmId, Customer.CustomerType type);

    List<Customer> findByFarmIdAndNameContainingIgnoreCase(Long farmId, String namePart);

    long countByFarmId(Long farmId);
}