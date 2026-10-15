package tc.impact.farmmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tc.impact.farmmanagement.entity.ProductionEvent;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductionEventRequest {

    @NotNull(message = "Animal is required")
    private Long animalId;

    private LocalDate date;

    @NotNull(message = "Event type is required")
    private ProductionEvent.EventType eventType;

    @Size(max = 200)
    private String title;

    @Size(max = 1000)
    private String description;

    @PositiveOrZero
    private BigDecimal cost;
}