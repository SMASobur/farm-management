package tc.impact.farmmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryExpenseSummaryResponse {
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private BigDecimal total;
}