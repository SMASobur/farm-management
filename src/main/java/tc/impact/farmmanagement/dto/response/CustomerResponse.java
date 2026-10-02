package tc.impact.farmmanagement.dto.response;

import lombok.Data;
import tc.impact.farmmanagement.entity.Customer;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CustomerResponse {
    private Long id;
    private String name;
    private String phone;
    private String address;
    private Customer.CustomerType type;
    private String notes;
    private LocalDate createdAt;
    private BigDecimal totalDue;   // populated on-demand in service
}