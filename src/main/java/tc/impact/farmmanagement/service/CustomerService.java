package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.request.CustomerRequest;
import tc.impact.farmmanagement.dto.response.CustomerResponse;
import tc.impact.farmmanagement.entity.Customer;
import tc.impact.farmmanagement.entity.Farm;
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
import tc.impact.farmmanagement.repository.CustomerRepository;
import tc.impact.farmmanagement.repository.SaleRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final FarmContext farmContext;

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        Farm farm = farmContext.getCurrentFarm();
        String normalizedPhone = normalizePhone(request.getPhone());

        if (normalizedPhone != null
                && customerRepository.existsByFarmIdAndPhone(farm.getId(), normalizedPhone)) {
            throw new BusinessException("Customer with phone '" + normalizedPhone + "' already exists");
        }

        Customer customer = new Customer();
        customer.setFarm(farm);
        customer.setName(request.getName());
        customer.setPhone(normalizedPhone);
        customer.setAddress(request.getAddress());
        customer.setType(request.getType() != null ? request.getType() : Customer.CustomerType.HOUSEHOLD);
        customer.setNotes(request.getNotes());

        return toResponse(customerRepository.save(customer));
    }

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
    public List<CustomerResponse> getByType(Customer.CustomerType type) {
        Long farmId = farmContext.getCurrentFarmId();
        return customerRepository.findByFarmIdAndType(farmId, type).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

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

        customer.setName(request.getName());
        customer.setAddress(request.getAddress());
        customer.setType(request.getType());
        customer.setNotes(request.getNotes());

        return toResponse(customerRepository.save(customer));
    }

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

    private Customer findOrThrow(Long id) {
        Long farmId = farmContext.getCurrentFarmId();
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        if (!customer.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + id);
        }
        return customer;
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String trimmed = phone.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private CustomerResponse toResponse(Customer customer) {
        CustomerResponse response = new CustomerResponse();
        response.setId(customer.getId());
        response.setName(customer.getName());
        response.setPhone(customer.getPhone());
        response.setAddress(customer.getAddress());
        response.setType(customer.getType());
        response.setNotes(customer.getNotes());
        response.setCreatedAt(customer.getCreatedAt());
        return response;
    }
}