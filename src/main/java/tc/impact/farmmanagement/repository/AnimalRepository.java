package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.Animal;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {

    List<Animal> findByFarmId(Long farmId);

    List<Animal> findByFarmIdAndSpeciesId(Long farmId, Long speciesId);

    List<Animal> findByFarmIdAndStatus(Long farmId, Animal.AnimalStatus status);

    List<Animal> findByFarmIdAndSpeciesIdAndStatus(Long farmId, Long speciesId, Animal.AnimalStatus status);

    Optional<Animal> findByFarmIdAndTagNumber(Long farmId, String tagNumber);

    boolean existsByFarmIdAndTagNumber(Long farmId, String tagNumber);

    long countByFarmId(Long farmId);

    long countByFarmIdAndSpeciesId(Long farmId, Long speciesId);

    long countByFarmIdAndSpeciesCode(Long farmId, String speciesCode);
}