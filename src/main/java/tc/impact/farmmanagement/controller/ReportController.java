package tc.impact.farmmanagement.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tc.impact.farmmanagement.dto.response.ApiResponse;
import tc.impact.farmmanagement.dto.response.MonthlyReportResponse;
import tc.impact.farmmanagement.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import tc.impact.farmmanagement.service.ExportService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * Full P&L report for a custom date range.
     * GET /api/reports?from=2026-10-01&to=2026-10-31
     */
    @GetMapping
    public ResponseEntity<ApiResponse<MonthlyReportResponse>> getReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(reportService.buildReport(from, to)));
    }

    /**
     * Monthly P&L report.
     * GET /api/reports/monthly?year=2026&month=10
     */
    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<MonthlyReportResponse>> getMonthlyReport(
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.success(reportService.buildMonthlyReport(year, month)));
    }
    @GetMapping("/export.xlsx")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        byte[] bytes = exportService.exportReportAsExcel(from, to);
        String filename = "farm-report-" + from + "-to-" + to + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/export.pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        byte[] bytes = exportService.exportReportAsPdf(from, to);
        String filename = "farm-report-" + from + "-to-" + to + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    private final ExportService exportService;
}