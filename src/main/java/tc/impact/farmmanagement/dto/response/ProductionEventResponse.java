package tc.impact.farmmanagement.dto.response;

import lombok.Data;
import tc.impact.farmmanagement.entity.ProductionEvent;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductionEventResponse {
    private Long id;

    private Long animalId;
    private String animalTagNumber;

    private LocalDate date;
    private ProductionEvent.EventType eventType;
    private String title;
    private String description;
    private BigDecimal cost;

    private Long enteredByUserId;
    private String enteredByName;

    private LocalDate createdAt;
}