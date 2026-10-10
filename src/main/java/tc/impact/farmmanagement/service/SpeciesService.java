package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.response.SpeciesResponse;
import tc.impact.farmmanagement.entity.Species;
import tc.impact.farmmanagement.repository.SpeciesRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpeciesService {

    private final SpeciesRepository speciesRepository;
    private final FarmContext farmContext;

    @Transactional(readOnly = true)
    public List<SpeciesResponse> getAll(boolean activeOnly) {
        Long farmId = farmContext.getCurrentFarmId();
        List<Species> list = activeOnly
                ? speciesRepository.findByFarmIdAndActiveTrueOrderByDisplayOrderAscNameAsc(farmId)
                : speciesRepository.findByFarmIdOrderByDisplayOrderAscNameAsc(farmId);
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private SpeciesResponse toResponse(Species s) {
        SpeciesResponse r = new SpeciesResponse();
        r.setId(s.getId());
        r.setCode(s.getCode());
        r.setName(s.getName());
        r.setNameBn(s.getNameBn());
        r.setMilkProducer(s.isMilkProducer());
        r.setActive(s.isActive());
        r.setSystemDefault(s.isSystemDefault());
        r.setDisplayOrder(s.getDisplayOrder());
        r.setCreatedAt(s.getCreatedAt());
        return r;
    }
}