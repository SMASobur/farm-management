package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.response.DashboardAlert;
import tc.impact.farmmanagement.dto.response.DashboardAlertsResponse;
import tc.impact.farmmanagement.entity.Animal;
import tc.impact.farmmanagement.entity.Expense;
import tc.impact.farmmanagement.entity.Sale;
import tc.impact.farmmanagement.entity.Worker;
import tc.impact.farmmanagement.repository.AnimalRepository;
import tc.impact.farmmanagement.repository.ExpenseRepository;
import tc.impact.farmmanagement.repository.MilkProductionRepository;
import tc.impact.farmmanagement.repository.SaleRepository;
import tc.impact.farmmanagement.repository.WorkerRepository;
import tc.impact.farmmanagement.entity.Flock;
import tc.impact.farmmanagement.repository.EggProductionRepository;
import tc.impact.farmmanagement.repository.FlockRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AnimalRepository animalRepository;
    private final MilkProductionRepository milkRepository;
    private final SaleRepository saleRepository;
    private final WorkerRepository workerRepository;
    private final ExpenseRepository expenseRepository;
    private final FarmContext farmContext;
    private final EggProductionRepository eggRepository;
    private final FlockRepository flockRepository;

    private static final double LOW_MILK_THRESHOLD = 0.70;
    private static final int OVERDUE_DAYS = 30;
    private static final int SALARY_DUE_DAY_OF_MONTH = 28;
    private static final int EGG_ALERT_MIN_BIRDS = 5;
    private static final double EGG_ALERT_THRESHOLD = 0.70;

    @Transactional(readOnly = true)
    public DashboardAlertsResponse getAlerts() {
        Long farmId = farmContext.getCurrentFarmId();
        List<DashboardAlert> alerts = new ArrayList<>();

        alerts.addAll(getLowMilkProductionAlerts(farmId));
        alerts.addAll(getLowEggProductionAlerts(farmId));
        alerts.addAll(getOverduePaymentAlerts(farmId));
        alerts.addAll(getSalaryDueAlerts(farmId));

        alerts.sort(Comparator.comparingInt(a -> severityRank(a.getSeverity())));

        DashboardAlertsResponse response = new DashboardAlertsResponse();
        response.setAlerts(alerts);
        response.setTotalCount(alerts.size());
        return response;
    }

    // ============================================================
    // Alert 1: Low Milk Production
    // ============================================================

    private List<DashboardAlert> getLowMilkProductionAlerts(Long farmId) {
        List<DashboardAlert> alerts = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate sevenDaysAgo = today.minusDays(7);

        // Only active milk-producing animals
        List<Animal> milkAnimals = animalRepository.findByFarmId(farmId).stream()
                .filter(Animal::isMilkProducer)
                .filter(Animal::isActive)
                .collect(Collectors.toList());

        for (Animal animal : milkAnimals) {
            Double todayTotal = milkRepository.sumLitersByAnimalAndDate(animal.getId(), today);
            if (todayTotal == null || todayTotal == 0) continue;

            Double weekSum = milkRepository.sumLitersByAnimalBetween(
                    animal.getId(), sevenDaysAgo, yesterday);
            if (weekSum == null || weekSum == 0) continue;

            long daysWithData = milkRepository
                    .findByAnimalIdAndDateBetweenOrderByDateAsc(animal.getId(), sevenDaysAgo, yesterday)
                    .size();
            if (daysWithData == 0) continue;

            double dailyAvg = weekSum / daysWithData;
            double ratio = todayTotal / dailyAvg;

            if (ratio < LOW_MILK_THRESHOLD) {
                DashboardAlert alert = new DashboardAlert();
                alert.setType("LOW_MILK_PRODUCTION");
                alert.setSeverity("WARNING");
                alert.setTitle(animal.getTagNumber() + " — milk drop");
                alert.setMessage(String.format(
                        "Produced %.1fL today vs %.1fL 7-day average (%.0f%%)",
                        todayTotal, dailyAvg, ratio * 100));
                alert.setValue(BigDecimal.valueOf(ratio * 100).setScale(1, RoundingMode.HALF_UP));
                alert.setLink("/animals/" + animal.getId());
                alerts.add(alert);
            }
        }
        return alerts;
    }

    // ============================================================
    // Alert 2: Overdue Payments
    // ============================================================

    private List<DashboardAlert> getOverduePaymentAlerts(Long farmId) {
        List<DashboardAlert> alerts = new ArrayList<>();
        LocalDate cutoff = LocalDate.now().minusDays(OVERDUE_DAYS);

        List<Sale> overdueSales = saleRepository.findByFarmIdAndDateBetweenOrderByDateAsc(
                farmId, LocalDate.of(2000, 1, 1), cutoff);

        Map<Long, List<Sale>> byCustomer = overdueSales.stream()
                .filter(s -> s.getDueAmount() != null && s.getDueAmount().signum() > 0)
                .collect(Collectors.groupingBy(s -> s.getCustomer().getId()));

        for (Map.Entry<Long, List<Sale>> entry : byCustomer.entrySet()) {
            List<Sale> sales = entry.getValue();
            BigDecimal totalDue = sales.stream()
                    .map(Sale::getDueAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Sale firstSale = sales.stream()
                    .min(Comparator.comparing(Sale::getDate))
                    .orElse(null);
            if (firstSale == null) continue;

            long daysOld = ChronoUnit.DAYS.between(firstSale.getDate(), LocalDate.now());

            DashboardAlert alert = new DashboardAlert();
            alert.setType("OVERDUE_PAYMENT");
            alert.setSeverity("DANGER");
            alert.setTitle(firstSale.getCustomer().getName() + " — overdue");
            alert.setMessage(String.format(
                    "৳%.0f due for %d sale(s), oldest from %s (%d days)",
                    totalDue, sales.size(), firstSale.getDate(), daysOld));
            alert.setValue(totalDue);
            alert.setLink("/customers/" + entry.getKey());
            alerts.add(alert);
        }
        return alerts;
    }

    // ============================================================
    // Alert 3: Salary Due
    // ============================================================

    private List<DashboardAlert> getSalaryDueAlerts(Long farmId) {
        List<DashboardAlert> alerts = new ArrayList<>();
        LocalDate today = LocalDate.now();

        if (today.getDayOfMonth() < SALARY_DUE_DAY_OF_MONTH) {
            return alerts;
        }

        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        List<Worker> activeWorkers = workerRepository.findByFarmIdAndStatus(
                farmId, Worker.WorkerStatus.ACTIVE);

        for (Worker worker : activeWorkers) {
            if (worker.getMonthlySalary() == null) continue;

            List<Expense> salaryPayments = expenseRepository.findByFarmIdAndWorkerIdOrderByDateDesc(
                    farmId, worker.getId());
            BigDecimal paid = salaryPayments.stream()
                    .filter(e -> !e.getDate().isBefore(monthStart) && !e.getDate().isAfter(monthEnd))
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal pending = worker.getMonthlySalary().subtract(paid);
            if (pending.signum() <= 0) continue;

            DashboardAlert alert = new DashboardAlert();
            alert.setType("SALARY_DUE");
            alert.setSeverity("WARNING");
            alert.setTitle(worker.getName() + " — salary pending");
            alert.setMessage(String.format(
                    "৳%.0f remaining for %s (paid ৳%.0f of ৳%.0f)",
                    pending,
                    today.getMonth().toString(),
                    paid,
                    worker.getMonthlySalary()));
            alert.setValue(pending);
            alert.setLink("/workers/" + worker.getId());
            alerts.add(alert);
        }
        return alerts;
    }

// ============================================================
// Alert 4: Low Egg Production (Batches)
// ============================================================

    private List<DashboardAlert> getLowEggProductionAlerts(Long farmId) {
        List<DashboardAlert> alerts = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate sevenDaysAgo = today.minusDays(7);

        // --- Batch-level alerts ---
        List<Flock> activeFlocks = flockRepository.findByFarmIdAndStatus(
                farmId, Flock.FlockStatus.ACTIVE);

        for (Flock flock : activeFlocks) {
            if (flock.getBirdCount() < EGG_ALERT_MIN_BIRDS) continue;

            Long todayTotal = eggRepository.sumTotalEggsByFlockAndDate(flock.getId(), today);
            if (todayTotal == null || todayTotal == 0) continue;

            Long weekSum = eggRepository.sumTotalEggsByFlockBetween(
                    flock.getId(), sevenDaysAgo, yesterday);
            if (weekSum == null || weekSum == 0) continue;

            long daysWithData = eggRepository
                    .findByFlockIdAndDateBetweenOrderByDateAsc(flock.getId(), sevenDaysAgo, yesterday)
                    .size();
            if (daysWithData == 0) continue;

            double dailyAvg = (double) weekSum / daysWithData;
            double ratio = (double) todayTotal / dailyAvg;

            if (ratio < EGG_ALERT_THRESHOLD) {
                DashboardAlert alert = new DashboardAlert();
                alert.setType("LOW_EGG_PRODUCTION");
                alert.setSeverity("WARNING");
                alert.setTitle(flock.getFlockCode() + " — egg drop");
                alert.setMessage(String.format(
                        "%d eggs today vs %.0f 7-day avg (%.0f%%) across %d birds",
                        todayTotal.intValue(), dailyAvg, ratio * 100, flock.getBirdCount()));
                alert.setValue(BigDecimal.valueOf(ratio * 100).setScale(1, RoundingMode.HALF_UP));
                alert.setLink("/flocks/" + flock.getId());
                alerts.add(alert);
            }
        }

        // --- Individual bird alerts ---
        List<Animal> eggAnimals = animalRepository.findByFarmId(farmId).stream()
                .filter(Animal::isEggProducer)
                .filter(Animal::isActive)
                .collect(Collectors.toList());

        for (Animal animal : eggAnimals) {
            Long todayTotal = eggRepository.sumTotalEggsByAnimalAndDate(animal.getId(), today);
            if (todayTotal == null || todayTotal == 0) continue;

            Long weekSum = eggRepository.sumTotalEggsByAnimalBetween(
                    animal.getId(), sevenDaysAgo, yesterday);
            if (weekSum == null || weekSum == 0) continue;

            long daysWithData = eggRepository
                    .findByAnimalIdAndDateBetweenOrderByDateAsc(animal.getId(), sevenDaysAgo, yesterday)
                    .size();
            if (daysWithData == 0) continue;

            double dailyAvg = (double) weekSum / daysWithData;
            double ratio = (double) todayTotal / dailyAvg;

            if (ratio < EGG_ALERT_THRESHOLD) {
                DashboardAlert alert = new DashboardAlert();
                alert.setType("LOW_EGG_PRODUCTION");
                alert.setSeverity("INFO");
                alert.setTitle(animal.getTagNumber() + " — egg drop");
                alert.setMessage(String.format(
                        "%d egg(s) today vs %.0f 7-day avg (%.0f%%)",
                        todayTotal.intValue(), dailyAvg, ratio * 100));
                alert.setValue(BigDecimal.valueOf(ratio * 100).setScale(1, RoundingMode.HALF_UP));
                alert.setLink("/animals/" + animal.getId());
                alerts.add(alert);
            }
        }

        return alerts;
    }

    // ============================================================
    // Helpers
    // ============================================================

    private int severityRank(String severity) {
        return switch (severity) {
            case "DANGER" -> 0;
            case "WARNING" -> 1;
            case "INFO" -> 2;
            default -> 3;
        };
    }
}