package tc.impact.farmmanagement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tc.impact.farmmanagement.dto.request.CustomerRequest;
import tc.impact.farmmanagement.dto.response.ApiResponse;
import tc.impact.farmmanagement.dto.response.CustomerResponse;
import tc.impact.farmmanagement.entity.Customer;
import tc.impact.farmmanagement.service.CustomerService;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> create(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created", customerService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long customerTypeId) {

        if (name != null && !name.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(customerService.searchByName(name)));
        }
        if (customerTypeId != null) {
            return ResponseEntity.ok(ApiResponse.success(customerService.getByType(customerTypeId)));
        }
        return ResponseEntity.ok(ApiResponse.success(customerService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(customerService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> update(
            @PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Customer updated", customerService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted", null));
    }
}