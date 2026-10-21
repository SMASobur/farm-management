package tc.impact.farmmanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import tc.impact.farmmanagement.entity.PoolProductType;

import java.time.LocalDate;

@Data
public class PoolUsageRequest {

    @NotNull(message = "Pool species ID is required")
    private Long poolSpeciesId;

    @NotNull(message = "Pool product type is required")
    private PoolProductType poolProductType;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @PositiveOrZero
    private Double internalUse;

    @PositiveOrZero
    private Double fedToAnimals;

    @PositiveOrZero
    private Double wasted;

    private String notes;
}