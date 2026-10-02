package tc.impact.farmmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tc.impact.farmmanagement.entity.Sale;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRevenueResponse {
    private Sale.ProductType productType;
    private BigDecimal revenue;
}