package tc.impact.farmmanagement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tc.impact.farmmanagement.dto.request.FlockRequest;
import tc.impact.farmmanagement.dto.response.ApiResponse;
import tc.impact.farmmanagement.dto.response.FlockResponse;
import tc.impact.farmmanagement.entity.Flock;
import tc.impact.farmmanagement.service.FlockService;

import java.util.List;

@RestController
@RequestMapping("/api/flocks")
@RequiredArgsConstructor
public class FlockController {

    private final FlockService flockService;

    @PostMapping
    public ResponseEntity<ApiResponse<FlockResponse>> create(@Valid @RequestBody FlockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Flock created", flockService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FlockResponse>>> getAll(
            @RequestParam(required = false) Long speciesId,
            @RequestParam(required = false) Flock.FlockStatus status) {

        // Both filters
        if (speciesId != null && status != null) {
            return ResponseEntity.ok(ApiResponse.success(
                    flockService.getBySpeciesAndStatus(speciesId, status)));
        }
        // Just species
        if (speciesId != null) {
            return ResponseEntity.ok(ApiResponse.success(
                    flockService.getBySpecies(speciesId)));
        }
        // Just status
        if (status != null) {
            return ResponseEntity.ok(ApiResponse.success(
                    flockService.getByStatus(status)));
        }
        // No filters
        return ResponseEntity.ok(ApiResponse.success(flockService.getAll()));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FlockResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(flockService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FlockResponse>> update(
            @PathVariable Long id, @Valid @RequestBody FlockRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Flock updated", flockService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        flockService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Flock deleted", null));
    }
}