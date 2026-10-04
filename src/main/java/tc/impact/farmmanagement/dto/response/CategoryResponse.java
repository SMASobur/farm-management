package tc.impact.farmmanagement.dto.response;

import lombok.Data;
import tc.impact.farmmanagement.entity.Category;

import java.time.LocalDate;

@Data
public class CategoryResponse {
    private Long id;
    private String code;
    private String name;
    private String nameBn;
    private Category.CategoryType type;
    private boolean active;
    private boolean systemDefault;
    private Integer displayOrder;
    private LocalDate createdAt;
    private Long defaultUnitId;
    private String defaultUnitCode;
    private String defaultUnitName;
    private java.math.BigDecimal defaultPrice;
}