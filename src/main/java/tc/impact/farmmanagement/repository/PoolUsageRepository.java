package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.PoolProductType;
import tc.impact.farmmanagement.entity.PoolUsage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PoolUsageRepository extends JpaRepository<PoolUsage, Long> {

    Optional<PoolUsage> findByFarmIdAndPoolSpeciesIdAndPoolProductTypeAndDate(
            Long farmId, Long poolSpeciesId, PoolProductType poolProductType, LocalDate date);

    List<PoolUsage> findByFarmIdAndDate(Long farmId, LocalDate date);

    List<PoolUsage> findByFarmIdAndPoolSpeciesIdAndPoolProductTypeAndDateBetween(
            Long farmId, Long poolSpeciesId, PoolProductType poolProductType,
            LocalDate from, LocalDate to);
}