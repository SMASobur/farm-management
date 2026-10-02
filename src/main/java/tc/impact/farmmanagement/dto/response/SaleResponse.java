package tc.impact.farmmanagement.dto.response;

import lombok.Data;
import tc.impact.farmmanagement.entity.Sale;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SaleResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private LocalDate date;
    private Sale.ProductType productType;
    private Sale.Unit unit;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal dueAmount;
    private String notes;
    private LocalDate createdAt;
}