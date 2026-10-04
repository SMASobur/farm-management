package tc.impact.farmmanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import tc.impact.farmmanagement.entity.Payment;
import tc.impact.farmmanagement.entity.Sale;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SaleRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Category ID is required (e.g., milk, egg, animal)")
    private Long categoryId;

    @NotNull(message = "Unit is required")
    private Long unitId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    @NotNull(message = "Unit price is required")
    @Positive(message = "Unit price must be positive")
    private BigDecimal unitPrice;

    private String notes;

    private BigDecimal initialPayment;

    private Payment.PaymentMethod initialPaymentMethod;
}