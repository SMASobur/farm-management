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
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
import tc.impact.farmmanagement.repository.AnimalRepository;
import tc.impact.farmmanagement.repository.MilkProductionRepository;
import tc.impact.farmmanagement.util.UserDisplayUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MilkProductionService {

    private final MilkProductionRepository milkRepository;
    private final AnimalRepository animalRepository;
    private final FarmContext farmContext;

    @Transactional
    public MilkProductionResponse create(MilkProductionRequest request) {
        Farm farm = farmContext.getCurrentFarm();
        Animal animal = animalRepository.findById(request.getAnimalId())
                .orElseThrow(() -> new ResourceNotFoundException("Animal not found with id: " + request.getAnimalId()));
        if (!animal.getFarm().getId().equals(farm.getId())) {
            throw new ResourceNotFoundException("Animal not found with id: " + request.getAnimalId());
        }
        validateAnimalCanProduceMilk(animal);

        if (milkRepository.existsByFarmIdAndAnimalIdAndDate(farm.getId(), animal.getId(), request.getDate())) {
            throw new BusinessException("Milk entry already exists for " + animal.getTagNumber() + " on " + request.getDate());
        }

        MilkProduction production = new MilkProduction();
        production.setFarm(farm);
        production.setAnimal(animal);
        production.setDate(request.getDate());
        production.setMorningLiters(safe(request.getMorningLiters()));
        production.setNoonLiters(safe(request.getNoonLiters()));
        production.setNightLiters(safe(request.getNightLiters()));
        production.setEnteredBy(farmContext.getCurrentUser());

        production.setLitersInternal(safe(request.getLitersInternal()));
        production.setLitersFed(safe(request.getLitersFed()));
        production.setLitersWasted(safe(request.getLitersWasted()));

        // Validate usage doesn't exceed total
        double total = safe(request.getMorningLiters())
                + safe(request.getNoonLiters())
                + safe(request.getNightLiters());

        double usage = safe(request.getLitersInternal())
                + safe(request.getLitersFed())
                + safe(request.getLitersWasted());

        if (usage > total) {
            throw new BusinessException(
                    "Internal + fed + wasted (" + usage + "L) exceeds total produced ("
                            + total + "L)");
        }

        return toResponse(milkRepository.save(production));
    }

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

    @Transactional
    public MilkProductionResponse update(Long id, MilkProductionRequest request) {
        MilkProduction production = findOrThrow(id);
        Long farmId = farmContext.getCurrentFarmId();

        if (!production.getAnimal().getId().equals(request.getAnimalId())) {
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
        } else if (!production.getDate().equals(request.getDate())
                && milkRepository.existsByFarmIdAndAnimalIdAndDate(farmId, production.getAnimal().getId(), request.getDate())) {
            throw new BusinessException("Milk entry already exists for " + production.getAnimal().getTagNumber() + " on " + request.getDate());
        }

        production.setDate(request.getDate());
        production.setMorningLiters(safe(request.getMorningLiters()));
        production.setNoonLiters(safe(request.getNoonLiters()));
        production.setNightLiters(safe(request.getNightLiters()));

        production.setLitersInternal(safe(request.getLitersInternal()));
        production.setLitersFed(safe(request.getLitersFed()));
        production.setLitersWasted(safe(request.getLitersWasted()));

        // Validate usage doesn't exceed total
        double total = safe(request.getMorningLiters())
                + safe(request.getNoonLiters())
                + safe(request.getNightLiters());
        double usage = safe(request.getLitersInternal())
                + safe(request.getLitersFed())
                + safe(request.getLitersWasted());

        if (usage > total) {
            throw new BusinessException(
                    "Internal + fed + wasted (" + usage + "L) exceeds total produced ("
                            + total + "L)");
        }

        // Also validate that reducing total doesn't invalidate already-sold amount
        if (production.getLitersSold() > 0
                && production.getLitersSold() > production.getAvailableForSale() + production.getLitersSold()) {
            throw new BusinessException(
                    "Cannot reduce total below already-sold amount (" + production.getLitersSold() + "L)");
        }

        return toResponse(milkRepository.save(production));
    }

    @Transactional
    public void delete(Long id) {
        MilkProduction production = findOrThrow(id);
        milkRepository.deleteById(production.getId());
    }

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

    @Transactional
    public void incrementLitersSold(Long productionId, Double amount) {
        MilkProduction p = milkRepository.findById(productionId)
                .orElseThrow(() -> new ResourceNotFoundException("Milk production not found"));

        double available = p.getAvailableForSale();

        if (amount > available) {
            throw new BusinessException(
                    "Cannot sell " + amount + "L — only " + available + "L available "
                            + "(produced " + p.getTotalLiters() + "L, sold " + p.getLitersSold()
                            + "L, internal " + p.getLitersInternal()
                            + "L, fed " + p.getLitersFed()
                            + "L, wasted " + p.getLitersWasted() + "L)");
        }

        double newSold = (p.getLitersSold() != null ? p.getLitersSold() : 0.0) + amount;
        p.setLitersSold(newSold);
        milkRepository.save(p);
    }

    @Transactional
    public void decrementLitersSold(Long productionId, Double amount) {
        MilkProduction p = milkRepository.findById(productionId)
                .orElseThrow(() -> new ResourceNotFoundException("Milk production not found"));
        double currentSold = p.getLitersSold() != null ? p.getLitersSold() : 0.0;
        double newSold = Math.max(0.0, currentSold - amount);
        p.setLitersSold(newSold);
        milkRepository.save(p);
    }

    private MilkProduction findOrThrow(Long id) {
        Long farmId = farmContext.getCurrentFarmId();
        MilkProduction p = milkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Milk entry not found with id: " + id));
        if (!p.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Milk entry not found with id: " + id);
        }
        return p;
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
        response.setLitersSold(m.getLitersSold());
        response.setLitersInternal(m.getLitersInternal());
        response.setLitersFed(m.getLitersFed());
        response.setLitersWasted(m.getLitersWasted());
        response.setAvailableForSale(m.getAvailableForSale());

        if (m.getEnteredBy() != null) {
            response.setEnteredByUserId(m.getEnteredBy().getId());
            response.setEnteredByName(UserDisplayUtil.displayName(m.getEnteredBy()));
        }
        return response;
    }
}