package tc.impact.farmmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAlert {

    /**
     * Alert category — used by frontend to pick icon/color.
     * LOW_MILK_PRODUCTION, OVERDUE_PAYMENT, SALARY_DUE
     */
    private String type;

    /**
     * Severity: INFO, WARNING, DANGER
     */
    private String severity;

    /**
     * Short title shown in the alert row.
     * e.g., "Cow-01 milk drop"
     */
    private String title;

    /**
     * Longer description with context.
     * e.g., "Produced 4.0L today vs 12.3L 7-day average"
     */
    private String message;

    /**
     * Optional numeric value (for sorting or display).
     */
    private BigDecimal value;

    /**
     * Deep link to the relevant page.
     * e.g., "/animals/12" or "/customers/5"
     */
    private String link;
}