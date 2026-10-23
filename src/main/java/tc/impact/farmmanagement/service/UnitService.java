package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.request.UnitRequest;
import tc.impact.farmmanagement.dto.response.UnitResponse;
import tc.impact.farmmanagement.entity.Farm;
import tc.impact.farmmanagement.entity.Unit;
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
import tc.impact.farmmanagement.repository.UnitRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UnitService {

    private final UnitRepository unitRepository;
    private final FarmContext farmContext;

    @Transactional
    public UnitResponse create(UnitRequest request) {
        Farm farm = farmContext.getCurrentFarm();
        String code = normalizeCode(request.getCode());

        if (unitRepository.existsByFarmIdAndCode(farm.getId(), code)) {
            throw new BusinessException("Unit with code '" + code + "' already exists");
        }

        Unit unit = new Unit();
        unit.setFarm(farm);
        unit.setCode(code);
        unit.setName(request.getName());
        unit.setNameBn(request.getNameBn());
        unit.setAbbreviation(request.getAbbreviation());
        unit.setActive(request.getActive() != null ? request.getActive() : true);
        unit.setSystemDefault(false);
        unit.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 100);

        return toResponse(unitRepository.save(unit));
    }

    @Transactional(readOnly = true)
    public List<UnitResponse> getAll(boolean activeOnly) {
        Long farmId = farmContext.getCurrentFarmId();
        List<Unit> units = activeOnly
                ? unitRepository.findByFarmIdAndActiveTrueOrderByDisplayOrderAscNameAsc(farmId)
                : unitRepository.findByFarmIdOrderByDisplayOrderAscNameAsc(farmId);
        return units.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UnitResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public UnitResponse update(Long id, UnitRequest request) {
        Long farmId = farmContext.getCurrentFarmId();
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));

        if (!unit.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Unit not found");
        }

        unit.setName(request.getName().trim());
        unit.setNameBn(request.getNameBn());
        unit.setAbbreviation(request.getAbbreviation());

        // Only update code if it changed
        if (request.getCode() != null && !unit.getCode().equals(request.getCode())) {
            if (unitRepository.findByFarmIdAndCode(farmId, request.getCode()).isPresent()) {
                throw new BusinessException("Unit code already exists");
            }
            unit.setCode(request.getCode().trim().toUpperCase());
        }

        return toResponse(unitRepository.save(unit));
    }

    @Transactional
    public UnitResponse toggleActive(Long id, boolean active) {
        Long farmId = farmContext.getCurrentFarmId();
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));

        if (!unit.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Unit not found");
        }

        // Protect system defaults from being deactivated
        if (unit.isSystemDefault() && !active) {
            throw new BusinessException("Cannot deactivate system default units.");
        }

        unit.setActive(active);
        return toResponse(unitRepository.save(unit));
    }

    @Transactional
    public void delete(Long id) {
        Unit unit = findOrThrow(id);
        if (unit.isSystemDefault()) {
            throw new BusinessException(
                    "Cannot delete system unit '" + unit.getCode() +
                            "'. Deactivate it instead (set active=false).");
        }
        unitRepository.deleteById(id);
    }

    private Unit findOrThrow(Long id) {
        Long farmId = farmContext.getCurrentFarmId();
        Unit u = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with id: " + id));
        if (!u.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Unit not found with id: " + id);
        }
        return u;
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase().replaceAll("\\s+", "_");
    }

    UnitResponse toResponse(Unit u) {
        UnitResponse r = new UnitResponse();
        r.setId(u.getId());
        r.setCode(u.getCode());
        r.setName(u.getName());
        r.setNameBn(u.getNameBn());
        r.setAbbreviation(u.getAbbreviation());
        r.setActive(u.isActive());
        r.setSystemDefault(u.isSystemDefault());
        r.setDisplayOrder(u.getDisplayOrder());
        r.setCreatedAt(u.getCreatedAt());
        return r;
    }
}