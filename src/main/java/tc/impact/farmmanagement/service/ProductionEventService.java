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

        ProductionEvent event = new ProductionEvent();
        event.setFarm(farm);
        event.setAnimal(animal);
        event.setDate(request.getDate() != null ? request.getDate() : LocalDate.now());
        event.setEventType(request.getEventType());
        event.setTitle(request.getTitle().trim());
        event.setDescription(request.getDescription() != null
                ? request.getDescription().trim()
                : null);
        event.setCost(request.getCost());
        event.setEnteredBy(farmContext.getCurrentUser());

        return toResponse(eventRepository.save(event));
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

        // Animal cannot be changed once created
        if (!event.getAnimal().getId().equals(request.getAnimalId())) {
            throw new tc.impact.farmmanagement.exception.BusinessException(
                    "Animal cannot be changed on an existing event. Delete and recreate.");
        }

        event.setDate(request.getDate() != null ? request.getDate() : event.getDate());
        event.setEventType(request.getEventType());
        event.setTitle(request.getTitle().trim());
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
}