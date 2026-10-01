package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tc.impact.farmmanagement.dto.request.AnimalRequest;
import tc.impact.farmmanagement.dto.response.AnimalResponse;
import tc.impact.farmmanagement.entity.Animal;
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
import tc.impact.farmmanagement.repository.AnimalRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnimalService {

    private final AnimalRepository animalRepository;

    public AnimalResponse createAnimal(AnimalRequest request) {
        if (animalRepository.existsByTagNumber(request.getTagNumber())) {
            throw new BusinessException("An animal with tag number '" + request.getTagNumber() + "' already exists");
        }

        Animal animal = new Animal();
        animal.setTagNumber(request.getTagNumber());
        animal.setName(request.getName());
        animal.setSpecies(request.getSpecies());
        animal.setStatus(request.getStatus() != null ? request.getStatus() : Animal.AnimalStatus.ACTIVE);
        animal.setGender(request.getGender());
        animal.setDateOfBirth(request.getDateOfBirth());

        return toResponse(animalRepository.save(animal));
    }

    public List<AnimalResponse> getAllAnimals() {
        return animalRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<AnimalResponse> getAnimalsBySpecies(Animal.Species species) {
        return animalRepository.findBySpecies(species).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<AnimalResponse> getAnimalsBySpeciesAndStatus(Animal.Species species, Animal.AnimalStatus status) {
        return animalRepository.findBySpeciesAndStatus(species, status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public AnimalResponse getAnimalById(Long id) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Animal not found with id: " + id));
        return toResponse(animal);
    }

    public AnimalResponse updateAnimal(Long id, AnimalRequest request) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Animal not found with id: " + id));

        if (!animal.getTagNumber().equals(request.getTagNumber())
                && animalRepository.existsByTagNumber(request.getTagNumber())) {
            throw new BusinessException("An animal with tag number '" + request.getTagNumber() + "' already exists");
        }

        animal.setTagNumber(request.getTagNumber());
        animal.setName(request.getName());
        animal.setSpecies(request.getSpecies());
        animal.setStatus(request.getStatus());
        animal.setGender(request.getGender());
        animal.setDateOfBirth(request.getDateOfBirth());

        return toResponse(animalRepository.save(animal));
    }

    public void deleteAnimal(Long id) {
        if (!animalRepository.existsById(id)) {
            throw new ResourceNotFoundException("Animal not found with id: " + id);
        }
        animalRepository.deleteById(id);
    }

    private AnimalResponse toResponse(Animal animal) {
        AnimalResponse response = new AnimalResponse();
        response.setId(animal.getId());
        response.setTagNumber(animal.getTagNumber());
        response.setName(animal.getName());
        response.setSpecies(animal.getSpecies());
        response.setStatus(animal.getStatus());
        response.setGender(animal.getGender());
        response.setDateOfBirth(animal.getDateOfBirth());
        response.setCreatedAt(animal.getCreatedAt());
        response.setMilkProducer(animal.isMilkProducer());
        return response;
    }
}