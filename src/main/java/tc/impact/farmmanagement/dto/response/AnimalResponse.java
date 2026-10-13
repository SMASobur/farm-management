package tc.impact.farmmanagement.dto.response;

import lombok.Data;
import tc.impact.farmmanagement.entity.Animal;

import java.time.LocalDate;

@Data
public class AnimalResponse {
    private Long id;
    private String tagNumber;
    private String name;

    private Long speciesId;
    private String speciesCode;
    private String speciesName;
    private String speciesNameBn;

    private Animal.LifecycleStatus lifecycleStatus;
    private Animal.Gender gender;
    private LocalDate dateOfBirth;
    private LocalDate createdAt;

    private boolean milkProducer;

    private boolean eggProducer;

    // ============ Derived from events ============

    private boolean currentlyPregnant;

    private boolean currentlySick;

    private boolean currentlyMilking;
}