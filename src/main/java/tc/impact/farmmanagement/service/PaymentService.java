package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.request.PaymentRequest;
import tc.impact.farmmanagement.dto.response.PaymentMethodSummaryResponse;
import tc.impact.farmmanagement.dto.response.PaymentResponse;
import tc.impact.farmmanagement.entity.Payment;
import tc.impact.farmmanagement.entity.Sale;
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
import tc.impact.farmmanagement.repository.PaymentRepository;
import tc.impact.farmmanagement.repository.SaleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SaleRepository saleRepository;

    // ============ Create ============

    @Transactional
    public PaymentResponse create(PaymentRequest request) {
        Sale sale = saleRepository.findById(request.getSaleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sale not found with id: " + request.getSaleId()));

        BigDecimal currentPaid = paymentRepository.sumAmountBySale(sale.getId());
        BigDecimal newTotalPaid = currentPaid.add(request.getAmount());

        // Prevent overpayment
        if (newTotalPaid.compareTo(sale.getTotalAmount()) > 0) {
            BigDecimal remaining = sale.getTotalAmount().subtract(currentPaid);
            throw new BusinessException(
                    "Payment of " + request.getAmount() + " would overpay. " +
                            "Total due: " + sale.getTotalAmount() + ", already paid: " + currentPaid +
                            ", remaining: " + remaining);
        }

        Payment payment = new Payment();
        payment.setSale(sale);
        payment.setAmount(request.getAmount());
        payment.setDate(request.getDate());
        payment.setMethod(request.getMethod() != null
                ? request.getMethod()
                : Payment.PaymentMethod.CASH);
        payment.setNotes(request.getNotes());

        Payment saved = paymentRepository.save(payment);
        recalculateSale(sale);

        return toResponse(saved);
    }

    // ============ Read ============

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAll() {
        return paymentRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getBySale(Long saleId) {
        if (!saleRepository.existsById(saleId)) {
            throw new ResourceNotFoundException("Sale not found with id: " + saleId);
        }
        return paymentRepository.findBySaleIdOrderByDateAsc(saleId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getByDate(LocalDate date) {
        return paymentRepository.findByDate(date).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getByRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException("'from' date cannot be after 'to' date");
        }
        return paymentRepository.findByDateBetweenOrderByDateAsc(from, to).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ============ Update ============

    @Transactional
    public PaymentResponse update(Long id, PaymentRequest request) {
        Payment payment = findOrThrow(id);
        Sale sale = payment.getSale();

        // Sale cannot be changed on update — keeps financial history clean.
        if (!sale.getId().equals(request.getSaleId())) {
            throw new BusinessException(
                    "Payment cannot be moved to a different sale. Delete and recreate if needed.");
        }

        // Compute other payments for this sale (excluding this one)
        BigDecimal othersSum = paymentRepository.sumAmountBySale(sale.getId())
                .subtract(payment.getAmount());
        BigDecimal newTotalPaid = othersSum.add(request.getAmount());

        if (newTotalPaid.compareTo(sale.getTotalAmount()) > 0) {
            BigDecimal remaining = sale.getTotalAmount().subtract(othersSum);
            throw new BusinessException(
                    "Updated payment of " + request.getAmount() + " would overpay. " +
                            "Maximum allowed now: " + remaining);
        }

        payment.setAmount(request.getAmount());
        payment.setDate(request.getDate());
        payment.setMethod(request.getMethod());
        payment.setNotes(request.getNotes());

        Payment saved = paymentRepository.save(payment);
        recalculateSale(sale);

        return toResponse(saved);
    }

    // ============ Delete ============

    @Transactional
    public void delete(Long id) {
        Payment payment = findOrThrow(id);
        Sale sale = payment.getSale();

        paymentRepository.delete(payment);
        recalculateSale(sale);
    }

    // ============ Summaries ============

    public BigDecimal getDailyCash(LocalDate date) {
        return paymentRepository.sumAmountByDate(date);
    }

    public BigDecimal getCashBetween(LocalDate from, LocalDate to) {
        return paymentRepository.sumAmountBetween(from, to);
    }

    public List<PaymentMethodSummaryResponse> getCashByMethod(LocalDate from, LocalDate to) {
        return paymentRepository.sumAmountByMethodBetween(from, to).stream()
                .map(row -> new PaymentMethodSummaryResponse(
                        (Payment.PaymentMethod) row[0],
                        ((BigDecimal) row[1]).setScale(2, RoundingMode.HALF_UP)))
                .collect(Collectors.toList());
    }

    // ============ Core invariant enforcement ============

    /**
     * Recomputes Sale.paidAmount and Sale.dueAmount from the sum of its payments.
     * Call this after ANY operation that changes payments for a sale.
     *
     * Guarantees: paidAmount + dueAmount == totalAmount
     */
    @Transactional
    protected void recalculateSale(Sale sale) {
        BigDecimal totalPaid = paymentRepository.sumAmountBySale(sale.getId());
        sale.setPaidAmount(totalPaid);
        sale.setDueAmount(sale.getTotalAmount().subtract(totalPaid));
        saleRepository.save(sale);
    }

    // ============ Helpers ============

    private Payment findOrThrow(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }

    private PaymentResponse toResponse(Payment p) {
        PaymentResponse response = new PaymentResponse();
        response.setId(p.getId());
        response.setSaleId(p.getSale().getId());
        response.setAmount(p.getAmount());
        response.setDate(p.getDate());
        response.setMethod(p.getMethod());
        response.setNotes(p.getNotes());
        response.setCreatedAt(p.getCreatedAt());
        return response;
    }
}