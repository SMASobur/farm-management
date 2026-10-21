package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.MilkProduction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MilkProductionRepository extends JpaRepository<MilkProduction, Long> {

    List<MilkProduction> findByFarmId(Long farmId);
    List<MilkProduction> findByFarmIdAndAnimalId(Long farmId, Long animalId);
    List<MilkProduction> findByFarmIdAndDate(Long farmId, LocalDate date);
    List<MilkProduction> findByFarmIdAndDateBetweenOrderByDateAsc(Long farmId, LocalDate from, LocalDate to);
    Optional<MilkProduction> findByFarmIdAndAnimalIdAndDate(Long farmId, Long animalId, LocalDate date);
    boolean existsByFarmIdAndAnimalIdAndDate(Long farmId, Long animalId, LocalDate date);

    @Query("SELECT COALESCE(SUM(m.totalLiters), 0.0) FROM MilkProduction m " +
            "WHERE m.farm.id = :farmId AND m.date = :date")
    Double sumLitersByFarmAndDate(@Param("farmId") Long farmId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(m.totalLiters), 0) FROM MilkProduction m " +
            "WHERE m.farm.id = :farmId AND m.date <= :date")
    Double sumTotalLitersUpToDate(@Param("farmId") Long farmId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(m.totalLiters), 0) FROM MilkProduction m " +
            "WHERE m.farm.id = :farmId AND m.poolSpecies.id = :speciesId AND m.date <= :date")
    Double sumLitersBySpeciesUpToDate(@Param("farmId") Long farmId, @Param("speciesId") Long speciesId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(m.totalLiters), 0.0) FROM MilkProduction m " +
            "WHERE m.farm.id = :farmId AND m.date BETWEEN :from AND :to")
    Double sumLitersByFarmBetween(@Param("farmId") Long farmId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT m.date, SUM(m.totalLiters) FROM MilkProduction m " +
            "WHERE m.farm.id = :farmId AND m.date BETWEEN :from AND :to " +
            "GROUP BY m.date ORDER BY m.date ASC")
    List<Object[]> dailyTotalsBetween(@Param("farmId") Long farmId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT DISTINCT m.animal.id FROM MilkProduction m " +
            "WHERE m.farm.id = :farmId AND m.date >= :since")
    List<Long> findAnimalIdsWithMilkSince(@Param("farmId") Long farmId, @Param("since") LocalDate since);

    @Query("SELECT COALESCE(SUM(m.totalLiters), 0.0) FROM MilkProduction m " +
            "WHERE m.animal.id = :animalId AND m.date = :date")
    Double sumLitersByAnimalAndDate(@Param("animalId") Long animalId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(m.totalLiters), 0.0) FROM MilkProduction m " +
            "WHERE m.animal.id = :animalId AND m.date BETWEEN :from AND :to")
    Double sumLitersByAnimalBetween(@Param("animalId") Long animalId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(m.totalLiters), 0.0) FROM MilkProduction m " +
            "WHERE m.farm.id = :farmId AND m.poolSpecies.id = :speciesId AND m.date = :date")
    Double sumLitersByFarmAndSpeciesAndDate(@Param("farmId") Long farmId, @Param("speciesId") Long speciesId, @Param("date") LocalDate date);

    List<MilkProduction> findByAnimalIdAndDateBetweenOrderByDateAsc(Long animalId, LocalDate from, LocalDate to);
}