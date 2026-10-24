package tc.impact.farmmanagement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tc.impact.farmmanagement.dto.request.CustomerTypeRequest;
import tc.impact.farmmanagement.dto.response.ApiResponse;
import tc.impact.farmmanagement.dto.response.CustomerTypeResponse;
import tc.impact.farmmanagement.service.CustomerTypeService;

import java.util.List;

@RestController
@RequestMapping("/api/customer-types")
@RequiredArgsConstructor
public class CustomerTypeController {

    private final CustomerTypeService customerTypeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerTypeResponse>>> getAll(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.success(customerTypeService.getAll(activeOnly)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerTypeResponse>> create(@Valid @RequestBody CustomerTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Customer type created", customerTypeService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerTypeResponse>> update(
            @PathVariable Long id, @Valid @RequestBody CustomerTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Customer type updated", customerTypeService.update(id, request)));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<ApiResponse<CustomerTypeResponse>> toggleActive(
            @PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(ApiResponse.success("Status updated", customerTypeService.toggleActive(id, active)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        customerTypeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Customer type deleted", null));
    }
}