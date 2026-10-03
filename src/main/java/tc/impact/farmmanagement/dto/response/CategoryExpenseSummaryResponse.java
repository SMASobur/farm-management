package tc.impact.farmmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tc.impact.farmmanagement.entity.Expense;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryExpenseSummaryResponse {
    private Expense.ExpenseCategory category;
    private BigDecimal total;
}