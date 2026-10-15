package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.request.ProductionEventRequest;
import tc.impact.farmmanagement.dto.response.ProductionEventResponse;
import tc.impact.farmmanagement.entity.Animal;
import tc.impact.farmmanagement.entity.Farm;
import tc.impact.farmmanagement.entity.ProductionEvent;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
import tc.impact.farmmanagement.repository.AnimalRepository;
import tc.impact.farmmanagement.repository.ProductionEventRepository;
import tc.impact.farmmanagement.util.UserDisplayUtil;
import tc.impact.farmmanagement.exception.BusinessException;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductionEventService {

    private final ProductionEventRepository eventRepository;
    private final AnimalRepository animalRepository;
    private final FarmContext farmContext;

    // ============ Create ============

    @Transactional
    public ProductionEventResponse create(ProductionEventRequest request) {
        Farm farm = farmContext.getCurrentFarm();
        Animal animal = loadAnimal(request.getAnimalId(), farm.getId());

        validateEventForAnimal(request.getEventType(), animal);
        validateEventHistory(request.getEventType(), animal, farm.getId());

        ProductionEvent event = new ProductionEvent();
        event.setFarm(farm);
        event.setAnimal(animal);
        event.setDate(request.getDate() != null ? request.getDate() : LocalDate.now());
        event.setEventType(request.getEventType());
        String title = request.getTitle() != null && !request.getTitle().isBlank()
                ? request.getTitle().trim()
                : defaultTitleFor(request.getEventType());

        event.setTitle(title);
        event.setDescription(request.getDescription() != null
                ? request.getDescription().trim()
                : null);
        event.setCost(request.getCost());
        event.setEnteredBy(farmContext.getCurrentUser());

        ProductionEvent saved = eventRepository.save(event);

        // Handle lifecycle transitions
        applyLifecycleTransition(request.getEventType(), animal);

        return toResponse(saved);
    }
    // ============ Read ============

    @Transactional(readOnly = true)
    public List<ProductionEventResponse> getByAnimal(Long animalId) {
        Long farmId = farmContext.getCurrentFarmId();
        // Validate animal belongs to farm
        loadAnimal(animalId, farmId);
        return eventRepository.findByFarmIdAndAnimalIdOrderByDateDesc(farmId, animalId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductionEventResponse> getByAnimalAndType(Long animalId, ProductionEvent.EventType type) {
        Long farmId = farmContext.getCurrentFarmId();
        loadAnimal(animalId, farmId);
        return eventRepository.findByFarmIdAndAnimalIdAndEventTypeOrderByDateDesc(farmId, animalId, type)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductionEventResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    // ============ Update ============

    @Transactional
    public ProductionEventResponse update(Long id, ProductionEventRequest request) {
        Long farmId = farmContext.getCurrentFarmId();
        ProductionEvent event = findOrThrow(id);

        if (!event.getAnimal().getId().equals(request.getAnimalId())) {
            throw new BusinessException(
                    "Animal cannot be changed on an existing event. Delete and recreate.");
        }

        Animal animal = event.getAnimal();

        // Re-validate: the event type may have changed
        validateEventForAnimal(request.getEventType(), animal);
        validateEventHistory(request.getEventType(), animal, farmId, id);   // exclude self

        event.setDate(request.getDate() != null ? request.getDate() : event.getDate());
        event.setEventType(request.getEventType());
        String title = request.getTitle() != null && !request.getTitle().isBlank()
                ? request.getTitle().trim()
                : defaultTitleFor(request.getEventType());

        event.setTitle(title);
        event.setDescription(request.getDescription() != null
                ? request.getDescription().trim()
                : null);
        event.setCost(request.getCost());

        return toResponse(eventRepository.save(event));
    }

    // ============ Delete ============

    @Transactional
    public void delete(Long id) {
        ProductionEvent event = findOrThrow(id);
        eventRepository.deleteById(event.getId());
    }

    // ============ Helpers ============

    private Animal loadAnimal(Long animalId, Long farmId) {
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new ResourceNotFoundException("Animal not found with id: " + animalId));
        if (!animal.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Animal not found with id: " + animalId);
        }
        return animal;
    }

    private ProductionEvent findOrThrow(Long id) {
        Long farmId = farmContext.getCurrentFarmId();
        ProductionEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));
        if (!event.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Event not found with id: " + id);
        }
        return event;
    }

    private ProductionEventResponse toResponse(ProductionEvent e) {
        ProductionEventResponse response = new ProductionEventResponse();
        response.setId(e.getId());
        if (e.getAnimal() != null) {
            response.setAnimalId(e.getAnimal().getId());
            response.setAnimalTagNumber(e.getAnimal().getTagNumber());
        }
        response.setDate(e.getDate());
        response.setEventType(e.getEventType());
        response.setTitle(e.getTitle());
        response.setDescription(e.getDescription());
        response.setCost(e.getCost());
        response.setCreatedAt(e.getCreatedAt());
        if (e.getEnteredBy() != null) {
            response.setEnteredByUserId(e.getEnteredBy().getId());
            response.setEnteredByName(UserDisplayUtil.displayName(e.getEnteredBy()));
        }
        return response;
    }
    private String defaultTitleFor(ProductionEvent.EventType type) {
        return switch (type) {
            case ILLNESS -> "Illness";
            case TREATMENT -> "Treatment given";
            case RECOVERY -> "Recovered";
            case VACCINATION -> "Vaccination";
            case DEWORMING -> "Deworming";
            case CHECKUP -> "Checkup";
            case PREGNANCY -> "Pregnancy confirmed";
            case CALVING -> "Gave birth";
            case WEIGHT -> "Weight recorded";
            case PURCHASE -> "Purchased";
            case SALE -> "Sold";
            case DEATH -> "Death";
            case NOTE -> "Note";
            case OTHER -> "Event";
        };
    }

    private void validateEventForAnimal(ProductionEvent.EventType type, Animal animal) {
        switch (type) {
            case PREGNANCY, CALVING -> {
                if (animal.getGender() != Animal.Gender.FEMALE) {
                    throw new BusinessException(
                            "Only female animals can have " + type + " events");
                }
                boolean isMilkSpecies = animal.getSpecies() != null
                        && animal.getSpecies().isMilkProducer();
                if (!isMilkSpecies) {
                    String speciesName = animal.getSpecies() != null
                            ? animal.getSpecies().getName()
                            : "unknown";
                    throw new BusinessException(
                            "Only milk-producing species can have " + type +
                                    " events. Species '" + speciesName + "' cannot.");
                }
            }
            case ILLNESS, TREATMENT, RECOVERY, VACCINATION, DEWORMING, CHECKUP, WEIGHT -> {
                if (animal.getLifecycleStatus() != Animal.LifecycleStatus.ACTIVE) {
                    throw new BusinessException(
                            "Cannot record " + type + " for animal with status " + animal.getLifecycleStatus());
                }
            }
            case SALE -> {
                if (animal.getLifecycleStatus() == Animal.LifecycleStatus.SOLD) {
                    throw new BusinessException(
                            "Animal '" + animal.getTagNumber() + "' is already SOLD");
                }
                if (animal.getLifecycleStatus() == Animal.LifecycleStatus.DEAD) {
                    throw new BusinessException(
                            "Cannot record SALE for a DEAD animal");
                }
            }
            case DEATH -> {
                if (animal.getLifecycleStatus() == Animal.LifecycleStatus.DEAD) {
                    throw new BusinessException(
                            "Animal '" + animal.getTagNumber() + "' is already marked DEAD");
                }
                if (animal.getLifecycleStatus() == Animal.LifecycleStatus.SOLD) {
                    throw new BusinessException(
                            "Cannot record DEATH for a SOLD animal");
                }
            }
            // NOTE, OTHER, PURCHASE have no species/gender restrictions
            default -> { }
        }
    }

    private void validateEventHistory(ProductionEvent.EventType type, Animal animal,
                                      Long farmId, Long... excludeEventId) {
        Long exclude = excludeEventId.length > 0 ? excludeEventId[0] : null;

        switch (type) {
            case PURCHASE -> {
                // Only allow ONE purchase event per animal
                List<ProductionEvent> purchases = eventRepository
                        .findByFarmIdAndAnimalIdAndEventTypeOrderByDateDesc(
                                farmId, animal.getId(), ProductionEvent.EventType.PURCHASE);
                boolean hasOtherPurchase = purchases.stream()
                        .anyMatch(p -> exclude == null || !p.getId().equals(exclude));
                if (hasOtherPurchase) {
                    throw new BusinessException(
                            "Animal '" + animal.getTagNumber() +
                                    "' already has a PURCHASE event recorded");
                }
            }
            default -> { }
        }
    }

    private void applyLifecycleTransition(ProductionEvent.EventType type, Animal animal) {
        boolean changed = false;

        if (type == ProductionEvent.EventType.SALE) {
            animal.setLifecycleStatus(Animal.LifecycleStatus.SOLD);
            changed = true;
        } else if (type == ProductionEvent.EventType.DEATH) {
            animal.setLifecycleStatus(Animal.LifecycleStatus.DEAD);
            changed = true;
        }

        if (changed) {
            animalRepository.save(animal);
        }
    }
}