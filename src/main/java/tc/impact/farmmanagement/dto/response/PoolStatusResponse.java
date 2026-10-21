package tc.impact.farmmanagement.dto.response;

import lombok.Data;
import tc.impact.farmmanagement.entity.PoolProductType;

import java.time.LocalDate;

@Data
public class PoolStatusResponse {

    private Long poolSpeciesId;
    private String poolSpeciesCode;
    private String poolSpeciesName;
    private PoolProductType poolProductType;
    private String poolProductTypeLabel;   // "Cow Milk", "Chicken Eggs"

    private LocalDate date;

    // Inventory
    private Double produced;
    private Double sold;
    private Double internalUse;
    private Double fedToAnimals;
    private Double wasted;

    /**
     * Produced - sold - internal - fed - wasted
     */
    private Double available;
}