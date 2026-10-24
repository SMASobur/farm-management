package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.request.SpeciesRequest;
import tc.impact.farmmanagement.dto.response.SpeciesResponse;
import tc.impact.farmmanagement.entity.Species;
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
import tc.impact.farmmanagement.repository.AnimalRepository;
import tc.impact.farmmanagement.repository.FlockRepository;
import tc.impact.farmmanagement.repository.SpeciesRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpeciesService {

    private final SpeciesRepository speciesRepository;
    private final FarmContext farmContext;
    private final AnimalRepository animalRepository;
    private final FlockRepository flockRepository;

    @Transactional(readOnly = true)
    public List<SpeciesResponse> getAll(boolean activeOnly) {
        Long farmId = farmContext.getCurrentFarmId();
        List<Species> list = activeOnly
                ? speciesRepository.findByFarmIdAndActiveTrueOrderByDisplayOrderAscNameAsc(farmId)
                : speciesRepository.findByFarmIdOrderByDisplayOrderAscNameAsc(farmId);
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public SpeciesResponse create(SpeciesRequest request) {
        Long farmId = farmContext.getCurrentFarmId();
        if (speciesRepository.findByFarmIdAndCode(farmId, request.getCode()).isPresent()) {
            throw new BusinessException("Species with code '" + request.getCode() + "' already exists.");
        }

        Species species = new Species();
        species.setFarm(farmContext.getCurrentFarm());
        species.setCode(request.getCode().trim().toUpperCase());
        species.setName(request.getName().trim());
        species.setNameBn(request.getNameBn());
        species.setMilkProducer(request.isMilkProducer());
        species.setEggProducer(request.isEggProducer());
        species.setActive(request.getActive() != null ? request.getActive() : true);
        species.setSystemDefault(false);
        species.setDisplayOrder(900);
        species.setEmoji(request.getEmoji());

        return toResponse(speciesRepository.save(species));
    }

    @Transactional
    public SpeciesResponse update(Long id, SpeciesRequest request) {
        Long farmId = farmContext.getCurrentFarmId();
        Species species = speciesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Species not found"));

        if (!species.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Species not found");
        }

        species.setName(request.getName().trim());
        species.setNameBn(request.getNameBn());
        species.setMilkProducer(request.isMilkProducer());
        species.setEggProducer(request.isEggProducer());
        species.setEmoji(request.getEmoji());

        if (request.getCode() != null && !species.getCode().equals(request.getCode().trim().toUpperCase())) {
            String newCode = request.getCode().trim().toUpperCase();
            if (speciesRepository.findByFarmIdAndCode(farmId, newCode).isPresent()) {
                throw new BusinessException("Species with code '" + newCode + "' already exists.");
            }
            species.setCode(newCode);
        }

        if (request.getActive() != null) {
            species.setActive(request.getActive());
        }

        return toResponse(speciesRepository.save(species));
    }

    @Transactional
    public SpeciesResponse toggleActive(Long id, boolean active) {
        Long farmId = farmContext.getCurrentFarmId();
        Species species = speciesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Species not found"));

        if (!species.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Species not found");
        }

        species.setActive(active);
        return toResponse(speciesRepository.save(species));
    }

    // ADDED: Safe delete method
    @Transactional
    public void delete(Long id) {
        Long farmId = farmContext.getCurrentFarmId();
        Species species = speciesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Species not found"));

        if (!species.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Species not found");
        }

        if (species.isSystemDefault()) {
            throw new BusinessException("Cannot delete system species '" + species.getName() + "'. Deactivate it instead.");
        }

        // Check if any animals or flocks are using this species
        long animalCount = animalRepository.countBySpeciesId(id);
        long flockCount = flockRepository.countBySpeciesId(id);

        if (animalCount > 0 || flockCount > 0) {
            throw new BusinessException(
                    "Cannot delete species '" + species.getName() +
                            "'. It is used by " + animalCount + " animal(s) and " + flockCount + " batch(es). Deactivate it instead.");
        }

        speciesRepository.deleteById(id);
    }

    private SpeciesResponse toResponse(Species s) {
        SpeciesResponse r = new SpeciesResponse();
        r.setId(s.getId());
        r.setCode(s.getCode());
        r.setName(s.getName());
        r.setNameBn(s.getNameBn());
        r.setMilkProducer(s.isMilkProducer());
        r.setEggProducer(s.isEggProducer());
        r.setActive(s.isActive());
        r.setSystemDefault(s.isSystemDefault());
        r.setDisplayOrder(s.getDisplayOrder());
        r.setCreatedAt(s.getCreatedAt());
        r.setMaxDailyYield(s.getMaxDailyYield());
        r.setEmoji(s.getEmoji());
        return r;
    }
}