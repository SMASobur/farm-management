package tc.impact.farmmanagement.dto.response;

import lombok.Data;

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

    private String status;
    private String gender;
    private LocalDate dateOfBirth;
    private LocalDate createdAt;
    private boolean milkProducer;
}