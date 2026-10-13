package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tc.impact.farmmanagement.entity.ProductionEvent;
import tc.impact.farmmanagement.repository.MilkProductionRepository;
import tc.impact.farmmanagement.repository.ProductionEventRepository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Derives temporary condition flags for animals from their event history.
 *
 * The flags are:
 *   - currentlyPregnant: latest PREGNANCY event exists, no later CALVING event
 *   - currentlySick:     latest ILLNESS event exists, no later TREATMENT event
 *   - currentlyMilking:  has at least one MilkProduction record in last 7 days
 *
 * Batch approach: computes the flags for ALL animals in a farm with
 * 5 queries total (2 for pregnant, 2 for sick, 1 for milking), independent
 * of animal count. Avoids N+1 problems.
 */
@Service
@RequiredArgsConstructor
public class AnimalDerivationService {

    private static final int MILKING_WINDOW_DAYS = 7;

    private final ProductionEventRepository eventRepository;
    private final MilkProductionRepository milkRepository;

    public static class DerivedFlags {
        public final boolean pregnant;
        public final boolean sick;
        public final boolean milking;

        public DerivedFlags(boolean pregnant, boolean sick, boolean milking) {
            this.pregnant = pregnant;
            this.sick = sick;
            this.milking = milking;
        }

        public static DerivedFlags none() {
            return new DerivedFlags(false, false, false);
        }
    }

    /**
     * Compute derived flags for all animals in a farm.
     * Returns Map<animalId, DerivedFlags>.
     */
    public Map<Long, DerivedFlags> computeFlagsForFarm(Long farmId) {
        // --- Pregnant ---
        Map<Long, LocalDate> lastPregnant = toDateMap(
                eventRepository.findLatestDatesByEventType(farmId, ProductionEvent.EventType.PREGNANCY));
        Map<Long, LocalDate> lastCalving = toDateMap(
                eventRepository.findLatestDatesByEventType(farmId, ProductionEvent.EventType.CALVING));

        Set<Long> pregnantSet = new HashSet<>();
        for (Map.Entry<Long, LocalDate> e : lastPregnant.entrySet()) {
            LocalDate pregnantDate = e.getValue();
            LocalDate calvingDate = lastCalving.get(e.getKey());
            if (calvingDate == null || pregnantDate.isAfter(calvingDate)
                    || pregnantDate.isEqual(calvingDate)) {
                pregnantSet.add(e.getKey());
            }
        }

        // --- Sick ---
        Map<Long, LocalDate> lastIllness = toDateMap(
                eventRepository.findLatestDatesByEventType(farmId, ProductionEvent.EventType.ILLNESS));
        Map<Long, LocalDate> lastTreatment = toDateMap(
                eventRepository.findLatestDatesByEventType(farmId, ProductionEvent.EventType.TREATMENT));

        Set<Long> sickSet = new HashSet<>();
        for (Map.Entry<Long, LocalDate> e : lastIllness.entrySet()) {
            LocalDate illnessDate = e.getValue();
            LocalDate treatmentDate = lastTreatment.get(e.getKey());
            if (treatmentDate == null || illnessDate.isAfter(treatmentDate)
                    || illnessDate.isEqual(treatmentDate)) {
                sickSet.add(e.getKey());
            }
        }

        // --- Milking ---
        LocalDate since = LocalDate.now().minusDays(MILKING_WINDOW_DAYS);
        Set<Long> milkingSet = new HashSet<>(milkRepository.findAnimalIdsWithMilkSince(farmId, since));

        // --- Combine ---
        Set<Long> allAnimalIds = new HashSet<>();
        allAnimalIds.addAll(lastPregnant.keySet());
        allAnimalIds.addAll(lastCalving.keySet());
        allAnimalIds.addAll(lastIllness.keySet());
        allAnimalIds.addAll(lastTreatment.keySet());
        allAnimalIds.addAll(milkingSet);

        Map<Long, DerivedFlags> result = new HashMap<>();
        for (Long animalId : allAnimalIds) {
            result.put(animalId, new DerivedFlags(
                    pregnantSet.contains(animalId),
                    sickSet.contains(animalId),
                    milkingSet.contains(animalId)));
        }
        return result;
    }

    /**
     * Compute derived flags for a single animal.
     * This is used by getAnimalById() when we only need one.
     */
    public DerivedFlags computeFlagsForAnimal(Long farmId, Long animalId) {
        boolean pregnant = computeFlag(
                farmId, animalId, ProductionEvent.EventType.PREGNANCY, ProductionEvent.EventType.CALVING);
        boolean sick = computeFlag(
                farmId, animalId, ProductionEvent.EventType.ILLNESS, ProductionEvent.EventType.TREATMENT);

        LocalDate since = LocalDate.now().minusDays(MILKING_WINDOW_DAYS);
        boolean milking = !milkRepository.findByFarmIdAndAnimalId(farmId, animalId).stream()
                .filter(m -> !m.getDate().isBefore(since))
                .toList().isEmpty();

        return new DerivedFlags(pregnant, sick, milking);
    }

    private boolean computeFlag(Long farmId, Long animalId,
                                ProductionEvent.EventType positiveType,
                                ProductionEvent.EventType negativeType) {
        LocalDate positiveDate = getLatestDate(farmId, animalId, positiveType);
        if (positiveDate == null) return false;

        LocalDate negativeDate = getLatestDate(farmId, animalId, negativeType);
        if (negativeDate == null) return true;

        return positiveDate.isAfter(negativeDate) || positiveDate.isEqual(negativeDate);
    }

    private LocalDate getLatestDate(Long farmId, Long animalId, ProductionEvent.EventType type) {
        List<ProductionEvent> events = eventRepository.findByFarmIdAndAnimalIdAndEventTypeOrderByDateDesc(
                farmId, animalId, type);
        return events.isEmpty() ? null : events.get(0).getDate();
    }

    private Map<Long, LocalDate> toDateMap(List<Object[]> rows) {
        Map<Long, LocalDate> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], (LocalDate) row[1]);
        }
        return map;
    }
}