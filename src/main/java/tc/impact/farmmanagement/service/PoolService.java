package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.request.PoolUsageRequest;
import tc.impact.farmmanagement.dto.response.PoolStatusResponse;
import tc.impact.farmmanagement.dto.response.PoolUsageResponse;
import tc.impact.farmmanagement.entity.*;
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
import tc.impact.farmmanagement.repository.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PoolService {

    private final FarmContext farmContext;
    private final SpeciesRepository speciesRepository;
    private final MilkProductionRepository milkRepository;
    private final EggProductionRepository eggRepository;
    private final SaleRepository saleRepository;
    private final PoolUsageRepository poolUsageRepository;

    // ============ Pool Availability ============

    /**
     * Compute what's currently available in a pool today.
     * Available = produced - sold - internal - fed - wasted
     */
    @Transactional(readOnly = true)
    public double getAvailableStock(Long farmId, Long poolSpeciesId,
                                    PoolProductType poolProductType, LocalDate date) {
        double produced = getProduced(farmId, poolSpeciesId, poolProductType, date);
        double sold = getSold(farmId, poolSpeciesId, poolProductType, date);
        PoolUsage usage = poolUsageRepository
                .findByFarmIdAndPoolSpeciesIdAndPoolProductTypeAndDate(
                        farmId, poolSpeciesId, poolProductType, date)
                .orElse(null);
        double used = usage != null
                ? (usage.getInternalUse() + usage.getFedToAnimals() + usage.getWasted())
                : 0.0;
        return produced - sold - used;
    }

    private double getProduced(Long farmId, Long poolSpeciesId,
                               PoolProductType productType, LocalDate date) {
        if (productType == PoolProductType.MILK) {
            Double sum = milkRepository.sumLitersByFarmAndSpeciesAndDate(farmId, poolSpeciesId, date);
            return sum != null ? sum : 0.0;
        }
        if (productType == PoolProductType.EGG) {
            Long sum = eggRepository.sumEggsByFarmAndSpeciesAndDate(farmId, poolSpeciesId, date);
            return sum != null ? sum : 0.0;
        }
        return 0.0;  // FISH/CROP not yet supported
    }

    private double getSold(Long farmId, Long poolSpeciesId,
                           PoolProductType productType, LocalDate date) {
        Double sum = saleRepository.sumQuantityByPool(
                farmId, poolSpeciesId, productType, date);
        return sum != null ? sum : 0.0;
    }

    // ============ List pools for a farm ============

    @Transactional(readOnly = true)
    public List<PoolStatusResponse> getPoolsStatus(Long farmId, LocalDate date) {
        List<Species> allSpecies = speciesRepository
                .findByFarmIdOrderByDisplayOrderAscNameAsc(farmId);

        List<PoolStatusResponse> result = new ArrayList<>();

        for (Species sp : allSpecies) {
            if (sp.isMilkProducer()) {
                result.add(buildStatus(farmId, sp, PoolProductType.MILK, date));
            }
            if (sp.isEggProducer()) {
                result.add(buildStatus(farmId, sp, PoolProductType.EGG, date));
            }
        }

        // Filter out pools with zero activity (produced + sold + usage = 0)
        return result.stream()
                .filter(p -> p.getProduced() > 0 || p.getSold() > 0
                        || p.getInternalUse() > 0 || p.getFedToAnimals() > 0
                        || p.getWasted() > 0)
                .collect(Collectors.toList());
    }

    private PoolStatusResponse buildStatus(Long farmId, Species sp,
                                           PoolProductType productType, LocalDate date) {
        PoolStatusResponse r = new PoolStatusResponse();
        r.setPoolSpeciesId(sp.getId());
        r.setPoolSpeciesCode(sp.getCode());
        r.setPoolSpeciesName(sp.getName());
        r.setPoolProductType(productType);
        r.setPoolProductTypeLabel(buildLabel(sp, productType));
        r.setDate(date);

        double produced = getProduced(farmId, sp.getId(), productType, date);
        double sold = getSold(farmId, sp.getId(), productType, date);

        Optional<PoolUsage> usageOpt = poolUsageRepository
                .findByFarmIdAndPoolSpeciesIdAndPoolProductTypeAndDate(
                        farmId, sp.getId(), productType, date);

        double internalUse = usageOpt.map(PoolUsage::getInternalUse).orElse(0.0);
        double fed = usageOpt.map(PoolUsage::getFedToAnimals).orElse(0.0);
        double wasted = usageOpt.map(PoolUsage::getWasted).orElse(0.0);

        r.setProduced(produced);
        r.setSold(sold);
        r.setInternalUse(internalUse);
        r.setFedToAnimals(fed);
        r.setWasted(wasted);
        r.setAvailable(produced - sold - internalUse - fed - wasted);

        return r;
    }

    private String buildLabel(Species sp, PoolProductType type) {
        if (type == PoolProductType.MILK) return sp.getName() + " Milk";
        if (type == PoolProductType.EGG) return sp.getName() + " Eggs";
        return sp.getName() + " " + type.name();
    }

    // ============ Record usage ============

    @Transactional
    public PoolUsageResponse recordUsage(PoolUsageRequest request) {
        Farm farm = farmContext.getCurrentFarm();
        Long farmId = farm.getId();

        Species sp = speciesRepository.findById(request.getPoolSpeciesId())
                .orElseThrow(() -> new ResourceNotFoundException("Species not found"));
        if (!sp.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Species not found");
        }

        if (request.getPoolProductType() == PoolProductType.MILK && !sp.isMilkProducer()) {
            throw new BusinessException("Species '" + sp.getName() + "' does not produce milk");
        }
        if (request.getPoolProductType() == PoolProductType.EGG && !sp.isEggProducer()) {
            throw new BusinessException("Species '" + sp.getName() + "' does not produce eggs");
        }

        // Check if usage exists — update or create
        Optional<PoolUsage> existing = poolUsageRepository
                .findByFarmIdAndPoolSpeciesIdAndPoolProductTypeAndDate(
                        farmId, sp.getId(), request.getPoolProductType(), request.getDate());

        PoolUsage usage;
        if (existing.isPresent()) {
            usage = existing.get();
        } else {
            usage = new PoolUsage();
            usage.setFarm(farm);
            usage.setPoolSpecies(sp);
            usage.setPoolProductType(request.getPoolProductType());
            usage.setDate(request.getDate());
        }

        usage.setInternalUse(safe(request.getInternalUse()));
        usage.setFedToAnimals(safe(request.getFedToAnimals()));
        usage.setWasted(safe(request.getWasted()));
        usage.setNotes(request.getNotes());
        usage.setEnteredBy(farmContext.getCurrentUser());

        // Validate total usage <= produced - sold
        double totalUsage = usage.getInternalUse() + usage.getFedToAnimals() + usage.getWasted();
        double produced = getProduced(farmId, sp.getId(), request.getPoolProductType(), request.getDate());
        double sold = getSold(farmId, sp.getId(), request.getPoolProductType(), request.getDate());
        double maxUsable = produced - sold;
        if (totalUsage > maxUsable) {
            throw new BusinessException(String.format(
                    "Total usage (%.2f) exceeds available (%.2f = produced %.2f - sold %.2f)",
                    totalUsage, maxUsable, produced, sold));
        }

        return toUsageResponse(poolUsageRepository.save(usage));
    }

    private PoolUsageResponse toUsageResponse(PoolUsage u) {
        PoolUsageResponse r = new PoolUsageResponse();
        r.setId(u.getId());
        r.setPoolSpeciesId(u.getPoolSpecies().getId());
        r.setPoolSpeciesName(u.getPoolSpecies().getName());
        r.setPoolProductType(u.getPoolProductType());
        r.setDate(u.getDate());
        r.setInternalUse(u.getInternalUse());
        r.setFedToAnimals(u.getFedToAnimals());
        r.setWasted(u.getWasted());
        r.setNotes(u.getNotes());
        r.setCreatedAt(u.getCreatedAt());
        return r;
    }

    private Double safe(Double value) {
        return value != null ? value : 0.0;
    }
}