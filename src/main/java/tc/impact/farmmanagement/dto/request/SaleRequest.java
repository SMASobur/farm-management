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

    @NotNull(message = "Product type is required")
    private Sale.ProductType productType;

    @NotNull(message = "Unit is required")
    private Sale.Unit unit;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    @NotNull(message = "Unit price is required")
    @Positive(message = "Unit price must be positive")
    private BigDecimal unitPrice;

    private String notes;

    // Optional: initial payment recorded at sale time
    private BigDecimal initialPayment;

    // Optional: payment method for the initial payment
    private Payment.PaymentMethod initialPaymentMethod;
}