package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.request.SaleRequest;
import tc.impact.farmmanagement.dto.response.DailyRevenueSummaryResponse;
import tc.impact.farmmanagement.dto.response.ProductRevenueResponse;
import tc.impact.farmmanagement.dto.response.SaleResponse;
import tc.impact.farmmanagement.entity.Customer;
import tc.impact.farmmanagement.entity.Payment;
import tc.impact.farmmanagement.entity.Sale;
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
import tc.impact.farmmanagement.repository.CustomerRepository;
import tc.impact.farmmanagement.repository.PaymentRepository;
import tc.impact.farmmanagement.repository.SaleRepository;

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
    private final PaymentRepository paymentRepository;

    // ============ Create ============

    @Transactional
    public SaleResponse create(SaleRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with id: " + request.getCustomerId()));

        Sale sale = new Sale();
        sale.setCustomer(customer);
        sale.setDate(request.getDate());
        sale.setProductType(request.getProductType());
        sale.setUnit(request.getUnit());
        sale.setQuantity(request.getQuantity());
        sale.setUnitPrice(request.getUnitPrice());
        sale.setNotes(request.getNotes());

        // Total is calculated in the entity's @PrePersist
        // But we need it now to validate initial payment
        BigDecimal total = request.getQuantity()
                .multiply(request.getUnitPrice())
                .setScale(2, RoundingMode.HALF_UP);
        sale.setTotalAmount(total);

        // Validate + apply initial payment
        BigDecimal initialPayment = request.getInitialPayment();
        if (initialPayment != null && initialPayment.signum() > 0) {
            if (initialPayment.compareTo(total) > 0) {
                throw new BusinessException(
                        "Initial payment (" + initialPayment + ") cannot exceed total amount (" + total + ")");
            }
            sale.setPaidAmount(initialPayment);
        } else {
            sale.setPaidAmount(BigDecimal.ZERO);
        }

        sale.setDueAmount(total.subtract(sale.getPaidAmount()));

        Sale saved = saleRepository.save(sale);

        // If there was an initial payment, persist a Payment row for audit trail
        if (initialPayment != null && initialPayment.signum() > 0) {
            Payment payment = new Payment();
            payment.setSale(saved);
            payment.setAmount(initialPayment);
            payment.setDate(request.getDate());
            payment.setMethod(request.getInitialPaymentMethod() != null
                    ? request.getInitialPaymentMethod()
                    : Payment.PaymentMethod.CASH);
            payment.setNotes("Initial payment at sale creation");
            paymentRepository.save(payment);
        }

        return toResponse(saved);
    }

    // ============ Read ============

    @Transactional(readOnly = true)
    public List<SaleResponse> getAll() {
        return saleRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SaleResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public List<SaleResponse> getByCustomer(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }
        return saleRepository.findByCustomerId(customerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> getByDate(LocalDate date) {
        return saleRepository.findByDate(date).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> getByRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException("'from' date cannot be after 'to' date");
        }
        return saleRepository.findByDateBetweenOrderByDateAsc(from, to).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> getByProductType(Sale.ProductType productType) {
        return saleRepository.findByProductType(productType).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ============ Dues ============

    @Transactional(readOnly = true)
    public List<SaleResponse> getAllWithDues() {
        return saleRepository.findAllWithDues().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> getDuesByCustomer(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }
        return saleRepository.findDuesByCustomer(customerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ============ Update ============

    @Transactional
    public SaleResponse update(Long id, SaleRequest request) {
        Sale sale = findOrThrow(id);

        // Customer cannot be changed on update (keeps dues history clean)
        if (!sale.getCustomer().getId().equals(request.getCustomerId())) {
            throw new BusinessException(
                    "Customer cannot be changed on an existing sale. Delete and recreate if needed.");
        }

        // Recalculate totals with new quantity/price
        BigDecimal newTotal = request.getQuantity()
                .multiply(request.getUnitPrice())
                .setScale(2, RoundingMode.HALF_UP);

        // If new total is less than what's already been paid, block the update
        if (newTotal.compareTo(sale.getPaidAmount()) < 0) {
            throw new BusinessException(
                    "Cannot reduce total amount below already-paid amount (" + sale.getPaidAmount() + "). " +
                            "Current paid: " + sale.getPaidAmount() + ", new total would be: " + newTotal);
        }

        sale.setDate(request.getDate());
        sale.setProductType(request.getProductType());
        sale.setUnit(request.getUnit());
        sale.setQuantity(request.getQuantity());
        sale.setUnitPrice(request.getUnitPrice());
        sale.setNotes(request.getNotes());

        sale.calculateAmounts(); // recompute total & due from quantity/price
        // @PreUpdate recalculates again on save, which is fine (idempotent)

        return toResponse(saleRepository.save(sale));
    }

    // ============ Delete ============

    @Transactional
    public void delete(Long id) {
        Sale sale = findOrThrow(id);

        if (sale.getPaidAmount().signum() > 0) {
            throw new BusinessException(
                    "Cannot delete a sale that has payments recorded. " +
                            "Delete the payments first, then delete the sale.");
        }

        // Cascade: delete all payment rows for this sale (they should be 0 anyway)
        List<Payment> payments = paymentRepository.findBySaleId(id);
        if (!payments.isEmpty()) {
            paymentRepository.deleteAll(payments);
        }

        saleRepository.deleteById(id);
    }

    // ============ Summaries ============

    public BigDecimal getDailyRevenue(LocalDate date) {
        return saleRepository.sumRevenueByDate(date);
    }

    public BigDecimal getRevenueBetween(LocalDate from, LocalDate to) {
        return saleRepository.sumRevenueBetween(from, to);
    }

    public List<DailyRevenueSummaryResponse> getDailyRevenueRange(LocalDate from, LocalDate to) {
        return saleRepository.dailyRevenueBetween(from, to).stream()
                .map(row -> new DailyRevenueSummaryResponse(
                        (LocalDate) row[0],
                        ((BigDecimal) row[1]).setScale(2, RoundingMode.HALF_UP)))
                .collect(Collectors.toList());
    }

    public List<ProductRevenueResponse> getRevenueByProduct(LocalDate from, LocalDate to) {
        return saleRepository.sumRevenueByProductTypeBetween(from, to).stream()
                .map(row -> new ProductRevenueResponse(
                        (Sale.ProductType) row[0],
                        ((BigDecimal) row[1]).setScale(2, RoundingMode.HALF_UP)))
                .collect(Collectors.toList());
    }

    public BigDecimal getAllDues() {
        return saleRepository.sumAllDues();
    }

    // ============ Helpers ============

    private Sale findOrThrow(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with id: " + id));
    }

    SaleResponse toResponse(Sale sale) {
        SaleResponse response = new SaleResponse();
        response.setId(sale.getId());
        response.setCustomerId(sale.getCustomer().getId());
        response.setCustomerName(sale.getCustomer().getName());
        response.setCustomerPhone(sale.getCustomer().getPhone());
        response.setDate(sale.getDate());
        response.setProductType(sale.getProductType());
        response.setUnit(sale.getUnit());
        response.setQuantity(sale.getQuantity());
        response.setUnitPrice(sale.getUnitPrice());
        response.setTotalAmount(sale.getTotalAmount());
        response.setPaidAmount(sale.getPaidAmount());
        response.setDueAmount(sale.getDueAmount());
        response.setNotes(sale.getNotes());
        response.setCreatedAt(sale.getCreatedAt());
        return response;
    }
}