package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.response.DashboardSummaryResponse;
import tc.impact.farmmanagement.entity.Animal;
import tc.impact.farmmanagement.entity.Flock;
import tc.impact.farmmanagement.repository.AnimalRepository;
import tc.impact.farmmanagement.repository.CustomerRepository;
import tc.impact.farmmanagement.repository.EggProductionRepository;
import tc.impact.farmmanagement.repository.ExpenseRepository;
import tc.impact.farmmanagement.repository.FlockRepository;
import tc.impact.farmmanagement.repository.MilkProductionRepository;
import tc.impact.farmmanagement.repository.PaymentRepository;
import tc.impact.farmmanagement.repository.SaleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SaleRepository saleRepository;
    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;
    private final MilkProductionRepository milkRepository;
    private final EggProductionRepository eggRepository;
    private final AnimalRepository animalRepository;
    private final FlockRepository flockRepository;
    private final CustomerRepository customerRepository;
    private final FarmContext farmContext;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse buildSummary() {
        Long farmId = farmContext.getCurrentFarmId();
        LocalDate today = LocalDate.now();

        DashboardSummaryResponse response = new DashboardSummaryResponse();
        response.setDate(today);
        response.setToday(buildToday(farmId, today));
        response.setThisMonth(buildThisMonth(farmId, today));
        response.setOutstanding(buildOutstanding(farmId));
        response.setCounts(buildCounts(farmId));
        return response;
    }

    // ============ Today ============

    private DashboardSummaryResponse.TodaySection buildToday(Long farmId, LocalDate today) {
        DashboardSummaryResponse.TodaySection s = new DashboardSummaryResponse.TodaySection();
        s.setMilkLiters(milkRepository.sumLitersByFarmAndDate(farmId, today));
        s.setEggs(eggRepository.sumEggsByFarmAndDate(farmId, today));
        s.setSalesRevenue(safe(saleRepository.sumRevenueByFarmAndDate(farmId, today)));
        s.setCashCollected(safe(paymentRepository.sumAmountByFarmAndDate(farmId, today)));
        s.setExpenses(safe(expenseRepository.sumAmountByFarmAndDate(farmId, today)));
        return s;
    }

    // ============ This Month ============

    private DashboardSummaryResponse.MonthSection buildThisMonth(Long farmId, LocalDate today) {
        LocalDate from = today.withDayOfMonth(1);
        LocalDate to = today.withDayOfMonth(today.lengthOfMonth());

        BigDecimal grossSales = safe(saleRepository.sumRevenueByFarmBetween(farmId, from, to));
        BigDecimal cashCollected = safe(paymentRepository.sumAmountByFarmBetween(farmId, from, to));
        BigDecimal expenses = safe(expenseRepository.sumAmountByFarmBetween(farmId, from, to));
        BigDecimal netProfit = grossSales.subtract(expenses);

        BigDecimal margin;
        if (grossSales.signum() == 0) {
            margin = BigDecimal.ZERO;
        } else {
            margin = netProfit.multiply(BigDecimal.valueOf(100))
                    .divide(grossSales, 2, RoundingMode.HALF_UP);
        }

        DashboardSummaryResponse.MonthSection s = new DashboardSummaryResponse.MonthSection();
        s.setGrossSales(grossSales);
        s.setCashCollected(cashCollected);
        s.setExpenses(expenses);
        s.setNetProfit(netProfit);
        s.setProfitMargin(margin);
        return s;
    }

    // ============ Outstanding ============

    private DashboardSummaryResponse.OutstandingSection buildOutstanding(Long farmId) {
        DashboardSummaryResponse.OutstandingSection s = new DashboardSummaryResponse.OutstandingSection();
        s.setTotalDues(safe(saleRepository.sumAllDuesByFarm(farmId)));
        s.setCustomersWithDues(saleRepository.countCustomersWithDues(farmId));
        return s;
    }

    // ============ Counts ============

    private DashboardSummaryResponse.CountsSection buildCounts(Long farmId) {
        DashboardSummaryResponse.CountsSection s = new DashboardSummaryResponse.CountsSection();
        s.setTotalAnimals(animalRepository.countByFarmId(farmId));
        s.setCows(animalRepository.countByFarmIdAndSpecies(farmId, Animal.Species.COW));
        s.setGoats(animalRepository.countByFarmIdAndSpecies(farmId, Animal.Species.GOAT));
        s.setSheep(animalRepository.countByFarmIdAndSpecies(farmId, Animal.Species.SHEEP));
        s.setChickens(animalRepository.countByFarmIdAndSpecies(farmId, Animal.Species.CHICKEN));
        s.setDucks(animalRepository.countByFarmIdAndSpecies(farmId, Animal.Species.DUCK));
        s.setBuffaloes(animalRepository.countByFarmIdAndSpecies(farmId, Animal.Species.BUFFALO));
        s.setActiveFlocks(flockRepository.countByFarmIdAndStatus(farmId, Flock.FlockStatus.ACTIVE));
        s.setTotalCustomers(customerRepository.countByFarmId(farmId));
        return s;
    }

    // ============ Helpers ============

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }
}