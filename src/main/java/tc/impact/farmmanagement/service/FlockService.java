package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.request.FlockRequest;
import tc.impact.farmmanagement.dto.response.FlockResponse;
import tc.impact.farmmanagement.entity.Farm;
import tc.impact.farmmanagement.entity.Flock;
import tc.impact.farmmanagement.entity.Species;
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
import tc.impact.farmmanagement.repository.FlockRepository;
import tc.impact.farmmanagement.repository.SpeciesRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlockService {

    private final FlockRepository flockRepository;
    private final SpeciesRepository speciesRepository;
    private final FarmContext farmContext;

    // ============ Create ============

    @Transactional
    public FlockResponse create(FlockRequest request) {
        Farm farm = farmContext.getCurrentFarm();

        if (flockRepository.existsByFarmIdAndFlockCode(farm.getId(), request.getFlockCode())) {
            throw new BusinessException("Flock with code '" + request.getFlockCode() + "' already exists");
        }

        Species species = resolveSpecies(request, farm);

        Flock flock = new Flock();
        flock.setFarm(farm);
        flock.setFlockCode(request.getFlockCode());
        flock.setName(request.getName());
        flock.setSpecies(species);
        flock.setBirdCount(request.getBirdCount());
        flock.setStatus(request.getStatus() != null ? request.getStatus() : Flock.FlockStatus.ACTIVE);
        flock.setStartedOn(request.getStartedOn());

        return toResponse(flockRepository.save(flock));
    }

    // ============ Read ============

    @Transactional(readOnly = true)
    public List<FlockResponse> getAll() {
        Long farmId = farmContext.getCurrentFarmId();
        return flockRepository.findByFarmId(farmId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FlockResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<FlockResponse> getBySpecies(Long speciesId) {
        Long farmId = farmContext.getCurrentFarmId();
        return flockRepository.findByFarmIdAndSpeciesId(farmId, speciesId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FlockResponse> getByStatus(Flock.FlockStatus status) {
        Long farmId = farmContext.getCurrentFarmId();
        return flockRepository.findByFarmIdAndStatus(farmId, status).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FlockResponse> getBySpeciesAndStatus(Long speciesId, Flock.FlockStatus status) {
        Long farmId = farmContext.getCurrentFarmId();
        // Filter in-memory since we only have single-filter repository methods
        return flockRepository.findByFarmIdAndSpeciesId(farmId, speciesId).stream()
                .filter(f -> f.getStatus() == status)
                .map(this::toResponse).collect(Collectors.toList());
    }
    // ============ Update ============

    @Transactional
    public FlockResponse update(Long id, FlockRequest request) {
        Long farmId = farmContext.getCurrentFarmId();
        Flock flock = findOrThrow(id);

        if (!flock.getFlockCode().equals(request.getFlockCode())
                && flockRepository.existsByFarmIdAndFlockCode(farmId, request.getFlockCode())) {
            throw new BusinessException("Flock with code '" + request.getFlockCode() + "' already exists");
        }

        Species species = resolveSpecies(request, farmContext.getCurrentFarm());

        flock.setFlockCode(request.getFlockCode());
        flock.setName(request.getName());
        flock.setSpecies(species);
        flock.setBirdCount(request.getBirdCount());
        if (request.getStatus() != null) flock.setStatus(request.getStatus());
        flock.setStartedOn(request.getStartedOn());

        return toResponse(flockRepository.save(flock));
    }

    // ============ Delete ============

    @Transactional
    public void delete(Long id) {
        Flock flock = findOrThrow(id);
        flockRepository.deleteById(flock.getId());
    }

    // ============ Helpers ============

    Flock findOrThrow(Long id) {
        Long farmId = farmContext.getCurrentFarmId();
        Flock flock = flockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flock not found with id: " + id));
        if (!flock.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Flock not found with id: " + id);
        }
        return flock;
    }

    private Species resolveSpecies(FlockRequest request, Farm farm) {
        boolean hasSpeciesId = request.getSpeciesId() != null;
        boolean hasCustomName = request.getCustomSpeciesName() != null
                && !request.getCustomSpeciesName().isBlank();

        if (hasSpeciesId && hasCustomName) {
            throw new BusinessException("Provide either speciesId or customSpeciesName, not both");
        }
        if (!hasSpeciesId && !hasCustomName) {
            throw new BusinessException("Species is required");
        }

        if (hasSpeciesId) {
            Species sp = speciesRepository.findById(request.getSpeciesId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Species not found with id: " + request.getSpeciesId()));
            if (!sp.getFarm().getId().equals(farm.getId())) {
                throw new ResourceNotFoundException("Species not found");
            }
            if (!sp.isActive()) {
                throw new BusinessException("Species '" + sp.getName() + "' is inactive");
            }
            return sp;
        }

        String name = request.getCustomSpeciesName().trim();
        String code = normalizeCode(name);
        if (code.isEmpty()) {
            throw new BusinessException("Invalid species name");
        }
        if (code.length() > 50) {
            code = code.substring(0, 50);
        }

        var existing = speciesRepository.findByFarmIdAndCode(farm.getId(), code);
        if (existing.isPresent()) {
            return existing.get();
        }

        boolean saveForReuse = Boolean.TRUE.equals(request.getSaveCustomSpecies());

        Species sp = new Species();
        sp.setFarm(farm);
        sp.setCode(code);
        sp.setName(name);
        sp.setNameBn(request.getCustomSpeciesNameBn());
        sp.setMilkProducer(Boolean.TRUE.equals(request.getCustomSpeciesMilkProducer()));
        sp.setEggProducer(Boolean.TRUE.equals(request.getCustomSpeciesEggProducer()));
        sp.setActive(saveForReuse);
        sp.setSystemDefault(false);
        sp.setDisplayOrder(900);
        return speciesRepository.save(sp);
    }

    private String normalizeCode(String name) {
        return name.trim()
                .toUpperCase()
                .replaceAll("[^\\p{L}\\p{N}]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private FlockResponse toResponse(Flock f) {
        FlockResponse r = new FlockResponse();
        r.setId(f.getId());
        r.setFlockCode(f.getFlockCode());
        r.setName(f.getName());
        r.setSpeciesId(f.getSpecies().getId());
        r.setSpeciesCode(f.getSpecies().getCode());
        r.setSpeciesName(f.getSpecies().getName());
        r.setSpeciesNameBn(f.getSpecies().getNameBn());
        r.setBirdCount(f.getBirdCount());
        r.setStatus(f.getStatus());
        r.setStartedOn(f.getStartedOn());
        r.setCreatedAt(f.getCreatedAt());
        return r;
    }
}