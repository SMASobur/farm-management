package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT COALESCE(SUM(p.internalUse + p.fedToAnimals + p.wasted), 0) FROM PoolUsage p " +
            "WHERE p.farm.id = :farmId AND p.poolProductType = :type AND p.date <= :date")
    Double sumUsageUpToDate(@Param("farmId") Long farmId, @Param("type") PoolProductType type, @Param("date") LocalDate date);

    @Query("SELECT p FROM PoolUsage p " +
            "WHERE p.farm.id = :farmId AND p.poolSpecies.id = :speciesId " +
            "AND p.poolProductType = :type AND p.date <= :date " +
            "ORDER BY p.date DESC")
    List<PoolUsage> findLatestUsageUpToDate(
            @Param("farmId") Long farmId,
            @Param("speciesId") Long speciesId,
            @Param("type") PoolProductType type,
            @Param("date") LocalDate date);
}