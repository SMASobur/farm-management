package tc.impact.farmmanagement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 20)
    private String phone;

    @Size(max = 255)
    private String address;

    /**
     * Either customerTypeId OR customTypeName must be provided (not both).
     * customerTypeId may be null → default to HOUSEHOLD type server-side.
     */
    private Long customerTypeId;

    @Size(max = 100)
    private String customTypeName;

    /**
     * When true and customTypeName is provided, creates a new CustomerType
     * for reuse on this farm.
     */
    private Boolean saveCustomType;

    @Size(max = 500)
    private String notes;
}