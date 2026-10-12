package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.request.SaleRequest;
import tc.impact.farmmanagement.dto.response.DailyRevenueSummaryResponse;
import tc.impact.farmmanagement.dto.response.ProductRevenueResponse;
import tc.impact.farmmanagement.dto.response.SaleResponse;
import tc.impact.farmmanagement.entity.*;
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
import tc.impact.farmmanagement.repository.*;
import tc.impact.farmmanagement.util.UserDisplayUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final CustomerRepository customerRepository;
    private final CategoryRepository categoryRepository;
    private final PaymentRepository paymentRepository;
    private final FarmContext farmContext;
    private final UnitRepository unitRepository;

    // ============ Create ============

    @Transactional
    public SaleResponse create(SaleRequest request) {
        Farm farm = farmContext.getCurrentFarm();

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        if (!customer.getFarm().getId().equals(farm.getId())) {
            throw new ResourceNotFoundException("Customer not found");
        }

        Category category = loadProductCategory(request.getCategoryId(), farm.getId());
        Unit unit = resolveUnit(request, farm.getId());

        // Handle "Save as category" toggle
        String customProductName = null;
        if (isOtherCategory(category) && hasText(request.getCustomProductName())) {
            if (Boolean.TRUE.equals(request.getSaveCustomProductAsCategory())) {
                Unit defaultUnit = resolveDefaultUnitForCategory(request, farm.getId());
                category = findOrCreateCategoryForFarm(
                        request.getCustomProductName().trim(),
                        request.getCustomProductNameBn() != null
                                ? request.getCustomProductNameBn().trim()
                                : null,
                        defaultUnit,
                        farm);
            } else {
                customProductName = request.getCustomProductName().trim();
            }
        }

        Sale sale = new Sale();
        sale.setFarm(farm);
        sale.setCustomer(customer);
        sale.setDate(request.getDate());
        sale.setCategory(category);
        sale.setUnit(unit);
        sale.setQuantity(request.getQuantity());
        sale.setUnitPrice(request.getUnitPrice());
        sale.setNotes(request.getNotes());
        sale.setCustomProductName(customProductName);
        sale.setEnteredBy(farmContext.getCurrentUser());

        BigDecimal total = request.getQuantity().multiply(request.getUnitPrice())
                .setScale(2, RoundingMode.HALF_UP);
        sale.setTotalAmount(total);

        BigDecimal initialPayment = request.getInitialPayment();
        if (initialPayment != null && initialPayment.signum() > 0) {
            if (initialPayment.compareTo(total) > 0) {
                throw new BusinessException("Initial payment cannot exceed total amount");
            }
            sale.setPaidAmount(initialPayment);
        } else {
            sale.setPaidAmount(BigDecimal.ZERO);
        }
        sale.setDueAmount(total.subtract(sale.getPaidAmount()));

        Sale saved = saleRepository.save(sale);

        if (initialPayment != null && initialPayment.signum() > 0) {
            Payment payment = new Payment();
            payment.setFarm(farm);
            payment.setSale(saved);
            payment.setAmount(initialPayment);
            payment.setDate(request.getDate());
            payment.setMethod(request.getInitialPaymentMethod() != null
                    ? request.getInitialPaymentMethod() : Payment.PaymentMethod.CASH);
            payment.setNotes("Initial payment at sale creation");
            paymentRepository.save(payment);
        }

        return toResponse(saved);
    }

    // ============ Read ============

    @Transactional(readOnly = true)
    public List<SaleResponse> getAll() {
        Long farmId = farmContext.getCurrentFarmId();
        return saleRepository.findByFarmId(farmId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SaleResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> getByCustomer(Long customerId) {
        Long farmId = farmContext.getCurrentFarmId();
        return saleRepository.findByFarmIdAndCustomerId(farmId, customerId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> getByDate(LocalDate date) {
        Long farmId = farmContext.getCurrentFarmId();
        return saleRepository.findByFarmIdAndDate(farmId, date).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> getByRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) throw new BusinessException("'from' date cannot be after 'to' date");
        Long farmId = farmContext.getCurrentFarmId();
        return saleRepository.findByFarmIdAndDateBetweenOrderByDateAsc(farmId, from, to).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> getByCategory(Long categoryId) {
        Long farmId = farmContext.getCurrentFarmId();
        return saleRepository.findByFarmIdAndCategoryId(farmId, categoryId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> getFiltered(
            LocalDate from, LocalDate to, Long customerId, Long categoryId) {
        Long farmId = farmContext.getCurrentFarmId();
        List<Sale> sales;

        if (customerId != null && from != null && to != null) {
            sales = saleRepository.findByFarmIdAndCustomerIdAndDateBetweenOrderByDateDescIdDesc(
                    farmId, customerId, from, to);
        } else if (categoryId != null && from != null && to != null) {
            sales = saleRepository.findByFarmIdAndCategoryIdAndDateBetweenOrderByDateDescIdDesc(
                    farmId, categoryId, from, to);
        } else if (customerId != null) {
            sales = saleRepository.findByFarmIdAndCustomerIdOrderByDateDescIdDesc(farmId, customerId);
        } else if (categoryId != null) {
            sales = saleRepository.findByFarmIdAndCategoryIdOrderByDateDescIdDesc(farmId, categoryId);
        } else if (from != null && to != null) {
            sales = saleRepository.findByFarmIdAndDateBetweenOrderByDateDescIdDesc(farmId, from, to);
        } else {
            sales = saleRepository.findByFarmIdOrderByDateDescIdDesc(farmId);
        }

        return sales.stream()
                .filter(s -> customerId == null || s.getCustomer().getId().equals(customerId))
                .filter(s -> categoryId == null || s.getCategory().getId().equals(categoryId))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> getAllWithDues() {
        return saleRepository.findDuesByFarm(farmContext.getCurrentFarmId()).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> getDuesByCustomer(Long customerId) {
        Long farmId = farmContext.getCurrentFarmId();
        return saleRepository.findDuesByFarmAndCustomer(farmId, customerId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    // ============ Update ============

    @Transactional
    public SaleResponse update(Long id, SaleRequest request) {
        Sale sale = findOrThrow(id);
        Long farmId = farmContext.getCurrentFarmId();

        if (!sale.getCustomer().getId().equals(request.getCustomerId())) {
            throw new BusinessException("Customer cannot be changed on an existing sale.");
        }

        Category category = loadProductCategory(request.getCategoryId(), farmId);
        Unit unit = resolveUnit(request, farmId);

        String customProductName = null;
        if (isOtherCategory(category) && hasText(request.getCustomProductName())) {
            if (Boolean.TRUE.equals(request.getSaveCustomProductAsCategory())) {
                Unit defaultUnit = resolveDefaultUnitForCategory(request, farmId);
                category = findOrCreateCategoryForFarm(
                        request.getCustomProductName().trim(),
                        request.getCustomProductNameBn() != null
                                ? request.getCustomProductNameBn().trim()
                                : null,
                        defaultUnit,
                        farmContext.getCurrentFarm());
            } else {
                customProductName = request.getCustomProductName().trim();
            }
        }

        BigDecimal newTotal = request.getQuantity().multiply(request.getUnitPrice())
                .setScale(2, RoundingMode.HALF_UP);
        if (newTotal.compareTo(sale.getPaidAmount()) < 0) {
            throw new BusinessException("Cannot reduce total below already-paid amount");
        }

        sale.setDate(request.getDate());
        sale.setCategory(category);
        sale.setUnit(unit);
        sale.setQuantity(request.getQuantity());
        sale.setUnitPrice(request.getUnitPrice());
        sale.setNotes(request.getNotes());
        sale.setCustomProductName(customProductName);

        sale.calculateAmounts();
        return toResponse(saleRepository.save(sale));
    }

    // ============ Delete ============

    @Transactional
    public void delete(Long id) {
        Sale sale = findOrThrow(id);
        if (sale.getPaidAmount().signum() > 0) {
            throw new BusinessException("Cannot delete a sale with payments. Delete payments first.");
        }
        List<Payment> payments = paymentRepository.findByFarmIdAndSaleId(
                farmContext.getCurrentFarmId(), id);
        if (!payments.isEmpty()) {
            paymentRepository.deleteAll(payments);
        }
        saleRepository.deleteById(id);
    }

    // ============ Summaries ============

    @Transactional(readOnly = true)
    public List<DailyRevenueSummaryResponse> getDailyRevenueRange(LocalDate from, LocalDate to) {
        return saleRepository.dailyRevenueBetween(farmContext.getCurrentFarmId(), from, to).stream()
                .map(row -> new DailyRevenueSummaryResponse(
                        (LocalDate) row[0],
                        ((BigDecimal) row[1]).setScale(2, RoundingMode.HALF_UP)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductRevenueResponse> getRevenueByCategory(LocalDate from, LocalDate to) {
        return saleRepository.sumRevenueByCategoryBetween(farmContext.getCurrentFarmId(), from, to).stream()
                .map(row -> new ProductRevenueResponse(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        ((BigDecimal) row[3]).setScale(2, RoundingMode.HALF_UP)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal getAllDues() {
        return saleRepository.sumAllDuesByFarm(farmContext.getCurrentFarmId());
    }

    // ============ Helpers ============

    private Unit resolveUnit(SaleRequest request, Long farmId) {
        boolean hasUnitId = request.getUnitId() != null;
        boolean hasCustomName = request.getCustomUnitName() != null
                && !request.getCustomUnitName().isBlank();

        if (hasUnitId && hasCustomName) {
            throw new BusinessException("Provide either unitId or customUnitName, not both");
        }
        if (!hasUnitId && !hasCustomName) {
            throw new BusinessException("Unit is required");
        }

        if (hasUnitId) {
            return loadUnit(request.getUnitId(), farmId);
        }

        String name = request.getCustomUnitName().trim();
        String code = normalizeCode(name);
        if (code.isEmpty()) {
            throw new BusinessException("Invalid unit name");
        }
        if (code.length() > 20) {
            code = code.substring(0, 20);
        }

        var existing = unitRepository.findByFarmIdAndCode(farmId, code);
        if (existing.isPresent()) {
            return existing.get();
        }

        boolean saveForReuse = Boolean.TRUE.equals(request.getSaveCustomUnitToUnits());

        Farm farm = farmContext.getCurrentFarm();
        Unit unit = new Unit();
        unit.setFarm(farm);
        unit.setCode(code);
        unit.setName(name);
        unit.setNameBn(
                request.getCustomUnitNameBn() != null
                        ? request.getCustomUnitNameBn().trim()
                        : null);
        unit.setAbbreviation(
                request.getCustomUnitAbbreviation() != null
                        ? request.getCustomUnitAbbreviation().trim()
                        : null);
        unit.setActive(saveForReuse);
        unit.setSystemDefault(false);
        unit.setDisplayOrder(900);
        return unitRepository.save(unit);
    }

    /**
     * Resolves the default unit to attach to a new custom category.
     * Returns null if not provided.
     */
    private Unit resolveDefaultUnitForCategory(SaleRequest request, Long farmId) {
        if (request.getCustomProductDefaultUnitId() == null) {
            return null;
        }
        return loadUnit(request.getCustomProductDefaultUnitId(), farmId);
    }

    private Category findOrCreateCategoryForFarm(String name, String nameBn, Unit defaultUnit, Farm farm) {
        String code = normalizeCode(name);
        if (code.isEmpty()) {
            throw new BusinessException("Invalid category name");
        }
        if (code.length() > 50) {
            code = code.substring(0, 50);
        }

        var existing = categoryRepository.findByFarmIdAndCodeAndType(
                farm.getId(), code, Category.CategoryType.PRODUCT);
        if (existing.isPresent()) {
            return existing.get();
        }

        Category category = new Category();
        category.setFarm(farm);
        category.setCode(code);
        category.setName(name);
        category.setNameBn(nameBn);
        category.setType(Category.CategoryType.PRODUCT);
        category.setActive(true);
        category.setSystemDefault(false);
        category.setDisplayOrder(900);
        category.setDefaultUnit(defaultUnit);   // ← attach the default unit
        return categoryRepository.save(category);
    }

    private Unit loadUnit(Long unitId, Long farmId) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with id: " + unitId));
        if (!unit.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Unit not found");
        }
        if (!unit.isActive()) {
            throw new BusinessException("Unit '" + unit.getName() + "' is inactive");
        }
        return unit;
    }

    private Category loadProductCategory(Long categoryId, Long farmId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
        if (!category.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Category not found with id: " + categoryId);
        }
        if (!category.isActive()) {
            throw new BusinessException("Category '" + category.getName() + "' is inactive");
        }
        if (category.getType() != Category.CategoryType.PRODUCT) {
            throw new BusinessException("Category '" + category.getName() + "' is not a product category");
        }
        return category;
    }

    private Sale findOrThrow(Long id) {
        Long farmId = farmContext.getCurrentFarmId();
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with id: " + id));
        if (!sale.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Sale not found with id: " + id);
        }
        return sale;
    }

    private boolean isOtherCategory(Category category) {
        return "OTHER".equalsIgnoreCase(category.getCode());
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private String normalizeCode(String name) {
        return name.trim()
                .toUpperCase()
                .replaceAll("[^\\p{L}\\p{N}]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    SaleResponse toResponse(Sale sale) {
        SaleResponse response = new SaleResponse();
        response.setId(sale.getId());
        response.setCustomerId(sale.getCustomer().getId());
        response.setCustomerName(sale.getCustomer().getName());
        response.setCustomerPhone(sale.getCustomer().getPhone());
        response.setDate(sale.getDate());
        response.setCategoryId(sale.getCategory().getId());
        response.setCategoryCode(sale.getCategory().getCode());
        response.setCategoryName(sale.getCategory().getName());
        response.setCategoryNameBn(sale.getCategory().getNameBn());
        response.setUnitId(sale.getUnit().getId());
        response.setUnitCode(sale.getUnit().getCode());
        response.setUnitName(sale.getUnit().getName());
        response.setUnitAbbreviation(sale.getUnit().getAbbreviation());
        response.setQuantity(sale.getQuantity());
        response.setUnitPrice(sale.getUnitPrice());
        response.setTotalAmount(sale.getTotalAmount());
        response.setPaidAmount(sale.getPaidAmount());
        response.setDueAmount(sale.getDueAmount());
        response.setCustomProductName(sale.getCustomProductName());
        response.setNotes(sale.getNotes());
        response.setCreatedAt(sale.getCreatedAt());
        if (sale.getEnteredBy() != null) {
            response.setEnteredByUserId(sale.getEnteredBy().getId());
            response.setEnteredByName(UserDisplayUtil.displayName(sale.getEnteredBy()));
        }
        return response;
    }
}