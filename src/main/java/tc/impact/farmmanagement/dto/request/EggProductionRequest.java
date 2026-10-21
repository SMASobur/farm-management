package tc.impact.farmmanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EggProductionRequest {

    private Long animalId;
    private Long flockId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @PositiveOrZero
    private Integer goodEggs;

    @PositiveOrZero
    private Integer crackedEggs;
}