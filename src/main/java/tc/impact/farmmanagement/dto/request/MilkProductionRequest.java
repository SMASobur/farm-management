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

    @PositiveOrZero
    private Double morningLiters;

    @PositiveOrZero
    private Double noonLiters;

    @PositiveOrZero
    private Double nightLiters;

    @PositiveOrZero
    private Double litersInternal;

    @PositiveOrZero
    private Double litersFed;

    @PositiveOrZero
    private Double litersWasted;
}