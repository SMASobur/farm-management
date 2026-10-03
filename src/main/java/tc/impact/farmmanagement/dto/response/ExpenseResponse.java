package tc.impact.farmmanagement.dto.response;

import lombok.Data;
import tc.impact.farmmanagement.entity.Category;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExpenseResponse {
    private Long id;
    private LocalDate date;
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private String categoryNameBn;
    private String description;
    private BigDecimal amount;
    private String notes;
    private LocalDate createdAt;
}