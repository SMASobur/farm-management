package tc.impact.farmmanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MilkProductionRequest {

    @NotNull(message = "Animal ID is required")
    private Long animalId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @PositiveOrZero(message = "Morning liters cannot be negative")
    private Double morningLiters;

    @PositiveOrZero(message = "Noon liters cannot be negative")
    private Double noonLiters;

    @PositiveOrZero(message = "Night liters cannot be negative")
    private Double nightLiters;
}