package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.Animal;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {

    List<Animal> findByFarmId(Long farmId);

    Optional<Animal> findByFarmIdAndTagNumber(Long farmId, String tagNumber);

    boolean existsByFarmIdAndTagNumber(Long farmId, String tagNumber);

    List<Animal> findByFarmIdAndSpecies(Long farmId, Animal.Species species);

    List<Animal> findByFarmIdAndStatus(Long farmId, Animal.AnimalStatus status);

    List<Animal> findByFarmIdAndSpeciesAndStatus(Long farmId, Animal.Species species, Animal.AnimalStatus status);

    long countByFarmId(Long farmId);

    long countByFarmIdAndSpecies(Long farmId, Animal.Species species);

    long countByFarmIdAndStatus(Long farmId, Animal.AnimalStatus status);

}