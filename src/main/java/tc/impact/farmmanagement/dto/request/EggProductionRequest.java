package tc.impact.farmmanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EggProductionRequest {

    /**
     * Exactly ONE of animalId or flockId must be provided.
     */
    private Long animalId;
    private Long flockId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @PositiveOrZero(message = "Good eggs cannot be negative")
    private Integer goodEggs;

    @PositiveOrZero(message = "Cracked eggs cannot be negative")
    private Integer crackedEggs;
}