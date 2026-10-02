package tc.impact.farmmanagement.dto.response;

import lombok.Data;

import java.time.LocalDate;

@Data
public class EggProductionResponse {
    private Long id;
    private Long animalId;         // nullable
    private Long flockId;          // nullable
    private String sourceType;     // "ANIMAL" or "FLOCK"
    private String sourceDisplayName;
    private LocalDate date;
    private Integer goodEggs;
    private Integer crackedEggs;
    private Integer totalEggs;
}