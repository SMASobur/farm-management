package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tc.impact.farmmanagement.dto.request.CustomerRequest;
import tc.impact.farmmanagement.dto.response.CustomerResponse;
import tc.impact.farmmanagement.entity.Customer;
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

    // ============ Create ============

    public CustomerResponse create(CustomerRequest request) {
        if (request.getPhone() != null && !request.getPhone().isBlank()
                && customerRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("Customer with phone '" + request.getPhone() + "' already exists");
        }

        Customer customer = new Customer();
        customer.setName(request.getName());
        customer.setPhone(normalizePhone(request.getPhone()));
        customer.setAddress(request.getAddress());
        customer.setType(request.getType() != null ? request.getType() : Customer.CustomerType.HOUSEHOLD);
        customer.setNotes(request.getNotes());

        return toResponse(customerRepository.save(customer));
    }

    // ============ Read ============

    public List<CustomerResponse> getAll() {
        return customerRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CustomerResponse getById(Long id) {
        Customer customer = findOrThrow(id);
        CustomerResponse response = toResponse(customer);
        response.setTotalDue(customerRepository.sumDueByCustomer(id));
        return response;
    }

    public List<CustomerResponse> searchByName(String namePart) {
        return customerRepository.findByNameContainingIgnoreCase(namePart).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<CustomerResponse> getByType(Customer.CustomerType type) {
        return customerRepository.findByType(type).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ============ Update ============

    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = findOrThrow(id);

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            String normalized = normalizePhone(request.getPhone());
            if (!normalized.equals(customer.getPhone()) && customerRepository.existsByPhone(normalized)) {
                throw new BusinessException("Customer with phone '" + normalized + "' already exists");
            }
            customer.setPhone(normalized);
        } else {
            customer.setPhone(null);
        }

        customer.setName(request.getName());
        customer.setAddress(request.getAddress());
        customer.setType(request.getType());
        customer.setNotes(request.getNotes());

        return toResponse(customerRepository.save(customer));
    }

    // ============ Delete ============

    public void delete(Long id) {
        Customer customer = findOrThrow(id);
        // Safety check: don't allow deleting a customer with outstanding sales
        long saleCount = saleRepository.findByCustomerId(id).size();
        if (saleCount > 0) {
            throw new BusinessException(
                    "Cannot delete customer '" + customer.getName() +
                            "' — they have " + saleCount + " sale(s) on record. Delete the sales first.");
        }
        customerRepository.deleteById(id);
    }

    // ============ Helpers ============

    private Customer findOrThrow(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
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