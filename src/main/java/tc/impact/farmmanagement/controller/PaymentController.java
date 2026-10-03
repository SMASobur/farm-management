package tc.impact.farmmanagement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tc.impact.farmmanagement.dto.request.PaymentRequest;
import tc.impact.farmmanagement.dto.response.ApiResponse;
import tc.impact.farmmanagement.dto.response.PaymentMethodSummaryResponse;
import tc.impact.farmmanagement.dto.response.PaymentResponse;
import tc.impact.farmmanagement.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // ============ CRUD ============

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> create(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment recorded", paymentService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getAll(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from != null && to != null) {
            return ResponseEntity.ok(ApiResponse.success(paymentService.getByRange(from, to)));
        }
        return ResponseEntity.ok(ApiResponse.success(paymentService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> update(
            @PathVariable Long id, @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payment updated", paymentService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        paymentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Payment deleted", null));
    }

    // ============ Filters ============

    @GetMapping("/sale/{saleId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getBySale(@PathVariable Long saleId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getBySale(saleId)));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getByDate(date)));
    }

    // ============ Summaries ============

    @GetMapping("/summary/daily")
    public ResponseEntity<ApiResponse<BigDecimal>> getDailyCash(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getDailyCash(date)));
    }

    @GetMapping("/summary/range")
    public ResponseEntity<ApiResponse<BigDecimal>> getCashBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getCashBetween(from, to)));
    }

    @GetMapping("/summary/by-method")
    public ResponseEntity<ApiResponse<List<PaymentMethodSummaryResponse>>> getCashByMethod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getCashByMethod(from, to)));
    }
}