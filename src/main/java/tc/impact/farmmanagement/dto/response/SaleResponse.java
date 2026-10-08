package tc.impact.farmmanagement.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SaleResponse {
    private Long id;

    private Long customerId;
    private String customerName;
    private String customerPhone;

    private LocalDate date;

    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private String categoryNameBn;

    private Long unitId;
    private String unitCode;
    private String unitName;
    private String unitAbbreviation;

    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal dueAmount;

    private String customProductName;
    private String notes;

    private LocalDate createdAt;
}