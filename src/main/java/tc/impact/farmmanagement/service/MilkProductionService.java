package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.request.MilkProductionRequest;
import tc.impact.farmmanagement.dto.response.DailyMilkSummaryResponse;
import tc.impact.farmmanagement.dto.response.MilkProductionResponse;
import tc.impact.farmmanagement.entity.Animal;
import tc.impact.farmmanagement.entity.MilkProduction;
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
import tc.impact.farmmanagement.repository.AnimalRepository;
import tc.impact.farmmanagement.repository.MilkProductionRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MilkProductionService {

    private final MilkProductionRepository milkRepository;
    private final AnimalRepository animalRepository;

    // ============ Create ============

    @Transactional
    public MilkProductionResponse create(MilkProductionRequest request) {
        Animal animal = animalRepository.findById(request.getAnimalId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Animal not found with id: " + request.getAnimalId()));

        validateAnimalCanProduceMilk(animal);

        if (milkRepository.existsByAnimalIdAndDate(animal.getId(), request.getDate())) {
            throw new BusinessException(
                    "Milk entry already exists for " + animal.getTagNumber() +
                            " on " + request.getDate());
        }

        MilkProduction production = new MilkProduction();
        production.setAnimal(animal);
        production.setDate(request.getDate());
        production.setMorningLiters(safe(request.getMorningLiters()));
        production.setNoonLiters(safe(request.getNoonLiters()));
        production.setNightLiters(safe(request.getNightLiters()));

        return toResponse(milkRepository.save(production));
    }

    // ============ Read ============

    public List<MilkProductionResponse> getAll() {
        return milkRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public MilkProductionResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public List<MilkProductionResponse> getByAnimal(Long animalId) {
        if (!animalRepository.existsById(animalId)) {
            throw new ResourceNotFoundException("Animal not found with id: " + animalId);
        }
        return milkRepository.findByAnimalId(animalId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<MilkProductionResponse> getByDate(LocalDate date) {
        return milkRepository.findByDate(date).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<MilkProductionResponse> getByRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException("'from' date cannot be after 'to' date");
        }
        return milkRepository.findByDateBetweenOrderByDateAsc(from, to).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ============ Update ============

    @Transactional
    public MilkProductionResponse update(Long id, MilkProductionRequest request) {
        MilkProduction production = findOrThrow(id);

        // If the animal is changing, validate the new animal + uniqueness
        if (!production.getAnimal().getId().equals(request.getAnimalId())) {
            Animal newAnimal = animalRepository.findById(request.getAnimalId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Animal not found with id: " + request.getAnimalId()));
            validateAnimalCanProduceMilk(newAnimal);

            if (milkRepository.existsByAnimalIdAndDate(newAnimal.getId(), request.getDate())) {
                throw new BusinessException(
                        "Milk entry already exists for " + newAnimal.getTagNumber() +
                                " on " + request.getDate());
            }
            production.setAnimal(newAnimal);
        } else if (!production.getDate().equals(request.getDate())
                && milkRepository.existsByAnimalIdAndDate(production.getAnimal().getId(), request.getDate())) {
            throw new BusinessException(
                    "Milk entry already exists for " + production.getAnimal().getTagNumber() +
                            " on " + request.getDate());
        }

        production.setDate(request.getDate());
        production.setMorningLiters(safe(request.getMorningLiters()));
        production.setNoonLiters(safe(request.getNoonLiters()));
        production.setNightLiters(safe(request.getNightLiters()));

        // @PreUpdate will recalculate totalLiters
        return toResponse(milkRepository.save(production));
    }

    // ============ Delete ============

    @Transactional
    public void delete(Long id) {
        if (!milkRepository.existsById(id)) {
            throw new ResourceNotFoundException("Milk entry not found with id: " + id);
        }
        milkRepository.deleteById(id);
    }

    // ============ Summaries ============

    public Double getDailyTotal(LocalDate date) {
        return milkRepository.sumLitersByDate(date);
    }

    public List<DailyMilkSummaryResponse> getDailySummaries(LocalDate from, LocalDate to) {
        return milkRepository.dailyTotalsBetween(from, to).stream()
                .map(row -> new DailyMilkSummaryResponse((LocalDate) row[0], (Double) row[1]))
                .collect(Collectors.toList());
    }

    public Double getMonthlyTotal(int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        return milkRepository.sumLitersBetween(from, to);
    }

    // ============ Helpers ============

    private MilkProduction findOrThrow(Long id) {
        return milkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Milk entry not found with id: " + id));
    }

    private void validateAnimalCanProduceMilk(Animal animal) {
        if (!animal.isMilkProducer()) {
            throw new BusinessException(
                    "Animal '" + animal.getTagNumber() + "' (" + animal.getSpecies() +
                            ") does not produce milk");
        }
        if (animal.getStatus() == Animal.AnimalStatus.DEAD
                || animal.getStatus() == Animal.AnimalStatus.SOLD) {
            throw new BusinessException(
                    "Cannot record milk for animal '" + animal.getTagNumber() +
                            "' with status " + animal.getStatus());
        }
        if (animal.getStatus() == Animal.AnimalStatus.DRY) {
            throw new BusinessException(
                    "Animal '" + animal.getTagNumber() + "' is marked DRY (not producing milk)");
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
        return response;
    }
}