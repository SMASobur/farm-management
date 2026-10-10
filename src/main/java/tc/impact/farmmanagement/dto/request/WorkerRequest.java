package tc.impact.farmmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tc.impact.farmmanagement.entity.Worker;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class WorkerRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 20)
    private String phone;

    @Size(max = 255)
    private String address;

    @Size(max = 100)
    private String role;

    @PositiveOrZero
    private BigDecimal monthlySalary;

    @PositiveOrZero
    private BigDecimal dailyWage;

    private LocalDate hireDate;

    private Worker.WorkerStatus status;

    @Size(max = 500)
    private String notes;
}