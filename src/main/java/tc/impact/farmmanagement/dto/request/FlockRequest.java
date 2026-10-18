package tc.impact.farmmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tc.impact.farmmanagement.entity.Flock;

import java.time.LocalDate;

@Data
public class FlockRequest {

    @NotBlank(message = "Flock code is required")
    @Size(max = 50)
    private String flockCode;

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

    @NotNull(message = "Bird count is required")
    @Positive(message = "Bird count must be positive")
    private Integer birdCount;

    private Flock.FlockStatus status;

    private LocalDate startedOn;
}