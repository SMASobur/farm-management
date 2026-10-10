package tc.impact.farmmanagement.dto.response;

import lombok.Data;
import tc.impact.farmmanagement.entity.Worker;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class WorkerResponse {
    private Long id;
    private String name;
    private String phone;
    private String address;
    private String role;
    private BigDecimal monthlySalary;
    private BigDecimal dailyWage;
    private LocalDate hireDate;
    private Worker.WorkerStatus status;
    private String notes;
    private LocalDate createdAt;
}