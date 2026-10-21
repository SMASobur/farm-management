package tc.impact.farmmanagement.dto.response;

import lombok.Data;
import tc.impact.farmmanagement.entity.PoolProductType;

import java.time.LocalDate;

@Data
public class MilkProductionResponse {
    private Long id;
    private Long animalId;
    private String animalTagNumber;
    private String animalName;
    private LocalDate date;
    private Double morningLiters;
    private Double noonLiters;
    private Double nightLiters;
    private Double totalLiters;

    // Pool info
    private Long poolSpeciesId;
    private String poolSpeciesCode;
    private String poolSpeciesName;
    private PoolProductType poolProductType;

    private Long enteredByUserId;
    private String enteredByName;
}