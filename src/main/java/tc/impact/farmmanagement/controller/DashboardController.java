package tc.impact.farmmanagement.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tc.impact.farmmanagement.dto.response.ApiResponse;
import tc.impact.farmmanagement.dto.response.DashboardAlertsResponse;
import tc.impact.farmmanagement.dto.response.DashboardSummaryResponse;
import tc.impact.farmmanagement.service.AlertService;
import tc.impact.farmmanagement.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.buildSummary()));
    }

    private final AlertService alertService;   // add to constructor

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<DashboardAlertsResponse>> getAlerts() {
        return ResponseEntity.ok(ApiResponse.success(alertService.getAlerts()));
    }
}