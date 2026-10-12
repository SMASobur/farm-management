package tc.impact.farmmanagement.dto.request;

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

    private Long customerTypeId;

    @Size(max = 100)
    private String customTypeName;

    @Size(max = 100)
    private String customTypeNameBn;

    private Boolean saveCustomType;

    @Size(max = 500)
    private String notes;
}