package tc.impact.farmmanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tc.impact.farmmanagement.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SaleRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private Long unitId;

    @Size(max = 50)
    private String customUnitName;

    @Size(max = 50)
    private String customUnitNameBn;

    @Size(max = 10)
    private String customUnitAbbreviation;

    private Boolean saveCustomUnitToUnits;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    @NotNull(message = "Unit price is required")
    @Positive(message = "Unit price must be positive")
    private BigDecimal unitPrice;

    @Size(max = 100)
    private String customProductName;

    @Size(max = 100)
    private String customProductNameBn;

    private Long customProductDefaultUnitId;

    private Boolean saveCustomProductAsCategory;

    private String notes;

    private BigDecimal initialPayment;

    private Payment.PaymentMethod initialPaymentMethod;
}