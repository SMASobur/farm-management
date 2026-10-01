package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.Animal;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {

    Optional<Animal> findByTagNumber(String tagNumber);

    boolean existsByTagNumber(String tagNumber);

    List<Animal> findBySpecies(Animal.Species species);

    List<Animal> findByStatus(Animal.AnimalStatus status);

    List<Animal> findBySpeciesAndStatus(Animal.Species species, Animal.AnimalStatus status);
}