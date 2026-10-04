package tc.impact.farmmanagement.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DashboardSummaryResponse {

    private LocalDate date;
    private TodaySection today;
    private MonthSection thisMonth;
    private OutstandingSection outstanding;
    private CountsSection counts;

    @Data
    public static class TodaySection {
        private Double milkLiters;
        private Long eggs;
        private BigDecimal salesRevenue;
        private BigDecimal cashCollected;
        private BigDecimal expenses;
    }

    @Data
    public static class MonthSection {
        private BigDecimal grossSales;
        private BigDecimal cashCollected;
        private BigDecimal expenses;
        private BigDecimal netProfit;
        private BigDecimal profitMargin;
    }

    @Data
    public static class OutstandingSection {
        private BigDecimal totalDues;
        private Long customersWithDues;
    }

    @Data
    public static class CountsSection {
        private Long totalAnimals;
        private Long cows;
        private Long goats;
        private Long sheep;
        private Long chickens;
        private Long ducks;
        private Long buffaloes;
        private Long activeFlocks;
        private Long totalCustomers;
    }
}