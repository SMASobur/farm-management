package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.request.EggProductionRequest;
import tc.impact.farmmanagement.dto.response.DailyEggSummaryResponse;
import tc.impact.farmmanagement.dto.response.EggProductionResponse;
import tc.impact.farmmanagement.entity.Animal;
import tc.impact.farmmanagement.entity.EggProduction;
import tc.impact.farmmanagement.entity.Farm;
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
    private final FarmContext farmContext;

    @Transactional
    public EggProductionResponse create(EggProductionRequest request) {
        Farm farm = farmContext.getCurrentFarm();
        validateSource(request);

        EggProduction production = new EggProduction();
        production.setFarm(farm);
        production.setDate(request.getDate());
        production.setGoodEggs(safe(request.getGoodEggs()));
        production.setCrackedEggs(safe(request.getCrackedEggs()));

        if (request.getAnimalId() != null) {
            Animal animal = animalRepository.findById(request.getAnimalId())
                    .orElseThrow(() -> new ResourceNotFoundException("Animal not found with id: " + request.getAnimalId()));
            if (!animal.getFarm().getId().equals(farm.getId())) {
                throw new ResourceNotFoundException("Animal not found with id: " + request.getAnimalId());
            }
            validateAnimalLaysEggs(animal);
            if (eggRepository.existsByFarmIdAndAnimalIdAndDate(farm.getId(), animal.getId(), request.getDate())) {
                throw new BusinessException("Egg entry already exists for animal '" + animal.getTagNumber() + "' on " + request.getDate());
            }
            production.setAnimal(animal);
        } else {
            Flock flock = flockRepository.findById(request.getFlockId())
                    .orElseThrow(() -> new ResourceNotFoundException("Flock not found with id: " + request.getFlockId()));
            if (!flock.getFarm().getId().equals(farm.getId())) {
                throw new ResourceNotFoundException("Flock not found with id: " + request.getFlockId());
            }
            validateFlockLaysEggs(flock);
            if (eggRepository.existsByFarmIdAndFlockIdAndDate(farm.getId(), flock.getId(), request.getDate())) {
                throw new BusinessException("Egg entry already exists for flock '" + flock.getFlockCode() + "' on " + request.getDate());
            }
            production.setFlock(flock);
        }

        return toResponse(eggRepository.save(production));
    }

    @Transactional(readOnly = true)
    public List<EggProductionResponse> getAll() {
        return eggRepository.findByFarmId(farmContext.getCurrentFarmId()).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EggProductionResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<EggProductionResponse> getByAnimal(Long animalId) {
        return eggRepository.findByFarmIdAndAnimalId(farmContext.getCurrentFarmId(), animalId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EggProductionResponse> getByFlock(Long flockId) {
        return eggRepository.findByFarmIdAndFlockId(farmContext.getCurrentFarmId(), flockId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EggProductionResponse> getByDate(LocalDate date) {
        return eggRepository.findByFarmIdAndDate(farmContext.getCurrentFarmId(), date).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EggProductionResponse> getByRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) throw new BusinessException("'from' date cannot be after 'to' date");
        return eggRepository.findByFarmIdAndDateBetweenOrderByDateAsc(farmContext.getCurrentFarmId(), from, to).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public EggProductionResponse update(Long id, EggProductionRequest request) {
        EggProduction production = findOrThrow(id);
        Long farmId = farmContext.getCurrentFarmId();
        validateSource(request);

        if (request.getAnimalId() != null) {
            if (production.getAnimal() == null || !production.getAnimal().getId().equals(request.getAnimalId())) {
                throw new BusinessException("Source cannot be changed. Delete and recreate.");
            }
        } else {
            if (production.getFlock() == null || !production.getFlock().getId().equals(request.getFlockId())) {
                throw new BusinessException("Source cannot be changed. Delete and recreate.");
            }
        }

        if (!production.getDate().equals(request.getDate())) {
            if (production.getAnimal() != null
                    && eggRepository.existsByFarmIdAndAnimalIdAndDate(farmId, production.getAnimal().getId(), request.getDate())) {
                throw new BusinessException("Egg entry already exists on " + request.getDate());
            }
            if (production.getFlock() != null
                    && eggRepository.existsByFarmIdAndFlockIdAndDate(farmId, production.getFlock().getId(), request.getDate())) {
                throw new BusinessException("Egg entry already exists on " + request.getDate());
            }
        }

        production.setDate(request.getDate());
        production.setGoodEggs(safe(request.getGoodEggs()));
        production.setCrackedEggs(safe(request.getCrackedEggs()));

        return toResponse(eggRepository.save(production));
    }

    @Transactional
    public void delete(Long id) {
        EggProduction p = findOrThrow(id);
        eggRepository.deleteById(p.getId());
    }

    @Transactional(readOnly = true)
    public Long getDailyTotal(LocalDate date) {
        return eggRepository.sumEggsByFarmAndDate(farmContext.getCurrentFarmId(), date);
    }

    @Transactional(readOnly = true)
    public List<DailyEggSummaryResponse> getDailySummaries(LocalDate from, LocalDate to) {
        return eggRepository.dailyTotalsBetween(farmContext.getCurrentFarmId(), from, to).stream()
                .map(row -> new DailyEggSummaryResponse((LocalDate) row[0], ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Long getMonthlyTotal(int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        return eggRepository.sumEggsByFarmBetween(farmContext.getCurrentFarmId(), from, to);
    }

    private EggProduction findOrThrow(Long id) {
        Long farmId = farmContext.getCurrentFarmId();
        EggProduction p = eggRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Egg entry not found with id: " + id));
        if (!p.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Egg entry not found with id: " + id);
        }
        return p;
    }

    private void validateSource(EggProductionRequest request) {
        boolean hasAnimal = request.getAnimalId() != null;
        boolean hasFlock = request.getFlockId() != null;
        if (!hasAnimal && !hasFlock) throw new BusinessException("Either animalId or flockId must be provided");
        if (hasAnimal && hasFlock) throw new BusinessException("Only one of animalId or flockId can be provided");
    }

    private void validateAnimalLaysEggs(Animal animal) {
        String speciesCode = animal.getSpecies() != null ? animal.getSpecies().getCode() : null;
        boolean isBird = "CHICKEN".equals(speciesCode) || "DUCK".equals(speciesCode) || "GOOSE".equals(speciesCode);
        if (!isBird) {
            throw new BusinessException("Animal '" + animal.getTagNumber() + "' (" + speciesCode + ") does not lay eggs");
        }
        if (animal.getStatus() == Animal.AnimalStatus.DEAD || animal.getStatus() == Animal.AnimalStatus.SOLD) {
            throw new BusinessException("Cannot record eggs for animal with status " + animal.getStatus());
        }
    }

    private void validateFlockLaysEggs(Flock flock) {
        if (flock.getStatus() == Flock.FlockStatus.RETIRED || flock.getStatus() == Flock.FlockStatus.SOLD) {
            throw new BusinessException("Cannot record eggs for flock with status " + flock.getStatus());
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
        if (e.getAnimal() != null) response.setAnimalId(e.getAnimal().getId());
        if (e.getFlock() != null) response.setFlockId(e.getFlock().getId());
        return response;
    }
}