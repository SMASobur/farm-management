package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.CustomerType;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerTypeRepository extends JpaRepository<CustomerType, Long> {

    List<CustomerType> findByFarmIdOrderByDisplayOrderAscNameAsc(Long farmId);

    List<CustomerType> findByFarmIdAndActiveTrueOrderByDisplayOrderAscNameAsc(Long farmId);

    Optional<CustomerType> findByFarmIdAndCode(Long farmId, String code);

    boolean existsByFarmIdAndCode(Long farmId, String code);
}