package tc.impact.farmmanagement.dto.response;

import lombok.Data;
import tc.impact.farmmanagement.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentResponse {
    private Long id;
    private Long saleId;
    private BigDecimal amount;
    private LocalDate date;
    private Payment.PaymentMethod method;
    private String notes;
    private LocalDate createdAt;
}