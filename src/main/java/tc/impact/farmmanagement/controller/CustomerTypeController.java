package tc.impact.farmmanagement.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
}