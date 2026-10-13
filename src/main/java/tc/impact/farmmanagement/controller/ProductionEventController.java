package tc.impact.farmmanagement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tc.impact.farmmanagement.dto.request.ProductionEventRequest;
import tc.impact.farmmanagement.dto.response.ApiResponse;
import tc.impact.farmmanagement.dto.response.ProductionEventResponse;
import tc.impact.farmmanagement.entity.ProductionEvent;
import tc.impact.farmmanagement.service.ProductionEventService;

import java.util.List;

@RestController
@RequestMapping("/api/animal-events")
@RequiredArgsConstructor
public class ProductionEventController {

    private final ProductionEventService eventService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductionEventResponse>> create(
            @Valid @RequestBody ProductionEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Event recorded", eventService.create(request)));
    }

    @GetMapping("/animal/{animalId}")
    public ResponseEntity<ApiResponse<List<ProductionEventResponse>>> getByAnimal(
            @PathVariable Long animalId,
            @RequestParam(required = false) ProductionEvent.EventType eventType) {

        if (eventType != null) {
            return ResponseEntity.ok(ApiResponse.success(
                    eventService.getByAnimalAndType(animalId, eventType)));
        }
        return ResponseEntity.ok(ApiResponse.success(eventService.getByAnimal(animalId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductionEventResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(eventService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductionEventResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductionEventRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Event updated", eventService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Event deleted", null));
    }
}