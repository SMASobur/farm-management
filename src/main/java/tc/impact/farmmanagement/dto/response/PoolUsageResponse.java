package tc.impact.farmmanagement.dto.response;

import lombok.Data;
import tc.impact.farmmanagement.entity.PoolProductType;

import java.time.LocalDate;

@Data
public class PoolUsageResponse {
    private Long id;
    private Long poolSpeciesId;
    private String poolSpeciesName;
    private PoolProductType poolProductType;
    private LocalDate date;
    private Double internalUse;
    private Double fedToAnimals;
    private Double wasted;
    private String notes;
    private LocalDate createdAt;
}