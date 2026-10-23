package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.request.CustomerTypeRequest;
import tc.impact.farmmanagement.dto.response.CustomerTypeResponse;
import tc.impact.farmmanagement.entity.CustomerType;
import tc.impact.farmmanagement.entity.Farm;
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
import tc.impact.farmmanagement.repository.CustomerTypeRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerTypeService {

    private final CustomerTypeRepository customerTypeRepository;
    private final FarmContext farmContext;

    @Transactional(readOnly = true)
    public List<CustomerTypeResponse> getAll(boolean activeOnly) {
        Long farmId = farmContext.getCurrentFarmId();
        List<CustomerType> list = activeOnly
                ? customerTypeRepository.findByFarmIdAndActiveTrueOrderByDisplayOrderAscNameAsc(farmId)
                : customerTypeRepository.findByFarmIdOrderByDisplayOrderAscNameAsc(farmId);
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public CustomerTypeResponse create(CustomerTypeRequest request) {
        Farm farm = farmContext.getCurrentFarm();
        Long farmId = farm.getId();

        // Check if code already exists
        String normalizedCode = request.getCode().trim().toUpperCase();
        if (customerTypeRepository.findByFarmIdAndCode(farmId, normalizedCode).isPresent()) {
            throw new BusinessException("Customer type code '" + normalizedCode + "' already exists.");
        }

        CustomerType type = new CustomerType();
        type.setFarm(farm);
        type.setCode(normalizedCode);
        type.setName(request.getName().trim());
        type.setNameBn(request.getNameBn() != null ? request.getNameBn().trim() : null);
        type.setActive(true);
        type.setSystemDefault(false);
        type.setDisplayOrder(900);

        return toResponse(customerTypeRepository.save(type));
    }

    @Transactional
    public CustomerTypeResponse update(Long id, CustomerTypeRequest request) {
        Long farmId = farmContext.getCurrentFarmId();
        CustomerType type = customerTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer type not found"));

        if (!type.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Customer type not found");
        }

        type.setName(request.getName().trim());
        type.setNameBn(request.getNameBn());

        if (request.getCode() != null && !type.getCode().equals(request.getCode())) {
            if (customerTypeRepository.findByFarmIdAndCode(farmId, request.getCode()).isPresent()) {
                throw new BusinessException("Customer type code already exists");
            }
            type.setCode(request.getCode().trim().toUpperCase());
        }

        return toResponse(customerTypeRepository.save(type));
    }

    @Transactional
    public CustomerTypeResponse toggleActive(Long id, boolean active) {
        Long farmId = farmContext.getCurrentFarmId();
        CustomerType type = customerTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer type not found"));

        if (!type.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Customer type not found");
        }

        if (type.isSystemDefault() && !active) {
            throw new BusinessException("Cannot deactivate system default types.");
        }

        type.setActive(active);
        return toResponse(customerTypeRepository.save(type));
    }

    private CustomerTypeResponse toResponse(CustomerType t) {
        CustomerTypeResponse r = new CustomerTypeResponse();
        r.setId(t.getId());
        r.setCode(t.getCode());
        r.setName(t.getName());
        r.setNameBn(t.getNameBn());
        r.setActive(t.isActive());
        r.setSystemDefault(t.isSystemDefault());
        r.setDisplayOrder(t.getDisplayOrder());
        r.setCreatedAt(t.getCreatedAt());
        return r;
    }
}