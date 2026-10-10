package tc.impact.farmmanagement.dto.response;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CustomerTypeResponse {
    private Long id;
    private String code;
    private String name;
    private String nameBn;
    private boolean active;
    private boolean systemDefault;
    private Integer displayOrder;
    private LocalDate createdAt;
}