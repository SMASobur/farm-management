package tc.impact.farmmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UnitRequest {

    @NotBlank(message = "Code is required")
    @Size(max = 20)
    private String code;

    @NotBlank(message = "Name is required")
    @Size(max = 50)
    private String name;

    @Size(max = 50)
    private String nameBn;

    @Size(max = 10)
    private String abbreviation;

    private Boolean active;

    private Integer displayOrder;
}