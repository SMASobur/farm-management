package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.Customer;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByFarmId(Long farmId);

    List<Customer> findByFarmIdAndCustomerTypeId(Long farmId, Long customerTypeId);

    List<Customer> findByFarmIdAndNameContainingIgnoreCase(Long farmId, String namePart);

    List<Customer> findByFarmIdAndGuestFalse(Long farmId);

    Optional<Customer> findByFarmIdAndPhone(Long farmId, String phone);

    boolean existsByFarmIdAndPhone(Long farmId, String phone);

    long countByFarmId(Long farmId);


}