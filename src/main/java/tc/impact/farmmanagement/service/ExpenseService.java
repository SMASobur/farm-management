package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.request.ExpenseRequest;
import tc.impact.farmmanagement.dto.response.CategoryExpenseSummaryResponse;
import tc.impact.farmmanagement.dto.response.DailyExpenseSummaryResponse;
import tc.impact.farmmanagement.dto.response.ExpenseResponse;
import tc.impact.farmmanagement.entity.Expense;
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
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

    // ============ Create ============

    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        Expense expense = new Expense();
        expense.setDate(request.getDate() != null ? request.getDate() : LocalDate.now());
        expense.setCategory(request.getCategory());
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setNotes(request.getNotes());

        return toResponse(expenseRepository.save(expense));
    }

    // ============ Read ============

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getAll() {
        return expenseRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getByDate(LocalDate date) {
        return expenseRepository.findByDate(date).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getByRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException("'from' date cannot be after 'to' date");
        }
        return expenseRepository.findByDateBetweenOrderByDateAsc(from, to).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getByCategory(Expense.ExpenseCategory category) {
        return expenseRepository.findByCategory(category).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getByCategoryAndRange(
            Expense.ExpenseCategory category, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException("'from' date cannot be after 'to' date");
        }
        return expenseRepository.findByCategoryAndDateBetweenOrderByDateAsc(category, from, to).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ============ Update ============

    @Transactional
    public ExpenseResponse update(Long id, ExpenseRequest request) {
        Expense expense = findOrThrow(id);

        expense.setDate(request.getDate() != null ? request.getDate() : expense.getDate());
        expense.setCategory(request.getCategory());
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setNotes(request.getNotes());

        return toResponse(expenseRepository.save(expense));
    }

    // ============ Delete ============

    @Transactional
    public void delete(Long id) {
        if (!expenseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Expense not found with id: " + id);
        }
        expenseRepository.deleteById(id);
    }

    // ============ Summaries ============

    @Transactional(readOnly = true)
    public BigDecimal getDailyTotal(LocalDate date) {
        return expenseRepository.sumAmountByDate(date);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalBetween(LocalDate from, LocalDate to) {
        return expenseRepository.sumAmountBetween(from, to);
    }

    @Transactional(readOnly = true)
    public List<DailyExpenseSummaryResponse> getDailySummaries(LocalDate from, LocalDate to) {
        return expenseRepository.dailyTotalsBetween(from, to).stream()
                .map(row -> new DailyExpenseSummaryResponse(
                        (LocalDate) row[0],
                        ((BigDecimal) row[1]).setScale(2, RoundingMode.HALF_UP)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryExpenseSummaryResponse> getCategorySummaries(LocalDate from, LocalDate to) {
        return expenseRepository.sumByCategoryBetween(from, to).stream()
                .map(row -> new CategoryExpenseSummaryResponse(
                        (Expense.ExpenseCategory) row[0],
                        ((BigDecimal) row[1]).setScale(2, RoundingMode.HALF_UP)))
                .collect(Collectors.toList());
    }

    // ============ Helpers ============

    private Expense findOrThrow(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
    }

    private ExpenseResponse toResponse(Expense e) {
        ExpenseResponse response = new ExpenseResponse();
        response.setId(e.getId());
        response.setDate(e.getDate());
        response.setCategory(e.getCategory());
        response.setDescription(e.getDescription());
        response.setAmount(e.getAmount());
        response.setNotes(e.getNotes());
        response.setCreatedAt(e.getCreatedAt());
        return response;
    }
}