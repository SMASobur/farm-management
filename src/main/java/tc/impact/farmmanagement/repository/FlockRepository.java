package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.Flock;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlockRepository extends JpaRepository<Flock, Long> {

    Optional<Flock> findByFlockCode(String flockCode);

    boolean existsByFlockCode(String flockCode);

    List<Flock> findBySpecies(Flock.Species species);

    List<Flock> findByStatus(Flock.FlockStatus status);
}