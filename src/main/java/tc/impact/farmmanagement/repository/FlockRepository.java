package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.Flock;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlockRepository extends JpaRepository<Flock, Long> {

    List<Flock> findByFarmId(Long farmId);

    List<Flock> findByFarmIdAndStatus(Long farmId, Flock.FlockStatus status);

    List<Flock> findByFarmIdAndSpeciesId(Long farmId, Long speciesId);

    Optional<Flock> findByFarmIdAndFlockCode(Long farmId, String flockCode);

    boolean existsByFarmIdAndFlockCode(Long farmId, String flockCode);

    long countByFarmIdAndStatus(Long farmId, Flock.FlockStatus status);
}