package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.request.CustomerRequest;
import tc.impact.farmmanagement.dto.response.CustomerResponse;
import tc.impact.farmmanagement.entity.Customer;
import tc.impact.farmmanagement.entity.CustomerType;
import tc.impact.farmmanagement.entity.Farm;
import tc.impact.farmmanagement.entity.Sale;
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
import tc.impact.farmmanagement.repository.CustomerRepository;
import tc.impact.farmmanagement.repository.CustomerTypeRepository;
import tc.impact.farmmanagement.repository.SaleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerTypeRepository customerTypeRepository;
    private final SaleRepository saleRepository;
    private final FarmContext farmContext;
    private final ExportService exportService;

    // ============ Create ============

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        Farm farm = farmContext.getCurrentFarm();
        String normalizedPhone = normalizePhone(request.getPhone());

        if (normalizedPhone != null
                && customerRepository.existsByFarmIdAndPhone(farm.getId(), normalizedPhone)) {
            throw new BusinessException("Customer with phone '" + normalizedPhone + "' already exists");
        }

        CustomerType customerType = resolveCustomerType(request, farm);

        Customer customer = new Customer();
        customer.setFarm(farm);
        customer.setName(request.getName());
        customer.setPhone(normalizedPhone);
        customer.setAddress(request.getAddress());
        customer.setCustomerType(customerType);
        customer.setNotes(request.getNotes());

        return toResponse(customerRepository.save(customer));
    }

    // ============ Read ============

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAll() {
        Long farmId = farmContext.getCurrentFarmId();
        return customerRepository.findByFarmId(farmId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(Long id) {
        Long farmId = farmContext.getCurrentFarmId();
        Customer customer = findOrThrow(id);
        CustomerResponse response = toResponse(customer);
        response.setTotalDue(saleRepository.sumDueByCustomerAndFarm(id, farmId));
        return response;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> searchByName(String namePart) {
        Long farmId = farmContext.getCurrentFarmId();
        return customerRepository.findByFarmIdAndNameContainingIgnoreCase(farmId, namePart).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getByType(Long customerTypeId) {
        Long farmId = farmContext.getCurrentFarmId();
        return customerRepository.findByFarmIdAndCustomerTypeId(farmId, customerTypeId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    // ============ Update ============

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Long farmId = farmContext.getCurrentFarmId();
        Customer customer = findOrThrow(id);

        String normalizedPhone = normalizePhone(request.getPhone());
        if (normalizedPhone != null) {
            if (!normalizedPhone.equals(customer.getPhone())
                    && customerRepository.existsByFarmIdAndPhone(farmId, normalizedPhone)) {
                throw new BusinessException("Customer with phone '" + normalizedPhone + "' already exists");
            }
            customer.setPhone(normalizedPhone);
        } else {
            customer.setPhone(null);
        }

        CustomerType customerType = resolveCustomerType(request, farmContext.getCurrentFarm());

        customer.setName(request.getName());
        customer.setAddress(request.getAddress());
        customer.setCustomerType(customerType);
        customer.setNotes(request.getNotes());

        return toResponse(customerRepository.save(customer));
    }

    // ============ Delete ============

    @Transactional
    public void delete(Long id) {
        Long farmId = farmContext.getCurrentFarmId();
        Customer customer = findOrThrow(id);
        long saleCount = saleRepository.findByFarmIdAndCustomerId(farmId, id).size();
        if (saleCount > 0) {
            throw new BusinessException(
                    "Cannot delete customer '" + customer.getName() +
                            "' — they have " + saleCount + " sale(s) on record.");
        }
        customerRepository.deleteById(id);
    }

    // ============ Statement PDF ============

    @Transactional(readOnly = true)
    public byte[] generateStatementPdf(Long customerId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException("'from' date cannot be after 'to' date");
        }

        Long farmId = farmContext.getCurrentFarmId();
        Customer customer = findOrThrow(customerId);

        List<Sale> sales = saleRepository.findByFarmIdAndCustomerIdAndDateBetweenOrderByDateAsc(
                farmId, customerId, from, to);

        BigDecimal totalSales = sales.stream()
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = sales.stream()
                .map(Sale::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDue = totalSales.subtract(totalPaid);

        return exportService.exportCustomerStatementAsPdf(
                customer, sales, totalSales, totalPaid, totalDue, from, to);
    }

    // ============ Helpers ============

    private Customer findOrThrow(Long id) {
        Long farmId = farmContext.getCurrentFarmId();
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        if (!customer.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + id);
        }
        return customer;
    }

    private CustomerType resolveCustomerType(CustomerRequest request, Farm farm) {
        boolean hasTypeId = request.getCustomerTypeId() != null;
        boolean hasCustomName = request.getCustomTypeName() != null
                && !request.getCustomTypeName().isBlank();

        if (hasTypeId && hasCustomName) {
            throw new BusinessException("Provide either customerTypeId or customTypeName, not both");
        }

        // Default to HOUSEHOLD if neither provided
        if (!hasTypeId && !hasCustomName) {
            return customerTypeRepository.findByFarmIdAndCode(farm.getId(), "HOUSEHOLD")
                    .orElseThrow(() -> new BusinessException(
                            "Default customer type 'Household' not found. Please check seeders."));
        }

        if (hasTypeId) {
            CustomerType type = customerTypeRepository.findById(request.getCustomerTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Customer type not found with id: " + request.getCustomerTypeId()));
            if (!type.getFarm().getId().equals(farm.getId())) {
                throw new ResourceNotFoundException("Customer type not found");
            }
            if (!type.isActive()) {
                throw new BusinessException("Customer type '" + type.getName() + "' is inactive");
            }
            return type;
        }

        // Custom type — normalize to code
        String name = request.getCustomTypeName().trim();
        String code = normalizeCode(name);
        if (code.isEmpty()) {
            throw new BusinessException("Invalid customer type name");
        }
        if (code.length() > 50) {
            code = code.substring(0, 50);
        }

        var existing = customerTypeRepository.findByFarmIdAndCode(farm.getId(), code);
        if (existing.isPresent()) {
            return existing.get();
        }

        boolean saveForReuse = Boolean.TRUE.equals(request.getSaveCustomType());

        CustomerType type = new CustomerType();
        type.setFarm(farm);
        type.setCode(code);
        type.setName(name);
        type.setNameBn(
                request.getCustomTypeNameBn() != null
                        ? request.getCustomTypeNameBn().trim()
                        : null);
        type.setActive(saveForReuse);
        type.setSystemDefault(false);
        type.setDisplayOrder(900);
        return customerTypeRepository.save(type);
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String trimmed = phone.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeCode(String name) {
        return name.trim()
                .toUpperCase()
                .replaceAll("[^\\p{L}\\p{N}]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private CustomerResponse toResponse(Customer customer) {
        CustomerResponse response = new CustomerResponse();
        response.setId(customer.getId());
        response.setName(customer.getName());
        response.setPhone(customer.getPhone());
        response.setAddress(customer.getAddress());
        if (customer.getCustomerType() != null) {
            response.setCustomerTypeId(customer.getCustomerType().getId());
            response.setCustomerTypeCode(customer.getCustomerType().getCode());
            response.setCustomerTypeName(customer.getCustomerType().getName());
            response.setCustomerTypeNameBn(customer.getCustomerType().getNameBn());
        }
        response.setNotes(customer.getNotes());
        response.setCreatedAt(customer.getCreatedAt());
        return response;
    }
}