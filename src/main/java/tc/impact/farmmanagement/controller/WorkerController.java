package tc.impact.farmmanagement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tc.impact.farmmanagement.dto.request.WorkerRequest;
import tc.impact.farmmanagement.dto.response.ApiResponse;
import tc.impact.farmmanagement.dto.response.WorkerResponse;
import tc.impact.farmmanagement.entity.Worker;
import tc.impact.farmmanagement.service.WorkerService;

import java.util.List;

@RestController
@RequestMapping("/api/workers")
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerService workerService;

    @PostMapping
    public ResponseEntity<ApiResponse<WorkerResponse>> create(@Valid @RequestBody WorkerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Worker created", workerService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkerResponse>>> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Worker.WorkerStatus status) {

        if (name != null && !name.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(workerService.searchByName(name)));
        }
        if (status != null) {
            return ResponseEntity.ok(ApiResponse.success(workerService.getByStatus(status)));
        }
        return ResponseEntity.ok(ApiResponse.success(workerService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkerResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(workerService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkerResponse>> update(
            @PathVariable Long id, @Valid @RequestBody WorkerRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Worker updated", workerService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        workerService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Worker deleted", null));
    }
}