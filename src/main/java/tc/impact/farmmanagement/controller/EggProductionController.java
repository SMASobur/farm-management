package tc.impact.farmmanagement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tc.impact.farmmanagement.dto.request.EggProductionRequest;
import tc.impact.farmmanagement.dto.response.ApiResponse;
import tc.impact.farmmanagement.dto.response.DailyEggSummaryResponse;
import tc.impact.farmmanagement.dto.response.EggProductionResponse;
import tc.impact.farmmanagement.service.EggProductionService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/eggs")
@RequiredArgsConstructor
public class EggProductionController {

    private final EggProductionService eggService;

    // ============ CRUD ============

    @PostMapping
    public ResponseEntity<ApiResponse<EggProductionResponse>> create(
            @Valid @RequestBody EggProductionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Egg entry created", eggService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EggProductionResponse>>> getAll(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from != null && to != null) {
            return ResponseEntity.ok(ApiResponse.success(eggService.getByRange(from, to)));
        }
        return ResponseEntity.ok(ApiResponse.success(eggService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EggProductionResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(eggService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EggProductionResponse>> update(
            @PathVariable Long id, @Valid @RequestBody EggProductionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Egg entry updated", eggService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        eggService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Egg entry deleted", null));
    }

    // ============ Filters ============

    @GetMapping("/animal/{animalId}")
    public ResponseEntity<ApiResponse<List<EggProductionResponse>>> getByAnimal(@PathVariable Long animalId) {
        return ResponseEntity.ok(ApiResponse.success(eggService.getByAnimal(animalId)));
    }

    @GetMapping("/flock/{flockId}")
    public ResponseEntity<ApiResponse<List<EggProductionResponse>>> getByFlock(@PathVariable Long flockId) {
        return ResponseEntity.ok(ApiResponse.success(eggService.getByFlock(flockId)));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<ApiResponse<List<EggProductionResponse>>> getByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(eggService.getByDate(date)));
    }

    // ============ Summaries ============

    @GetMapping("/summary/daily")
    public ResponseEntity<ApiResponse<Long>> getDailyTotal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(eggService.getDailyTotal(date)));
    }

    @GetMapping("/summary/range")
    public ResponseEntity<ApiResponse<List<DailyEggSummaryResponse>>> getRangeSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(eggService.getDailySummaries(from, to)));
    }

    @GetMapping("/summary/monthly")
    public ResponseEntity<ApiResponse<Long>> getMonthlyTotal(
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.success(eggService.getMonthlyTotal(year, month)));
    }
}