package tc.impact.farmmanagement.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
public class MonthlyReportResponse {

    private LocalDate from;
    private LocalDate to;

    private RevenueSection revenue;
    private ExpenseSection expenses;
    private ProductionSection production;
    private ProfitSection profit;

    // ============ Revenue ============

    @Data
    public static class RevenueSection {
        private BigDecimal grossSales;        // sum of Sale.totalAmount
        private BigDecimal cashCollected;     // sum of Payment.amount
        private BigDecimal outstandingDues;   // sum of Sale.dueAmount
        private Map<String, BigDecimal> byProduct;   // category code -> revenue
    }

    // ============ Expenses ============

    @Data
    public static class ExpenseSection {
        private BigDecimal total;
        private Map<String, BigDecimal> byCategory;  // category code -> total
    }

    // ============ Production ============

    @Data
    public static class ProductionSection {
        private Double milkLiters;
        private Long eggs;
    }

    // ============ Profit ============

    @Data
    public static class ProfitSection {
        private BigDecimal netProfit;
        private BigDecimal profitMargin;      // percentage, 2 decimals
    }
}