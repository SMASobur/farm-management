package tc.impact.farmmanagement.dto.response;

import lombok.Data;

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
    private Long workerId;
    private String workerName;
    private String description;
    private BigDecimal amount;
    private String notes;
    private LocalDate createdAt;
    private Long enteredByUserId;
    private String enteredByName;
}