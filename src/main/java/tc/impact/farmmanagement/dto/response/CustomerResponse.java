package tc.impact.farmmanagement.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CustomerResponse {
    private Long id;
    private String name;
    private String phone;
    private String address;

    private Long customerTypeId;
    private String customerTypeCode;
    private String customerTypeName;
    private String customerTypeNameBn;

    private String notes;
    private LocalDate createdAt;
    private BigDecimal totalDue;
}