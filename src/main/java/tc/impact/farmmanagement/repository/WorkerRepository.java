package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.Worker;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, Long> {

    List<Worker> findByFarmId(Long farmId);

    List<Worker> findByFarmIdAndStatus(Long farmId, Worker.WorkerStatus status);

    List<Worker> findByFarmIdAndNameContainingIgnoreCase(Long farmId, String namePart);

    Optional<Worker> findByFarmIdAndPhone(Long farmId, String phone);

    boolean existsByFarmIdAndPhone(Long farmId, String phone);

    long countByFarmIdAndStatus(Long farmId, Worker.WorkerStatus status);
}