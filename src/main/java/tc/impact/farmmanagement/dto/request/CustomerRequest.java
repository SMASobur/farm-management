package tc.impact.farmmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tc.impact.farmmanagement.entity.Customer;

@Data
public class CustomerRequest {

    @NotBlank(message = "Customer name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 20)
    private String phone;

    @Size(max = 255)
    private String address;

    private Customer.CustomerType type;

    @Size(max = 500)
    private String notes;
}