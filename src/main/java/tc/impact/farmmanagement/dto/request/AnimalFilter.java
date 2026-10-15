package tc.impact.farmmanagement.dto.request;

import lombok.Data;
import tc.impact.farmmanagement.entity.Animal;

import java.time.LocalDate;

@Data
public class AnimalFilter {
    private Long speciesId;
    private Animal.LifecycleStatus status;

    private AnimalCondition condition;

    private String search;
    public enum AnimalCondition {
        PREGNANT,
        SICK,
        MILKING
    }
}