package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.request.AnimalRequest;
import tc.impact.farmmanagement.dto.response.AnimalResponse;
import tc.impact.farmmanagement.entity.Animal;
import tc.impact.farmmanagement.entity.Farm;
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
import tc.impact.farmmanagement.repository.AnimalRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final FarmContext farmContext;

    @Transactional
    public AnimalResponse createAnimal(AnimalRequest request) {
        Farm farm = farmContext.getCurrentFarm();

        if (animalRepository.existsByFarmIdAndTagNumber(farm.getId(), request.getTagNumber())) {
            throw new BusinessException("An animal with tag number '" + request.getTagNumber() + "' already exists");
        }

        Animal animal = new Animal();
        animal.setFarm(farm);
        animal.setTagNumber(request.getTagNumber());
        animal.setName(request.getName());
        animal.setSpecies(request.getSpecies());
        animal.setStatus(request.getStatus() != null ? request.getStatus() : Animal.AnimalStatus.ACTIVE);
        animal.setGender(request.getGender());
        animal.setDateOfBirth(request.getDateOfBirth());

        return toResponse(animalRepository.save(animal));
    }

    @Transactional(readOnly = true)
    public List<AnimalResponse> getAllAnimals() {
        Long farmId = farmContext.getCurrentFarmId();
        return animalRepository.findByFarmId(farmId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnimalResponse> getAnimalsBySpecies(Animal.Species species) {
        Long farmId = farmContext.getCurrentFarmId();
        return animalRepository.findByFarmIdAndSpecies(farmId, species).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnimalResponse> getAnimalsBySpeciesAndStatus(Animal.Species species, Animal.AnimalStatus status) {
        Long farmId = farmContext.getCurrentFarmId();
        return animalRepository.findByFarmIdAndSpeciesAndStatus(farmId, species, status).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AnimalResponse getAnimalById(Long id) {
        Long farmId = farmContext.getCurrentFarmId();
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Animal not found with id: " + id));
        assertSameFarm(animal.getFarm().getId(), farmId);
        return toResponse(animal);
    }

    @Transactional
    public AnimalResponse updateAnimal(Long id, AnimalRequest request) {
        Long farmId = farmContext.getCurrentFarmId();
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Animal not found with id: " + id));
        assertSameFarm(animal.getFarm().getId(), farmId);

        if (!animal.getTagNumber().equals(request.getTagNumber())
                && animalRepository.existsByFarmIdAndTagNumber(farmId, request.getTagNumber())) {
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

    @Transactional
    public void deleteAnimal(Long id) {
        Long farmId = farmContext.getCurrentFarmId();
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Animal not found with id: " + id));
        assertSameFarm(animal.getFarm().getId(), farmId);
        animalRepository.deleteById(id);
    }

    private void assertSameFarm(Long entityFarmId, Long currentFarmId) {
        if (!entityFarmId.equals(currentFarmId)) {
            throw new ResourceNotFoundException("Resource not found");
        }
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