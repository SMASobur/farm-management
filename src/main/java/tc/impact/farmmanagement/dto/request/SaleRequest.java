package tc.impact.farmmanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tc.impact.farmmanagement.entity.Payment;
import tc.impact.farmmanagement.entity.PoolProductType;
import tc.impact.farmmanagement.entity.Sale;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SaleRequest {

    private Long customerId;

    private String guestCustomerName;

    private String guestCustomerPhone;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    // --- Unit Fields ---
    private Long unitId;

    @Size(max = 50)
    private String customUnitName;

    @Size(max = 50)
    private String customUnitNameBn;

    @Size(max = 20)
    private String customUnitAbbreviation;

    private Boolean saveCustomUnitToUnits;

    // --- Product/Category Custom Fields ---
    @Size(max = 100)
    private String customProductName;

    @Size(max = 100)
    private String customProductNameBn;

    private Boolean saveCustomProductAsCategory;

    private Long customProductDefaultUnitId;

    // --- Quantity & Price ---
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    @NotNull(message = "Unit price is required")
    @Positive(message = "Unit price must be positive")
    private BigDecimal unitPrice;

    // --- Pool Fields ---
    private Long poolSpeciesId;
    private PoolProductType poolProductType;

    // --- Meta ---
    private String notes;
    private BigDecimal initialPayment;
    private Payment.PaymentMethod initialPaymentMethod;


    private String emoji;
}