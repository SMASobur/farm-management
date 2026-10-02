package tc.impact.farmmanagement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tc.impact.farmmanagement.dto.request.MilkProductionRequest;
import tc.impact.farmmanagement.dto.response.ApiResponse;
import tc.impact.farmmanagement.dto.response.DailyMilkSummaryResponse;
import tc.impact.farmmanagement.dto.response.MilkProductionResponse;
import tc.impact.farmmanagement.service.MilkProductionService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/milk")
@RequiredArgsConstructor
public class MilkProductionController {

    private final MilkProductionService milkService;

    // ============ CRUD ============

    @PostMapping
    public ResponseEntity<ApiResponse<MilkProductionResponse>> create(
            @Valid @RequestBody MilkProductionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Milk entry created", milkService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MilkProductionResponse>>> getAll(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        // If date range provided, use range; otherwise return all
        if (from != null && to != null) {
            return ResponseEntity.ok(ApiResponse.success(milkService.getByRange(from, to)));
        }
        return ResponseEntity.ok(ApiResponse.success(milkService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MilkProductionResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(milkService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MilkProductionResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody MilkProductionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Milk entry updated",
                milkService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        milkService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Milk entry deleted", null));
    }

    // ============ Filters ============

    @GetMapping("/animal/{animalId}")
    public ResponseEntity<ApiResponse<List<MilkProductionResponse>>> getByAnimal(
            @PathVariable Long animalId) {
        return ResponseEntity.ok(ApiResponse.success(milkService.getByAnimal(animalId)));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<ApiResponse<List<MilkProductionResponse>>> getByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(milkService.getByDate(date)));
    }

    // ============ Summaries ============

    @GetMapping("/summary/daily")
    public ResponseEntity<ApiResponse<Double>> getDailyTotal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(milkService.getDailyTotal(date)));
    }

    @GetMapping("/summary/range")
    public ResponseEntity<ApiResponse<List<DailyMilkSummaryResponse>>> getRangeSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(milkService.getDailySummaries(from, to)));
    }

    @GetMapping("/summary/monthly")
    public ResponseEntity<ApiResponse<Double>> getMonthlyTotal(
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.success(milkService.getMonthlyTotal(year, month)));
    }
}