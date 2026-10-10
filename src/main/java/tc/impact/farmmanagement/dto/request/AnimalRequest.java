package tc.impact.farmmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tc.impact.farmmanagement.entity.Animal;

import java.time.LocalDate;

@Data
public class AnimalRequest {

    @NotBlank(message = "Tag number is required")
    @Size(max = 50)
    private String tagNumber;

    @Size(max = 100)
    private String name;

    /**
     * Either speciesId OR customSpeciesName must be provided.
     */
    private Long speciesId;

    @Size(max = 100)
    private String customSpeciesName;

    /**
     * When true and customSpeciesName is provided, a new Species is created
     * and saved for reuse on this farm.
     */
    private Boolean saveCustomSpecies;

    // These are enums — Jackson converts "ACTIVE" → Animal.AnimalStatus.ACTIVE
    private Animal.AnimalStatus status;

    private Animal.Gender gender;

    private LocalDate dateOfBirth;
}