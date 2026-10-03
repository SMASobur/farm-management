package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.request.FlockRequest;
import tc.impact.farmmanagement.dto.response.FlockResponse;
import tc.impact.farmmanagement.entity.Farm;
import tc.impact.farmmanagement.entity.Flock;
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
import tc.impact.farmmanagement.repository.FlockRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlockService {

    private final FlockRepository flockRepository;
    private final FarmContext farmContext;

    @Transactional
    public FlockResponse create(FlockRequest request) {
        Farm farm = farmContext.getCurrentFarm();

        if (flockRepository.existsByFarmIdAndFlockCode(farm.getId(), request.getFlockCode())) {
            throw new BusinessException("Flock with code '" + request.getFlockCode() + "' already exists");
        }

        Flock flock = new Flock();
        flock.setFarm(farm);
        flock.setFlockCode(request.getFlockCode());
        flock.setName(request.getName());
        flock.setSpecies(request.getSpecies());
        flock.setBirdCount(request.getBirdCount());
        flock.setStatus(request.getStatus() != null ? request.getStatus() : Flock.FlockStatus.ACTIVE);
        flock.setStartedOn(request.getStartedOn());

        return toResponse(flockRepository.save(flock));
    }

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
    public List<FlockResponse> getBySpecies(Flock.Species species) {
        Long farmId = farmContext.getCurrentFarmId();
        return flockRepository.findByFarmIdAndSpecies(farmId, species).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public FlockResponse update(Long id, FlockRequest request) {
        Long farmId = farmContext.getCurrentFarmId();
        Flock flock = findOrThrow(id);

        if (!flock.getFlockCode().equals(request.getFlockCode())
                && flockRepository.existsByFarmIdAndFlockCode(farmId, request.getFlockCode())) {
            throw new BusinessException("Flock with code '" + request.getFlockCode() + "' already exists");
        }

        flock.setFlockCode(request.getFlockCode());
        flock.setName(request.getName());
        flock.setSpecies(request.getSpecies());
        flock.setBirdCount(request.getBirdCount());
        flock.setStatus(request.getStatus());
        flock.setStartedOn(request.getStartedOn());

        return toResponse(flockRepository.save(flock));
    }

    @Transactional
    public void delete(Long id) {
        Flock flock = findOrThrow(id);
        flockRepository.deleteById(flock.getId());
    }

    Flock findOrThrow(Long id) {
        Long farmId = farmContext.getCurrentFarmId();
        Flock flock = flockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flock not found with id: " + id));
        if (!flock.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Flock not found with id: " + id);
        }
        return flock;
    }

    private FlockResponse toResponse(Flock flock) {
        FlockResponse response = new FlockResponse();
        response.setId(flock.getId());
        response.setFlockCode(flock.getFlockCode());
        response.setName(flock.getName());
        response.setSpecies(flock.getSpecies());
        response.setBirdCount(flock.getBirdCount());
        response.setStatus(flock.getStatus());
        response.setStartedOn(flock.getStartedOn());
        response.setCreatedAt(flock.getCreatedAt());
        return response;
    }
}