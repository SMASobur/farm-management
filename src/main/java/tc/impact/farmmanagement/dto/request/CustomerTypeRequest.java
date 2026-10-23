package tc.impact.farmmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerTypeRequest {

    @NotBlank(message = "Customer type name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 100)
    private String nameBn;

    // Optional: Only used during creation or if explicitly changing the code
    @Size(max = 50)
    private String code;
}