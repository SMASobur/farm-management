package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.request.MilkProductionRequest;
import tc.impact.farmmanagement.dto.response.DailyMilkSummaryResponse;
import tc.impact.farmmanagement.dto.response.MilkProductionResponse;
import tc.impact.farmmanagement.entity.Animal;
import tc.impact.farmmanagement.entity.Farm;
import tc.impact.farmmanagement.entity.MilkProduction;
import tc.impact.farmmanagement.entity.PoolProductType;
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
import tc.impact.farmmanagement.repository.AnimalRepository;
import tc.impact.farmmanagement.repository.MilkProductionRepository;
import tc.impact.farmmanagement.util.UserDisplayUtil;
import tc.impact.farmmanagement.util.MilkYieldLimits;


import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MilkProductionService {

    private final MilkProductionRepository milkRepository;
    private final AnimalRepository animalRepository;
    private final FarmContext farmContext;

    // ============ Create ============

    @Transactional
    public MilkProductionResponse create(MilkProductionRequest request) {
        Farm farm = farmContext.getCurrentFarm();
        Animal animal = animalRepository.findById(request.getAnimalId())
                .orElseThrow(() -> new ResourceNotFoundException("Animal not found with id: " + request.getAnimalId()));
        if (!animal.getFarm().getId().equals(farm.getId())) {
            throw new ResourceNotFoundException("Animal not found with id: " + request.getAnimalId());
        }
        validateAnimalCanProduceMilk(animal);

        validateMilkYield(request, animal);

        if (milkRepository.existsByFarmIdAndAnimalIdAndDate(farm.getId(), animal.getId(), request.getDate())) {
            throw new BusinessException("Milk entry already exists for " + animal.getTagNumber() + " on " + request.getDate());
        }

        MilkProduction production = new MilkProduction();
        production.setFarm(farm);
        production.setAnimal(animal);
        production.setPoolSpecies(animal.getSpecies());
        production.setPoolProductType(PoolProductType.MILK);
        production.setDate(request.getDate());
        production.setMorningLiters(safe(request.getMorningLiters()));
        production.setNoonLiters(safe(request.getNoonLiters()));
        production.setNightLiters(safe(request.getNightLiters()));
        production.setEnteredBy(farmContext.getCurrentUser());

        return toResponse(milkRepository.save(production));
    }

    // ============ Read ============

    @Transactional(readOnly = true)
    public List<MilkProductionResponse> getAll() {
        return milkRepository.findByFarmId(farmContext.getCurrentFarmId()).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MilkProductionResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<MilkProductionResponse> getByAnimal(Long animalId) {
        return milkRepository.findByFarmIdAndAnimalId(farmContext.getCurrentFarmId(), animalId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MilkProductionResponse> getByDate(LocalDate date) {
        return milkRepository.findByFarmIdAndDate(farmContext.getCurrentFarmId(), date).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MilkProductionResponse> getByRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) throw new BusinessException("'from' date cannot be after 'to' date");
        return milkRepository.findByFarmIdAndDateBetweenOrderByDateAsc(farmContext.getCurrentFarmId(), from, to).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    // ============ Update ============

    @Transactional
    public MilkProductionResponse update(Long id, MilkProductionRequest request) {
        MilkProduction production = findOrThrow(id);
        Long farmId = farmContext.getCurrentFarmId();

        Animal animalToValidate = production.getAnimal();
        if (!production.getAnimal().getId().equals(request.getAnimalId())) {
            animalToValidate = animalRepository.findById(request.getAnimalId())
                    .orElseThrow(() -> new ResourceNotFoundException("Animal not found"));
        }
        validateMilkYield(request, animalToValidate);

        // If changing animal or date, verify duplicates don't exist
        boolean animalChanged = !production.getAnimal().getId().equals(request.getAnimalId());
        boolean dateChanged = !production.getDate().equals(request.getDate());

        if (animalChanged) {
            Animal newAnimal = animalRepository.findById(request.getAnimalId())
                    .orElseThrow(() -> new ResourceNotFoundException("Animal not found with id: " + request.getAnimalId()));
            if (!newAnimal.getFarm().getId().equals(farmId)) {
                throw new ResourceNotFoundException("Animal not found with id: " + request.getAnimalId());
            }
            validateAnimalCanProduceMilk(newAnimal);

            if (milkRepository.existsByFarmIdAndAnimalIdAndDate(farmId, newAnimal.getId(), request.getDate())) {
                throw new BusinessException("Milk entry already exists for " + newAnimal.getTagNumber() + " on " + request.getDate());
            }
            production.setAnimal(newAnimal);
            production.setPoolSpecies(newAnimal.getSpecies());
        } else if (dateChanged) {
            if (milkRepository.existsByFarmIdAndAnimalIdAndDate(farmId, production.getAnimal().getId(), request.getDate())) {
                throw new BusinessException("Milk entry already exists for " + production.getAnimal().getTagNumber() + " on " + request.getDate());
            }
        }

        production.setDate(request.getDate());
        production.setMorningLiters(safe(request.getMorningLiters()));
        production.setNoonLiters(safe(request.getNoonLiters()));
        production.setNightLiters(safe(request.getNightLiters()));

        return toResponse(milkRepository.save(production));
    }

    // ============ Delete ============

    @Transactional
    public void delete(Long id) {
        MilkProduction production = findOrThrow(id);
        milkRepository.deleteById(production.getId());
    }

    // ============ Summaries ============

    @Transactional(readOnly = true)
    public Double getDailyTotal(LocalDate date) {
        return milkRepository.sumLitersByFarmAndDate(farmContext.getCurrentFarmId(), date);
    }

    @Transactional(readOnly = true)
    public List<DailyMilkSummaryResponse> getDailySummaries(LocalDate from, LocalDate to) {
        return milkRepository.dailyTotalsBetween(farmContext.getCurrentFarmId(), from, to).stream()
                .map(row -> new DailyMilkSummaryResponse((LocalDate) row[0], (Double) row[1]))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Double getMonthlyTotal(int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        return milkRepository.sumLitersByFarmBetween(farmContext.getCurrentFarmId(), from, to);
    }

    // ============ Helpers ============

    private MilkProduction findOrThrow(Long id) {
        Long farmId = farmContext.getCurrentFarmId();
        MilkProduction p = milkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Milk entry not found with id: " + id));
        if (!p.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Milk entry not found with id: " + id);
        }
        return p;
    }

    private void validateMilkYield(MilkProductionRequest request, Animal animal) {
        double morning = safe(request.getMorningLiters());
        double noon = safe(request.getNoonLiters());
        double night = safe(request.getNightLiters());
        double totalYield = morning + noon + night;

        if (morning < 0 || noon < 0 || night < 0) {
            throw new BusinessException("Milk amounts cannot be negative");
        }

        double maxLimit = MilkYieldLimits.getMaxDailyYield(animal);

        if (totalYield > maxLimit) {
            throw new BusinessException(String.format(
                    "Unrealistic milk yield: %.1fL. A %s cannot produce more than %.1fL per day.",
                    totalYield, animal.getSpecies().getName(), maxLimit
            ));
        }
        if (totalYield <= 0.0) {
            throw new BusinessException("Total milk yield must be greater than 0");
        }
    }


    private void validateAnimalCanProduceMilk(Animal animal) {
        if (animal.getLifecycleStatus() == Animal.LifecycleStatus.DEAD
                || animal.getLifecycleStatus() == Animal.LifecycleStatus.SOLD
                || animal.getLifecycleStatus() == Animal.LifecycleStatus.CULLED) {
            throw new BusinessException(
                    "Cannot record milk for animal with status " + animal.getLifecycleStatus());
        }
        if (!animal.isMilkProducer()) {
            throw new BusinessException(
                    "Animal '" + animal.getTagNumber() + "' (" +
                            animal.getSpecies().getName() + ") does not produce milk");
        }
    }

    private Double safe(Double value) {
        return value != null ? value : 0.0;
    }

    private MilkProductionResponse toResponse(MilkProduction m) {
        MilkProductionResponse response = new MilkProductionResponse();
        response.setId(m.getId());
        response.setAnimalId(m.getAnimal().getId());
        response.setAnimalTagNumber(m.getAnimal().getTagNumber());
        response.setAnimalName(m.getAnimal().getName());
        response.setDate(m.getDate());
        response.setMorningLiters(m.getMorningLiters());
        response.setNoonLiters(m.getNoonLiters());
        response.setNightLiters(m.getNightLiters());
        response.setTotalLiters(m.getTotalLiters());

        if (m.getPoolSpecies() != null) {
            response.setPoolSpeciesId(m.getPoolSpecies().getId());
            response.setPoolSpeciesCode(m.getPoolSpecies().getCode());
            response.setPoolSpeciesName(m.getPoolSpecies().getName());
        }
        response.setPoolProductType(m.getPoolProductType());

        if (m.getEnteredBy() != null) {
            response.setEnteredByUserId(m.getEnteredBy().getId());
            response.setEnteredByName(UserDisplayUtil.displayName(m.getEnteredBy()));
        }
        return response;
    }
}