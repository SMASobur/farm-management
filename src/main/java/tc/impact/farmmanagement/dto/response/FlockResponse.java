package tc.impact.farmmanagement.dto.response;

import lombok.Data;
import tc.impact.farmmanagement.entity.Flock;

import java.time.LocalDate;

@Data
public class FlockResponse {
    private Long id;
    private String flockCode;
    private String name;

    private Long speciesId;
    private String speciesCode;
    private String speciesName;
    private String speciesNameBn;

    private Integer birdCount;
    private Flock.FlockStatus status;
    private LocalDate startedOn;
    private LocalDate createdAt;
}