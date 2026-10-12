package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.request.AnimalRequest;
import tc.impact.farmmanagement.dto.response.AnimalResponse;
import tc.impact.farmmanagement.entity.Animal;
import tc.impact.farmmanagement.entity.Farm;
import tc.impact.farmmanagement.entity.Species;
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
import tc.impact.farmmanagement.repository.AnimalRepository;
import tc.impact.farmmanagement.repository.SpeciesRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final SpeciesRepository speciesRepository;
    private final FarmContext farmContext;

    // ============ Create ============

    @Transactional
    public AnimalResponse createAnimal(AnimalRequest request) {
        Farm farm = farmContext.getCurrentFarm();

        if (animalRepository.existsByFarmIdAndTagNumber(farm.getId(), request.getTagNumber())) {
            throw new BusinessException("An animal with tag number '" + request.getTagNumber() + "' already exists");
        }

        Species species = resolveSpecies(request, farm);

        Animal animal = new Animal();
        animal.setFarm(farm);
        animal.setTagNumber(request.getTagNumber());
        animal.setName(request.getName());
        animal.setSpecies(species);
        animal.setStatus(request.getStatus() != null ? request.getStatus() : Animal.AnimalStatus.ACTIVE);
        animal.setGender(request.getGender());
        animal.setDateOfBirth(request.getDateOfBirth());

        validateAnimalRules(animal);

        return toResponse(animalRepository.save(animal));
    }

    // ============ Read ============

    @Transactional(readOnly = true)
    public List<AnimalResponse> getAllAnimals() {
        Long farmId = farmContext.getCurrentFarmId();
        return animalRepository.findByFarmId(farmId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnimalResponse> getAnimalsBySpecies(Long speciesId) {
        Long farmId = farmContext.getCurrentFarmId();
        return animalRepository.findByFarmIdAndSpeciesId(farmId, speciesId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnimalResponse> getAnimalsByStatus(Animal.AnimalStatus status) {
        Long farmId = farmContext.getCurrentFarmId();
        return animalRepository.findByFarmIdAndStatus(farmId, status).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnimalResponse> getAnimalsBySpeciesAndStatus(Long speciesId, Animal.AnimalStatus status) {
        Long farmId = farmContext.getCurrentFarmId();
        return animalRepository.findByFarmIdAndSpeciesIdAndStatus(farmId, speciesId, status).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AnimalResponse getAnimalById(Long id) {
        return toResponse(findOrThrow(id));
    }

    // ============ Update ============

    @Transactional
    public AnimalResponse updateAnimal(Long id, AnimalRequest request) {
        Long farmId = farmContext.getCurrentFarmId();
        Animal animal = findOrThrow(id);

        if (!animal.getTagNumber().equals(request.getTagNumber())
                && animalRepository.existsByFarmIdAndTagNumber(farmId, request.getTagNumber())) {
            throw new BusinessException("An animal with tag number '" + request.getTagNumber() + "' already exists");
        }

        Species species = resolveSpecies(request, farmContext.getCurrentFarm());

        animal.setTagNumber(request.getTagNumber());
        animal.setName(request.getName());
        animal.setSpecies(species);
        animal.setStatus(request.getStatus());
        animal.setGender(request.getGender());
        animal.setDateOfBirth(request.getDateOfBirth());

        validateAnimalRules(animal);

        return toResponse(animalRepository.save(animal));
    }

    // ============ Delete ============

    @Transactional
    public void deleteAnimal(Long id) {
        Animal animal = findOrThrow(id);
        animalRepository.deleteById(animal.getId());
    }

    // ============ Helpers ============

    private Animal findOrThrow(Long id) {
        Long farmId = farmContext.getCurrentFarmId();
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Animal not found with id: " + id));
        if (!animal.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Animal not found with id: " + id);
        }
        return animal;
    }

    private Species resolveSpecies(AnimalRequest request, Farm farm) {
        boolean hasSpeciesId = request.getSpeciesId() != null;
        boolean hasCustomName = request.getCustomSpeciesName() != null
                && !request.getCustomSpeciesName().isBlank();

        if (hasSpeciesId && hasCustomName) {
            throw new BusinessException("Provide either speciesId or customSpeciesName, not both");
        }
        if (!hasSpeciesId && !hasCustomName) {
            throw new BusinessException("Species is required");
        }

        if (hasSpeciesId) {
            Species sp = speciesRepository.findById(request.getSpeciesId())
                    .orElseThrow(() -> new ResourceNotFoundException("Species not found with id: " + request.getSpeciesId()));
            if (!sp.getFarm().getId().equals(farm.getId())) {
                throw new ResourceNotFoundException("Species not found");
            }
            if (!sp.isActive()) {
                throw new BusinessException("Species '" + sp.getName() + "' is inactive");
            }
            return sp;
        }

        String name = request.getCustomSpeciesName().trim();
        String code = normalizeCode(name);
        if (code.isEmpty()) {
            throw new BusinessException("Invalid species name");
        }
        if (code.length() > 50) {
            code = code.substring(0, 50);
        }

        var existing = speciesRepository.findByFarmIdAndCode(farm.getId(), code);
        if (existing.isPresent()) {
            return existing.get();
        }

        boolean saveForReuse = Boolean.TRUE.equals(request.getSaveCustomSpecies());

        Species sp = new Species();
        sp.setFarm(farm);
        sp.setCode(code);
        sp.setName(name);
        sp.setNameBn(
                request.getCustomSpeciesNameBn() != null
                        ? request.getCustomSpeciesNameBn().trim()
                        : null);
        sp.setMilkProducer(Boolean.TRUE.equals(request.getCustomSpeciesMilkProducer()));
        sp.setEggProducer(Boolean.TRUE.equals(request.getCustomSpeciesEggProducer()));
        sp.setActive(saveForReuse);
        sp.setSystemDefault(false);
        sp.setDisplayOrder(900);
        return speciesRepository.save(sp);
    }

    private void validateAnimalRules(Animal animal) {
        Animal.Gender gender = animal.getGender();
        Animal.AnimalStatus status = animal.getStatus();

        boolean isFemale = gender == Animal.Gender.FEMALE;
        boolean isMilkSpecies = animal.getSpecies() != null && animal.getSpecies().isMilkProducer();

        if (status == Animal.AnimalStatus.PREGNANT) {
            if (gender != Animal.Gender.FEMALE) {
                throw new BusinessException("Only female animals can be marked as PREGNANT");
            }
            if (!isMilkSpecies) {
                throw new BusinessException(
                        "Only milk-producing species (cow, goat, sheep, buffalo) can be marked as PREGNANT");
            }
        }

        if (status == Animal.AnimalStatus.DRY) {
            if (!isFemale) {
                throw new BusinessException("Only female animals can be marked as DRY");
            }
            if (!isMilkSpecies) {
                throw new BusinessException("Only milk-producing species can be marked as DRY");
            }
        }
    }

    private String normalizeCode(String name) {
        return name.trim()
                .toUpperCase()
                .replaceAll("[^\\p{L}\\p{N}]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private AnimalResponse toResponse(Animal animal) {
        AnimalResponse response = new AnimalResponse();
        response.setId(animal.getId());
        response.setTagNumber(animal.getTagNumber());
        response.setName(animal.getName());
        response.setSpeciesId(animal.getSpecies().getId());
        response.setSpeciesCode(animal.getSpecies().getCode());
        response.setSpeciesName(animal.getSpecies().getName());
        response.setSpeciesNameBn(animal.getSpecies().getNameBn());
        response.setStatus(animal.getStatus().name());
        response.setGender(animal.getGender() != null ? animal.getGender().name() : null);
        response.setDateOfBirth(animal.getDateOfBirth());
        response.setCreatedAt(animal.getCreatedAt());
        response.setMilkProducer(animal.isMilkProducer());
        response.setEggProducer(animal.isEggProducer());
        return response;
    }
}