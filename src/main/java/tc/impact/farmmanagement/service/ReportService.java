package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.response.MonthlyReportResponse;
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.repository.EggProductionRepository;
import tc.impact.farmmanagement.repository.ExpenseRepository;
import tc.impact.farmmanagement.repository.MilkProductionRepository;
import tc.impact.farmmanagement.repository.PaymentRepository;
import tc.impact.farmmanagement.repository.SaleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final SaleRepository saleRepository;
    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;
    private final MilkProductionRepository milkRepository;
    private final EggProductionRepository eggRepository;
    private final FarmContext farmContext;

    @Transactional(readOnly = true)
    public MonthlyReportResponse buildReport(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException("'from' date cannot be after 'to' date");
        }
        Long farmId = farmContext.getCurrentFarmId();

        MonthlyReportResponse report = new MonthlyReportResponse();
        report.setFrom(from);
        report.setTo(to);
        report.setRevenue(buildRevenue(farmId, from, to));
        report.setExpenses(buildExpenses(farmId, from, to));
        report.setProduction(buildProduction(farmId, from, to));
        report.setProfit(buildProfit(report.getRevenue(), report.getExpenses()));
        return report;
    }

    @Transactional(readOnly = true)
    public MonthlyReportResponse buildMonthlyReport(int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        return buildReport(from, to);
    }

    // ============ Revenue ============

    private MonthlyReportResponse.RevenueSection buildRevenue(Long farmId, LocalDate from, LocalDate to) {
        MonthlyReportResponse.RevenueSection revenue = new MonthlyReportResponse.RevenueSection();

        revenue.setGrossSales(safe(saleRepository.sumRevenueByFarmBetween(farmId, from, to)));
        revenue.setCashCollected(safe(paymentRepository.sumAmountByFarmBetween(farmId, from, to)));
        revenue.setOutstandingDues(safe(saleRepository.sumOutstandingDuesByFarmBetween(farmId, from, to)));

        Map<String, BigDecimal> byProduct = new LinkedHashMap<>();
        for (Object[] row : saleRepository.sumRevenueByCategoryCodeBetween(farmId, from, to)) {
            byProduct.put((String) row[0], safe((BigDecimal) row[1]));
        }
        revenue.setByProduct(byProduct);

        return revenue;
    }

    // ============ Expenses ============

    private MonthlyReportResponse.ExpenseSection buildExpenses(Long farmId, LocalDate from, LocalDate to) {
        MonthlyReportResponse.ExpenseSection expenses = new MonthlyReportResponse.ExpenseSection();

        expenses.setTotal(safe(expenseRepository.sumAmountByFarmBetween(farmId, from, to)));

        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        for (Object[] row : expenseRepository.sumAmountByCategoryCodeBetween(farmId, from, to)) {
            byCategory.put((String) row[0], safe((BigDecimal) row[1]));
        }
        expenses.setByCategory(byCategory);

        return expenses;
    }

    // ============ Production ============

    private MonthlyReportResponse.ProductionSection buildProduction(Long farmId, LocalDate from, LocalDate to) {
        MonthlyReportResponse.ProductionSection production = new MonthlyReportResponse.ProductionSection();
        production.setMilkLiters(milkRepository.sumLitersByFarmBetween(farmId, from, to));
        production.setEggs(eggRepository.sumEggsByFarmBetween(farmId, from, to));
        return production;
    }

    // ============ Profit ============

    private MonthlyReportResponse.ProfitSection buildProfit(
            MonthlyReportResponse.RevenueSection revenue,
            MonthlyReportResponse.ExpenseSection expenses) {

        MonthlyReportResponse.ProfitSection profit = new MonthlyReportResponse.ProfitSection();

        BigDecimal net = revenue.getGrossSales().subtract(expenses.getTotal());
        profit.setNetProfit(net);

        if (revenue.getGrossSales().signum() == 0) {
            profit.setProfitMargin(BigDecimal.ZERO);
        } else {
            profit.setProfitMargin(net
                    .multiply(BigDecimal.valueOf(100))
                    .divide(revenue.getGrossSales(), 2, RoundingMode.HALF_UP));
        }

        return profit;
    }

    // ============ Helpers ============

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }
}