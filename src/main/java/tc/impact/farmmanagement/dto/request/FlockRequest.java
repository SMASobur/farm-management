package tc.impact.farmmanagement.dto.request;

import jakarta.validation.constraints.*;
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

    @NotNull(message = "Species is required")
    private Flock.Species species;

    @NotNull(message = "Bird count is required")
    @Positive(message = "Bird count must be positive")
    private Integer birdCount;

    private Flock.FlockStatus status;

    private LocalDate startedOn;
}