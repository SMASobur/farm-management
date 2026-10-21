package tc.impact.farmmanagement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tc.impact.farmmanagement.dto.request.PoolUsageRequest;
import tc.impact.farmmanagement.dto.response.ApiResponse;
import tc.impact.farmmanagement.dto.response.PoolStatusResponse;
import tc.impact.farmmanagement.dto.response.PoolUsageResponse;
import tc.impact.farmmanagement.service.FarmContext;
import tc.impact.farmmanagement.service.PoolService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pools")
@RequiredArgsConstructor
public class PoolController {

    private final PoolService poolService;
    private final FarmContext farmContext;

    /**
     * Get status of all active pools for a date (defaults to today).
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<List<PoolStatusResponse>>> getPoolsStatus(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate target = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(
                poolService.getPoolsStatus(farmContext.getCurrentFarmId(), target)));
    }

    /**
     * Record usage for a pool (creates or updates the day's usage).
     */
    @PostMapping("/usage")
    public ResponseEntity<ApiResponse<PoolUsageResponse>> recordUsage(
            @Valid @RequestBody PoolUsageRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Usage recorded",
                poolService.recordUsage(request)));
    }
}