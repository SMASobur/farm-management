package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.request.PaymentRequest;
import tc.impact.farmmanagement.dto.response.PaymentMethodSummaryResponse;
import tc.impact.farmmanagement.dto.response.PaymentResponse;
import tc.impact.farmmanagement.entity.Farm;
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
    private final FarmContext farmContext;

    @Transactional
    public PaymentResponse create(PaymentRequest request) {
        Farm farm = farmContext.getCurrentFarm();
        Sale sale = saleRepository.findById(request.getSaleId())
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with id: " + request.getSaleId()));
        if (!sale.getFarm().getId().equals(farm.getId())) {
            throw new ResourceNotFoundException("Sale not found with id: " + request.getSaleId());
        }

        BigDecimal currentPaid = paymentRepository.sumAmountBySale(sale.getId());
        BigDecimal newTotalPaid = currentPaid.add(request.getAmount());

        if (newTotalPaid.compareTo(sale.getTotalAmount()) > 0) {
            throw new BusinessException(
                    "Payment would overpay. Total: " + sale.getTotalAmount() +
                            ", already paid: " + currentPaid +
                            ", remaining: " + sale.getTotalAmount().subtract(currentPaid));
        }

        Payment payment = new Payment();
        payment.setFarm(farm);
        payment.setSale(sale);
        payment.setAmount(request.getAmount());
        payment.setDate(request.getDate());
        payment.setMethod(request.getMethod() != null ? request.getMethod() : Payment.PaymentMethod.CASH);
        payment.setNotes(request.getNotes());

        Payment saved = paymentRepository.save(payment);
        recalculateSale(sale);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAll() {
        Long farmId = farmContext.getCurrentFarmId();
        return paymentRepository.findByFarmId(farmId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getBySale(Long saleId) {
        Long farmId = farmContext.getCurrentFarmId();
        return paymentRepository.findByFarmIdAndSaleIdOrderByDateAsc(farmId, saleId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getByDate(LocalDate date) {
        Long farmId = farmContext.getCurrentFarmId();
        return paymentRepository.findByFarmIdAndDate(farmId, date).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getByRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) throw new BusinessException("'from' date cannot be after 'to' date");
        Long farmId = farmContext.getCurrentFarmId();
        return paymentRepository.findByFarmIdAndDateBetweenOrderByDateAsc(farmId, from, to).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public PaymentResponse update(Long id, PaymentRequest request) {
        Payment payment = findOrThrow(id);
        Sale sale = payment.getSale();

        if (!sale.getId().equals(request.getSaleId())) {
            throw new BusinessException("Payment cannot be moved to a different sale.");
        }

        BigDecimal othersSum = paymentRepository.sumAmountBySale(sale.getId()).subtract(payment.getAmount());
        BigDecimal newTotalPaid = othersSum.add(request.getAmount());

        if (newTotalPaid.compareTo(sale.getTotalAmount()) > 0) {
            throw new BusinessException("Updated payment would overpay.");
        }

        payment.setAmount(request.getAmount());
        payment.setDate(request.getDate());
        payment.setMethod(request.getMethod());
        payment.setNotes(request.getNotes());

        Payment saved = paymentRepository.save(payment);
        recalculateSale(sale);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Payment payment = findOrThrow(id);
        Sale sale = payment.getSale();
        paymentRepository.delete(payment);
        recalculateSale(sale);
    }

    @Transactional(readOnly = true)
    public BigDecimal getDailyCash(LocalDate date) {
        return paymentRepository.sumAmountByFarmAndDate(farmContext.getCurrentFarmId(), date);
    }

    @Transactional(readOnly = true)
    public BigDecimal getCashBetween(LocalDate from, LocalDate to) {
        return paymentRepository.sumAmountByFarmBetween(farmContext.getCurrentFarmId(), from, to);
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodSummaryResponse> getCashByMethod(LocalDate from, LocalDate to) {
        return paymentRepository.sumAmountByMethodBetween(farmContext.getCurrentFarmId(), from, to).stream()
                .map(row -> new PaymentMethodSummaryResponse(
                        (Payment.PaymentMethod) row[0],
                        ((BigDecimal) row[1]).setScale(2, RoundingMode.HALF_UP)))
                .collect(Collectors.toList());
    }

    @Transactional
    protected void recalculateSale(Sale sale) {
        BigDecimal totalPaid = paymentRepository.sumAmountBySale(sale.getId());
        sale.setPaidAmount(totalPaid);
        sale.setDueAmount(sale.getTotalAmount().subtract(totalPaid));
        saleRepository.save(sale);
    }

    private Payment findOrThrow(Long id) {
        Long farmId = farmContext.getCurrentFarmId();
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        if (!payment.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Payment not found with id: " + id);
        }
        return payment;
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