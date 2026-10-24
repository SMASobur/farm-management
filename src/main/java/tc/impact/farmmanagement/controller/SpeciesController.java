package tc.impact.farmmanagement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tc.impact.farmmanagement.dto.request.SpeciesRequest;
import tc.impact.farmmanagement.dto.response.ApiResponse;
import tc.impact.farmmanagement.dto.response.SpeciesResponse;
import tc.impact.farmmanagement.service.SpeciesService;

import java.util.List;

@RestController
@RequestMapping("/api/species")
@RequiredArgsConstructor
public class SpeciesController {

    private final SpeciesService speciesService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SpeciesResponse>>> getAll(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.success(speciesService.getAll(activeOnly)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SpeciesResponse>> create(@Valid @RequestBody SpeciesRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Species created", speciesService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SpeciesResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody SpeciesRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Species updated", speciesService.update(id, request)));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<ApiResponse<SpeciesResponse>> toggleActive(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(ApiResponse.success("Status updated", speciesService.toggleActive(id, active)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        speciesService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Species deleted", null));
    }
}