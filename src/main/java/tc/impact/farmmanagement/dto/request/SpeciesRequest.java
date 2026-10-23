package tc.impact.farmmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SpeciesRequest {
    @NotBlank(message = "Species name is required")
    private String name;

    private String nameBn;

    @NotBlank(message = "Species code is required")
    private String code;

    private boolean milkProducer;
    private boolean eggProducer;
    private Boolean active;

    private String emoji;
}