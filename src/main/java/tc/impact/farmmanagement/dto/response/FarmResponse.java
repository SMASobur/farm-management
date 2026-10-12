package tc.impact.farmmanagement.dto.response;

import lombok.Data;
import tc.impact.farmmanagement.entity.Farm;

import java.time.LocalDate;

@Data
public class FarmResponse {
    private Long id;
    private String code;
    private String name;
    private String location;
    private String ownerName;
    private String ownerPhone;
    private String ownerEmail;
    private String logoUrl;
    private Farm.SubscriptionPlan plan;
    private boolean active;
    private LocalDate createdAt;
}