package tc.impact.farmmanagement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tc.impact.farmmanagement.dto.request.SaleRequest;
import tc.impact.farmmanagement.dto.response.*;
import tc.impact.farmmanagement.service.SaleService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    // ============ CRUD ============

    @PostMapping
    public ResponseEntity<ApiResponse<SaleResponse>> create(@Valid @RequestBody SaleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Sale created", saleService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SaleResponse>>> getAll(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long categoryId) {

        if (from != null && to != null) {
            return ResponseEntity.ok(ApiResponse.success(saleService.getByRange(from, to)));
        }
        if (categoryId != null) {
            return ResponseEntity.ok(ApiResponse.success(saleService.getByCategory(categoryId)));
        }
        return ResponseEntity.ok(ApiResponse.success(saleService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SaleResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(saleService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SaleResponse>> update(
            @PathVariable Long id, @Valid @RequestBody SaleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Sale updated", saleService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        saleService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Sale deleted", null));
    }

    // ============ Filters ============

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<SaleResponse>>> getByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.success(saleService.getByCustomer(customerId)));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<ApiResponse<List<SaleResponse>>> getByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(saleService.getByDate(date)));
    }

    // ============ Dues ============

    @GetMapping("/dues")
    public ResponseEntity<ApiResponse<List<SaleResponse>>> getAllWithDues() {
        return ResponseEntity.ok(ApiResponse.success(saleService.getAllWithDues()));
    }

    @GetMapping("/dues/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<SaleResponse>>> getDuesByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.success(saleService.getDuesByCustomer(customerId)));
    }

    @GetMapping("/dues/total")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalDues() {
        return ResponseEntity.ok(ApiResponse.success(saleService.getAllDues()));
    }

    // ============ Revenue summaries ============

    @GetMapping("/summary/daily")
    public ResponseEntity<ApiResponse<BigDecimal>> getDailyRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(saleService.getDailyRevenue(date)));
    }

    @GetMapping("/summary/range")
    public ResponseEntity<ApiResponse<List<DailyRevenueSummaryResponse>>> getRangeRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(saleService.getDailyRevenueRange(from, to)));
    }

    @GetMapping("/summary/by-category")
    public ResponseEntity<ApiResponse<List<ProductRevenueResponse>>> getRevenueByCategory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(saleService.getRevenueByCategory(from, to)));
    }
}