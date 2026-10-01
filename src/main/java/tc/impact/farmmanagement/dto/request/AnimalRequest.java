package tc.impact.farmmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tc.impact.farmmanagement.entity.Animal;

import java.time.LocalDate;

@Data
public class AnimalRequest {

    @NotBlank(message = "Tag number is required")
    @Size(max = 50, message = "Tag number too long")
    private String tagNumber;

    @Size(max = 100, message = "Name too long")
    private String name;

    @NotNull(message = "Species is required (COW, GOAT, CHICKEN, etc.)")
    private Animal.Species species;

    private Animal.AnimalStatus status;

    private Animal.Gender gender;

    private LocalDate dateOfBirth;
}