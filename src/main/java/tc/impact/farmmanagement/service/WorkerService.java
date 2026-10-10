package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.request.WorkerRequest;
import tc.impact.farmmanagement.dto.response.WorkerResponse;
import tc.impact.farmmanagement.entity.Farm;
import tc.impact.farmmanagement.entity.Worker;
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
import tc.impact.farmmanagement.repository.WorkerRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkerService {

    private final WorkerRepository workerRepository;
    private final FarmContext farmContext;

    // ============ Create ============

    @Transactional
    public WorkerResponse create(WorkerRequest request) {
        Farm farm = farmContext.getCurrentFarm();
        String normalizedPhone = normalizePhone(request.getPhone());

        if (normalizedPhone != null
                && workerRepository.existsByFarmIdAndPhone(farm.getId(), normalizedPhone)) {
            throw new BusinessException("Worker with phone '" + normalizedPhone + "' already exists");
        }

        Worker worker = new Worker();
        worker.setFarm(farm);
        worker.setName(request.getName());
        worker.setPhone(normalizedPhone);
        worker.setAddress(request.getAddress());
        worker.setRole(request.getRole());
        worker.setMonthlySalary(request.getMonthlySalary());
        worker.setDailyWage(request.getDailyWage());
        worker.setHireDate(request.getHireDate());
        worker.setStatus(request.getStatus() != null ? request.getStatus() : Worker.WorkerStatus.ACTIVE);
        worker.setNotes(request.getNotes());

        return toResponse(workerRepository.save(worker));
    }

    // ============ Read ============

    @Transactional(readOnly = true)
    public List<WorkerResponse> getAll() {
        Long farmId = farmContext.getCurrentFarmId();
        return workerRepository.findByFarmId(farmId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkerResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<WorkerResponse> searchByName(String namePart) {
        Long farmId = farmContext.getCurrentFarmId();
        return workerRepository.findByFarmIdAndNameContainingIgnoreCase(farmId, namePart).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkerResponse> getByStatus(Worker.WorkerStatus status) {
        Long farmId = farmContext.getCurrentFarmId();
        return workerRepository.findByFarmIdAndStatus(farmId, status).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    // ============ Update ============

    @Transactional
    public WorkerResponse update(Long id, WorkerRequest request) {
        Long farmId = farmContext.getCurrentFarmId();
        Worker worker = findOrThrow(id);

        String normalizedPhone = normalizePhone(request.getPhone());
        if (normalizedPhone != null) {
            if (!normalizedPhone.equals(worker.getPhone())
                    && workerRepository.existsByFarmIdAndPhone(farmId, normalizedPhone)) {
                throw new BusinessException("Worker with phone '" + normalizedPhone + "' already exists");
            }
            worker.setPhone(normalizedPhone);
        } else {
            worker.setPhone(null);
        }

        worker.setName(request.getName());
        worker.setAddress(request.getAddress());
        worker.setRole(request.getRole());
        worker.setMonthlySalary(request.getMonthlySalary());
        worker.setDailyWage(request.getDailyWage());
        worker.setHireDate(request.getHireDate());
        if (request.getStatus() != null) worker.setStatus(request.getStatus());
        worker.setNotes(request.getNotes());

        return toResponse(workerRepository.save(worker));
    }

    // ============ Delete ============

    @Transactional
    public void delete(Long id) {
        Worker worker = findOrThrow(id);
        workerRepository.deleteById(worker.getId());
    }

    // ============ Helpers ============

    private Worker findOrThrow(Long id) {
        Long farmId = farmContext.getCurrentFarmId();
        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found with id: " + id));
        if (!worker.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Worker not found with id: " + id);
        }
        return worker;
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String trimmed = phone.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private WorkerResponse toResponse(Worker w) {
        WorkerResponse r = new WorkerResponse();
        r.setId(w.getId());
        r.setName(w.getName());
        r.setPhone(w.getPhone());
        r.setAddress(w.getAddress());
        r.setRole(w.getRole());
        r.setMonthlySalary(w.getMonthlySalary());
        r.setDailyWage(w.getDailyWage());
        r.setHireDate(w.getHireDate());
        r.setStatus(w.getStatus());
        r.setNotes(w.getNotes());
        r.setCreatedAt(w.getCreatedAt());
        return r;
    }
}