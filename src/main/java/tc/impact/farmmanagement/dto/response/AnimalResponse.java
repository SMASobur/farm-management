package tc.impact.farmmanagement.dto.response;

import lombok.Data;
import tc.impact.farmmanagement.entity.Animal;

import java.time.LocalDate;

@Data
public class AnimalResponse {
    private Long id;
    private String tagNumber;
    private String name;
    private Animal.Species species;
    private Animal.AnimalStatus status;
    private Animal.Gender gender;
    private LocalDate dateOfBirth;
    private LocalDate createdAt;
    private boolean milkProducer;   // convenience flag for frontend
}