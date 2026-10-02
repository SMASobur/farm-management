package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tc.impact.farmmanagement.dto.request.FlockRequest;
import tc.impact.farmmanagement.dto.response.FlockResponse;
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

    public FlockResponse create(FlockRequest request) {
        if (flockRepository.existsByFlockCode(request.getFlockCode())) {
            throw new BusinessException("Flock with code '" + request.getFlockCode() + "' already exists");
        }

        Flock flock = new Flock();
        flock.setFlockCode(request.getFlockCode());
        flock.setName(request.getName());
        flock.setSpecies(request.getSpecies());
        flock.setBirdCount(request.getBirdCount());
        flock.setStatus(request.getStatus() != null ? request.getStatus() : Flock.FlockStatus.ACTIVE);
        flock.setStartedOn(request.getStartedOn());

        return toResponse(flockRepository.save(flock));
    }

    public List<FlockResponse> getAll() {
        return flockRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public FlockResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public List<FlockResponse> getBySpecies(Flock.Species species) {
        return flockRepository.findBySpecies(species).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public FlockResponse update(Long id, FlockRequest request) {
        Flock flock = findOrThrow(id);

        if (!flock.getFlockCode().equals(request.getFlockCode())
                && flockRepository.existsByFlockCode(request.getFlockCode())) {
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

    public void delete(Long id) {
        if (!flockRepository.existsById(id)) {
            throw new ResourceNotFoundException("Flock not found with id: " + id);
        }
        flockRepository.deleteById(id);
    }

    Flock findOrThrow(Long id) {
        return flockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flock not found with id: " + id));
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