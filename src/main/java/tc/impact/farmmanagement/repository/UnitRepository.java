package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.Unit;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {

    List<Unit> findByFarmIdOrderByDisplayOrderAscNameAsc(Long farmId);

    List<Unit> findByFarmIdAndActiveTrueOrderByDisplayOrderAscNameAsc(Long farmId);

    Optional<Unit> findByFarmIdAndCode(Long farmId, String code);

    boolean existsByFarmIdAndCode(Long farmId, String code);
}