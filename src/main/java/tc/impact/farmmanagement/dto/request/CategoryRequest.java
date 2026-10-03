package tc.impact.farmmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tc.impact.farmmanagement.entity.Category;

@Data
public class CategoryRequest {

    @NotBlank(message = "Code is required")
    @Size(max = 50)
    private String code;

    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 100)
    private String nameBn;

    @NotNull(message = "Type is required (EXPENSE or PRODUCT)")
    private Category.CategoryType type;

    private Boolean active;

    private Integer displayOrder;
}