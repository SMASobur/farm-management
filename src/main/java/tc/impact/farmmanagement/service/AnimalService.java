package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.request.AnimalFilter;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final SpeciesRepository speciesRepository;
    private final AnimalDerivationService derivationService;
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
        animal.setLifecycleStatus(request.getLifecycleStatus() != null
                ? request.getLifecycleStatus()
                : Animal.LifecycleStatus.ACTIVE);
        animal.setGender(request.getGender());
        animal.setDateOfBirth(request.getDateOfBirth());

        // Newly created animal — no events yet, so flags are all false
        return toResponse(animalRepository.save(animal),
                AnimalDerivationService.DerivedFlags.none());
    }

    // ============ Read ============

    @Transactional(readOnly = true)
    public List<AnimalResponse> getFiltered(AnimalFilter filter) {
        Long farmId = farmContext.getCurrentFarmId();

        // 1. Load candidate animals (SQL filters: species, lifecycle)
        List<Animal> animals;
        if (filter.getSpeciesId() != null && filter.getStatus() != null) {
            animals = animalRepository.findByFarmIdAndSpeciesIdAndLifecycleStatus(
                    farmId, filter.getSpeciesId(), filter.getStatus());
        } else if (filter.getSpeciesId() != null) {
            animals = animalRepository.findByFarmIdAndSpeciesId(farmId, filter.getSpeciesId());
        } else if (filter.getStatus() != null) {
            animals = animalRepository.findByFarmIdAndLifecycleStatus(farmId, filter.getStatus());
        } else {
            animals = animalRepository.findByFarmId(farmId);
        }

        // 2. Compute derived flags for all candidates (batch)
        Map<Long, AnimalDerivationService.DerivedFlags> flags =
                derivationService.computeFlagsForFarm(farmId);

        // 3. Map to responses
        List<AnimalResponse> responses = animals.stream()
                .map(a -> toResponse(a, flags.getOrDefault(a.getId(),
                        AnimalDerivationService.DerivedFlags.none())))
                .collect(Collectors.toList());

        // 4. Apply derived-condition filter (in memory)
        if (filter.getCondition() != null) {
            responses = responses.stream()
                    .filter(r -> matchesCondition(r, filter.getCondition()))
                    .collect(Collectors.toList());
        }

        // 5. Apply search filter (in memory)
        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            String q = filter.getSearch().trim().toLowerCase();
            responses = responses.stream()
                    .filter(r -> (r.getTagNumber() != null && r.getTagNumber().toLowerCase().contains(q))
                            || (r.getName() != null && r.getName().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }

        return responses;
    }

    private boolean matchesCondition(AnimalResponse r, AnimalFilter.AnimalCondition condition) {
        return switch (condition) {
            case PREGNANT -> r.isCurrentlyPregnant();
            case SICK     -> r.isCurrentlySick();
            case MILKING  -> r.isCurrentlyMilking();
        };
    }

    @Transactional(readOnly = true)
    public List<AnimalResponse> getAllAnimals() {
        Long farmId = farmContext.getCurrentFarmId();
        List<Animal> animals = animalRepository.findByFarmId(farmId);

        Map<Long, AnimalDerivationService.DerivedFlags> flags =
                derivationService.computeFlagsForFarm(farmId);

        return animals.stream()
                .map(a -> toResponse(a, flags.getOrDefault(a.getId(),
                        AnimalDerivationService.DerivedFlags.none())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnimalResponse> getAnimalsBySpecies(Long speciesId) {
        Long farmId = farmContext.getCurrentFarmId();
        List<Animal> animals = animalRepository.findByFarmIdAndSpeciesId(farmId, speciesId);

        Map<Long, AnimalDerivationService.DerivedFlags> flags =
                derivationService.computeFlagsForFarm(farmId);

        return animals.stream()
                .map(a -> toResponse(a, flags.getOrDefault(a.getId(),
                        AnimalDerivationService.DerivedFlags.none())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnimalResponse> getAnimalsByStatus(Animal.LifecycleStatus status) {
        Long farmId = farmContext.getCurrentFarmId();
        List<Animal> animals = animalRepository.findByFarmIdAndLifecycleStatus(farmId, status);
        Map<Long, AnimalDerivationService.DerivedFlags> flags =
                derivationService.computeFlagsForFarm(farmId);
        return animals.stream()
                .map(a -> toResponse(a, flags.getOrDefault(a.getId(),
                        AnimalDerivationService.DerivedFlags.none())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnimalResponse> getAnimalsBySpeciesAndStatus(
            Long speciesId, Animal.LifecycleStatus status) {
        Long farmId = farmContext.getCurrentFarmId();
        List<Animal> animals = animalRepository
                .findByFarmIdAndSpeciesIdAndLifecycleStatus(farmId, speciesId, status);

        Map<Long, AnimalDerivationService.DerivedFlags> flags =
                derivationService.computeFlagsForFarm(farmId);

        return animals.stream()
                .map(a -> toResponse(a, flags.getOrDefault(a.getId(),
                        AnimalDerivationService.DerivedFlags.none())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AnimalResponse getAnimalById(Long id) {
        Long farmId = farmContext.getCurrentFarmId();
        Animal animal = findOrThrow(id);
        AnimalDerivationService.DerivedFlags flags =
                derivationService.computeFlagsForAnimal(farmId, animal.getId());
        return toResponse(animal, flags);
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
        if (request.getLifecycleStatus() != null) {
            animal.setLifecycleStatus(request.getLifecycleStatus());
        }
        animal.setGender(request.getGender());
        animal.setDateOfBirth(request.getDateOfBirth());

        Animal saved = animalRepository.save(animal);

        // Compute flags after save so response reflects current state
        AnimalDerivationService.DerivedFlags flags =
                derivationService.computeFlagsForAnimal(farmId, saved.getId());

        return toResponse(saved, flags);
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
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Species not found with id: " + request.getSpeciesId()));
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
        sp.setNameBn(request.getCustomSpeciesNameBn());
        sp.setMilkProducer(Boolean.TRUE.equals(request.getCustomSpeciesMilkProducer()));
        sp.setEggProducer(Boolean.TRUE.equals(request.getCustomSpeciesEggProducer()));
        sp.setActive(saveForReuse);
        sp.setSystemDefault(false);
        sp.setDisplayOrder(900);
        sp.setMaxDailyYield(request.getCustomMaxDailyYield());

        return speciesRepository.save(sp);
    }

    private String normalizeCode(String name) {
        return name.trim()
                .toUpperCase()
                .replaceAll("[^\\p{L}\\p{N}]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private AnimalResponse toResponse(Animal animal, AnimalDerivationService.DerivedFlags flags) {
        AnimalResponse response = new AnimalResponse();
        response.setId(animal.getId());
        response.setTagNumber(animal.getTagNumber());
        response.setName(animal.getName());
        response.setSpeciesId(animal.getSpecies().getId());
        response.setSpeciesCode(animal.getSpecies().getCode());
        response.setSpeciesName(animal.getSpecies().getName());
        response.setSpeciesNameBn(animal.getSpecies().getNameBn());
        response.setLifecycleStatus(animal.getLifecycleStatus());
        response.setGender(animal.getGender());
        response.setDateOfBirth(animal.getDateOfBirth());
        response.setCreatedAt(animal.getCreatedAt());
        response.setMilkProducer(animal.isMilkProducer());
        response.setEggProducer(animal.isEggProducer());

        response.setCurrentlyPregnant(flags.pregnant);
        response.setCurrentlySick(flags.sick);
        response.setCurrentlyMilking(flags.milking);

        return response;
    }
}