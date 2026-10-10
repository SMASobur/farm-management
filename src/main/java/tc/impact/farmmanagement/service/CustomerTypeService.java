package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.response.CustomerTypeResponse;
import tc.impact.farmmanagement.entity.CustomerType;
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