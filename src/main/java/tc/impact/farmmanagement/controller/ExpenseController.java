package tc.impact.farmmanagement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tc.impact.farmmanagement.dto.request.ExpenseRequest;
import tc.impact.farmmanagement.dto.response.ApiResponse;
import tc.impact.farmmanagement.dto.response.CategoryExpenseSummaryResponse;
import tc.impact.farmmanagement.dto.response.DailyExpenseSummaryResponse;
import tc.impact.farmmanagement.dto.response.ExpenseResponse;
import tc.impact.farmmanagement.service.ExpenseService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseResponse>> create(@Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Expense recorded", expenseService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getAll(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (categoryId != null && from != null && to != null) {
            return ResponseEntity.ok(ApiResponse.success(
                    expenseService.getByCategoryAndRange(categoryId, from, to)));
        }
        if (from != null && to != null) {
            return ResponseEntity.ok(ApiResponse.success(expenseService.getByRange(from, to)));
        }
        if (categoryId != null) {
            return ResponseEntity.ok(ApiResponse.success(expenseService.getByCategory(categoryId)));
        }
        return ResponseEntity.ok(ApiResponse.success(expenseService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Expense updated", expenseService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        expenseService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Expense deleted", null));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.getByDate(date)));
    }

    @GetMapping("/summary/daily")
    public ResponseEntity<ApiResponse<BigDecimal>> getDailyTotal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.getDailyTotal(date)));
    }

    @GetMapping("/summary/range")
    public ResponseEntity<ApiResponse<List<DailyExpenseSummaryResponse>>> getRangeSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.getDailySummaries(from, to)));
    }

    @GetMapping("/summary/total")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.getTotalBetween(from, to)));
    }

    @GetMapping("/summary/by-category")
    public ResponseEntity<ApiResponse<List<CategoryExpenseSummaryResponse>>> getCategorySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.getCategorySummaries(from, to)));
    }
}