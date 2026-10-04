package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.Flock;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlockRepository extends JpaRepository<Flock, Long> {

    List<Flock> findByFarmId(Long farmId);

    Optional<Flock> findByFarmIdAndFlockCode(Long farmId, String flockCode);

    boolean existsByFarmIdAndFlockCode(Long farmId, String flockCode);

    List<Flock> findByFarmIdAndSpecies(Long farmId, Flock.Species species);

    List<Flock> findByFarmIdAndStatus(Long farmId, Flock.FlockStatus status);

    long countByFarmIdAndStatus(Long farmId, Flock.FlockStatus status);}