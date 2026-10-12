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

    private Long speciesId;

    @Size(max = 100)
    private String customSpeciesName;

    @Size(max = 100)
    private String customSpeciesNameBn;

    private Boolean customSpeciesMilkProducer;
    private Boolean customSpeciesEggProducer;

    private Boolean saveCustomSpecies;

    private Animal.AnimalStatus status;

    private Animal.Gender gender;

    private LocalDate dateOfBirth;
}