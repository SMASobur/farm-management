package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.request.ExpenseRequest;
import tc.impact.farmmanagement.dto.response.CategoryExpenseSummaryResponse;
import tc.impact.farmmanagement.dto.response.DailyExpenseSummaryResponse;
import tc.impact.farmmanagement.dto.response.ExpenseResponse;
import tc.impact.farmmanagement.entity.Category;
import tc.impact.farmmanagement.entity.Expense;
import tc.impact.farmmanagement.entity.Farm;
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
import tc.impact.farmmanagement.repository.CategoryRepository;
import tc.impact.farmmanagement.repository.ExpenseRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final FarmContext farmContext;

    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        Farm farm = farmContext.getCurrentFarm();
        Category category = loadCategory(request.getCategoryId(), farm.getId());

        Expense expense = new Expense();
        expense.setFarm(farm);
        expense.setDate(request.getDate() != null ? request.getDate() : LocalDate.now());
        expense.setCategory(category);
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setNotes(request.getNotes());

        return toResponse(expenseRepository.save(expense));
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getAll() {
        Long farmId = farmContext.getCurrentFarmId();
        return expenseRepository.findByFarmId(farmId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getByDate(LocalDate date) {
        Long farmId = farmContext.getCurrentFarmId();
        return expenseRepository.findByFarmIdAndDate(farmId, date).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getByRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) throw new BusinessException("'from' date cannot be after 'to' date");
        Long farmId = farmContext.getCurrentFarmId();
        return expenseRepository.findByFarmIdAndDateBetweenOrderByDateAsc(farmId, from, to).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getByCategory(Long categoryId) {
        Long farmId = farmContext.getCurrentFarmId();
        return expenseRepository.findByFarmIdAndCategoryId(farmId, categoryId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getByCategoryAndRange(Long categoryId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) throw new BusinessException("'from' date cannot be after 'to' date");
        Long farmId = farmContext.getCurrentFarmId();
        return expenseRepository.findByFarmIdAndCategoryIdAndDateBetweenOrderByDateAsc(farmId, categoryId, from, to).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ExpenseResponse update(Long id, ExpenseRequest request) {
        Long farmId = farmContext.getCurrentFarmId();
        Expense expense = findOrThrow(id);
        Category category = loadCategory(request.getCategoryId(), farmId);

        expense.setDate(request.getDate() != null ? request.getDate() : expense.getDate());
        expense.setCategory(category);
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setNotes(request.getNotes());

        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void delete(Long id) {
        Expense expense = findOrThrow(id);
        expenseRepository.deleteById(expense.getId());
    }

    @Transactional(readOnly = true)
    public BigDecimal getDailyTotal(LocalDate date) {
        return expenseRepository.sumAmountByFarmAndDate(farmContext.getCurrentFarmId(), date);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalBetween(LocalDate from, LocalDate to) {
        return expenseRepository.sumAmountByFarmBetween(farmContext.getCurrentFarmId(), from, to);
    }

    @Transactional(readOnly = true)
    public List<DailyExpenseSummaryResponse> getDailySummaries(LocalDate from, LocalDate to) {
        return expenseRepository.dailyTotalsBetween(farmContext.getCurrentFarmId(), from, to).stream()
                .map(row -> new DailyExpenseSummaryResponse(
                        (LocalDate) row[0],
                        ((BigDecimal) row[1]).setScale(2, RoundingMode.HALF_UP)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryExpenseSummaryResponse> getCategorySummaries(LocalDate from, LocalDate to) {
        return expenseRepository.sumAmountByCategoryCodeBetween(farmContext.getCurrentFarmId(), from, to).stream()
                .map(row -> new CategoryExpenseSummaryResponse(
                        (String) row[0],
                        ((BigDecimal) row[1]).setScale(2, RoundingMode.HALF_UP)))
                .collect(Collectors.toList());
    }

    private Category loadCategory(Long categoryId, Long farmId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
        if (!category.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Category not found with id: " + categoryId);
        }
        if (!category.isActive()) {
            throw new BusinessException("Category '" + category.getName() + "' is inactive");
        }
        if (category.getType() != Category.CategoryType.EXPENSE) {
            throw new BusinessException("Category '" + category.getName() + "' is not an expense category");
        }
        return category;
    }

    private Expense findOrThrow(Long id) {
        Long farmId = farmContext.getCurrentFarmId();
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
        if (!expense.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Expense not found with id: " + id);
        }
        return expense;
    }

    private ExpenseResponse toResponse(Expense e) {
        ExpenseResponse response = new ExpenseResponse();
        response.setId(e.getId());
        response.setDate(e.getDate());
        response.setCategoryId(e.getCategory().getId());
        response.setCategoryCode(e.getCategory().getCode());
        response.setCategoryName(e.getCategory().getName());
        response.setCategoryNameBn(e.getCategory().getNameBn());
        response.setDescription(e.getDescription());
        response.setAmount(e.getAmount());
        response.setNotes(e.getNotes());
        response.setCreatedAt(e.getCreatedAt());
        return response;
    }
}