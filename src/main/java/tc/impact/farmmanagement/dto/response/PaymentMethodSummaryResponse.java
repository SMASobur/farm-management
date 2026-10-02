package tc.impact.farmmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tc.impact.farmmanagement.entity.Payment;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodSummaryResponse {
    private Payment.PaymentMethod method;
    private BigDecimal amount;
}