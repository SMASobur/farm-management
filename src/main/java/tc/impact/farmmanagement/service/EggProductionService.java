package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.request.EggProductionRequest;
import tc.impact.farmmanagement.dto.response.DailyEggSummaryResponse;
import tc.impact.farmmanagement.dto.response.EggProductionResponse;
import tc.impact.farmmanagement.entity.Animal;
import tc.impact.farmmanagement.entity.EggProduction;
import tc.impact.farmmanagement.entity.Flock;
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
import tc.impact.farmmanagement.repository.AnimalRepository;
import tc.impact.farmmanagement.repository.EggProductionRepository;
import tc.impact.farmmanagement.repository.FlockRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EggProductionService {

    private final EggProductionRepository eggRepository;
    private final AnimalRepository animalRepository;
    private final FlockRepository flockRepository;

    // ============ Create ============

    @Transactional
    public EggProductionResponse create(EggProductionRequest request) {
        validateSource(request);

        EggProduction production = new EggProduction();
        production.setDate(request.getDate());
        production.setGoodEggs(safe(request.getGoodEggs()));
        production.setCrackedEggs(safe(request.getCrackedEggs()));

        if (request.getAnimalId() != null) {
            Animal animal = animalRepository.findById(request.getAnimalId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Animal not found with id: " + request.getAnimalId()));
            validateAnimalLaysEggs(animal);

            if (eggRepository.existsByAnimalIdAndDate(animal.getId(), request.getDate())) {
                throw new BusinessException("Egg entry already exists for animal '" +
                        animal.getTagNumber() + "' on " + request.getDate());
            }
            production.setAnimal(animal);
        } else {
            Flock flock = flockRepository.findById(request.getFlockId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Flock not found with id: " + request.getFlockId()));
            validateFlockLaysEggs(flock);

            if (eggRepository.existsByFlockIdAndDate(flock.getId(), request.getDate())) {
                throw new BusinessException("Egg entry already exists for flock '" +
                        flock.getFlockCode() + "' on " + request.getDate());
            }
            production.setFlock(flock);
        }

        return toResponse(eggRepository.save(production));
    }

    // ============ Read ============

    public List<EggProductionResponse> getAll() {
        return eggRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public EggProductionResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public List<EggProductionResponse> getByAnimal(Long animalId) {
        return eggRepository.findByAnimalId(animalId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<EggProductionResponse> getByFlock(Long flockId) {
        return eggRepository.findByFlockId(flockId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<EggProductionResponse> getByDate(LocalDate date) {
        return eggRepository.findByDate(date).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<EggProductionResponse> getByRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException("'from' date cannot be after 'to' date");
        }
        return eggRepository.findByDateBetweenOrderByDateAsc(from, to).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    // ============ Update ============

    @Transactional
    public EggProductionResponse update(Long id, EggProductionRequest request) {
        EggProduction production = findOrThrow(id);
        validateSource(request);

        // Source (animal/flock) cannot be changed on update — simpler & safer.
        // If you need to change source, delete and recreate.
        if (request.getAnimalId() != null) {
            if (production.getAnimal() == null || !production.getAnimal().getId().equals(request.getAnimalId())) {
                throw new BusinessException("Source cannot be changed on update. Delete and recreate if needed.");
            }
        } else {
            if (production.getFlock() == null || !production.getFlock().getId().equals(request.getFlockId())) {
                throw new BusinessException("Source cannot be changed on update. Delete and recreate if needed.");
            }
        }

        // Date change — check uniqueness under the new date
        if (!production.getDate().equals(request.getDate())) {
            if (production.getAnimal() != null &&
                    eggRepository.existsByAnimalIdAndDate(production.getAnimal().getId(), request.getDate())) {
                throw new BusinessException("Egg entry already exists for this animal on " + request.getDate());
            }
            if (production.getFlock() != null &&
                    eggRepository.existsByFlockIdAndDate(production.getFlock().getId(), request.getDate())) {
                throw new BusinessException("Egg entry already exists for this flock on " + request.getDate());
            }
        }

        production.setDate(request.getDate());
        production.setGoodEggs(safe(request.getGoodEggs()));
        production.setCrackedEggs(safe(request.getCrackedEggs()));

        return toResponse(eggRepository.save(production));
    }

    // ============ Delete ============

    @Transactional
    public void delete(Long id) {
        if (!eggRepository.existsById(id)) {
            throw new ResourceNotFoundException("Egg entry not found with id: " + id);
        }
        eggRepository.deleteById(id);
    }

    // ============ Summaries ============

    public Long getDailyTotal(LocalDate date) {
        return eggRepository.sumEggsByDate(date);
    }

    public List<DailyEggSummaryResponse> getDailySummaries(LocalDate from, LocalDate to) {
        return eggRepository.dailyTotalsBetween(from, to).stream()
                .map(row -> new DailyEggSummaryResponse((LocalDate) row[0], ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    public Long getMonthlyTotal(int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        return eggRepository.sumEggsBetween(from, to);
    }

    // ============ Helpers ============

    private EggProduction findOrThrow(Long id) {
        return eggRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Egg entry not found with id: " + id));
    }

    private void validateSource(EggProductionRequest request) {
        boolean hasAnimal = request.getAnimalId() != null;
        boolean hasFlock  = request.getFlockId()  != null;

        if (!hasAnimal && !hasFlock) {
            throw new BusinessException("Either animalId or flockId must be provided");
        }
        if (hasAnimal && hasFlock) {
            throw new BusinessException("Only one of animalId or flockId can be provided, not both");
        }
    }

    private void validateAnimalLaysEggs(Animal animal) {
        boolean isBird = animal.getSpecies() == Animal.Species.CHICKEN
                || animal.getSpecies() == Animal.Species.DUCK;

        if (!isBird) {
            throw new BusinessException("Animal '" + animal.getTagNumber() + "' (" +
                    animal.getSpecies() + ") does not lay eggs");
        }
        if (animal.getStatus() == Animal.AnimalStatus.DEAD
                || animal.getStatus() == Animal.AnimalStatus.SOLD) {
            throw new BusinessException("Cannot record eggs for animal '" +
                    animal.getTagNumber() + "' with status " + animal.getStatus());
        }
    }

    private void validateFlockLaysEggs(Flock flock) {
        if (flock.getStatus() == Flock.FlockStatus.RETIRED
                || flock.getStatus() == Flock.FlockStatus.SOLD) {
            throw new BusinessException("Cannot record eggs for flock '" +
                    flock.getFlockCode() + "' with status " + flock.getStatus());
        }
    }

    private Integer safe(Integer value) {
        return value != null ? value : 0;
    }

    private EggProductionResponse toResponse(EggProduction e) {
        EggProductionResponse response = new EggProductionResponse();
        response.setId(e.getId());
        response.setDate(e.getDate());
        response.setGoodEggs(e.getGoodEggs());
        response.setCrackedEggs(e.getCrackedEggs());
        response.setTotalEggs(e.getTotalEggs());
        response.setSourceType(e.getSourceType());
        response.setSourceDisplayName(e.getSourceDisplayName());

        if (e.getAnimal() != null) {
            response.setAnimalId(e.getAnimal().getId());
        }
        if (e.getFlock() != null) {
            response.setFlockId(e.getFlock().getId());
        }
        return response;
    }
}