package tc.impact.farmmanagement.dto.response;

import lombok.Data;
import tc.impact.farmmanagement.entity.PoolProductType;

import java.time.LocalDate;

@Data
public class EggProductionResponse {
    private Long id;
    private Long animalId;
    private Long flockId;
    private String sourceType;
    private String sourceDisplayName;
    private LocalDate date;
    private Integer goodEggs;
    private Integer crackedEggs;
    private Integer totalEggs;

    // Pool info
    private Long poolSpeciesId;
    private String poolSpeciesCode;
    private String poolSpeciesName;
    private PoolProductType poolProductType;

    private Long enteredByUserId;
    private String enteredByName;
}