package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.Species;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpeciesRepository extends JpaRepository<Species, Long> {

    List<Species> findByFarmIdOrderByDisplayOrderAscNameAsc(Long farmId);

    List<Species> findByFarmIdAndActiveTrueOrderByDisplayOrderAscNameAsc(Long farmId);

    Optional<Species> findByFarmIdAndCode(Long farmId, String code);

    boolean existsByFarmIdAndCode(Long farmId, String code);
}