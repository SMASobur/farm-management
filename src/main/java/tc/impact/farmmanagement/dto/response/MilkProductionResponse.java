package tc.impact.farmmanagement.dto.response;

import lombok.Data;

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
    private Long enteredByUserId;
    private String enteredByName;
    private Double litersSold;
    private Double litersInternal;
    private Double litersFed;
    private Double litersWasted;
    private Double availableForSale;
}