package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tc.impact.farmmanagement.entity.Farm;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
import tc.impact.farmmanagement.repository.FarmRepository;

import java.util.Optional;

/**
 * Provides the "current farm" for the request.
 *
 * For now (pre-JWT), it defaults to the first active farm in the DB.
 * Once JWT auth is in place, this will read the farm from the SecurityContext.
 */
@Service
@RequiredArgsConstructor
public class FarmContext {

    private final FarmRepository farmRepository;

    /**
     * Returns the current farm.
     * Pre-JWT: defaults to the first active farm.
     * Post-JWT: will read from SecurityContext.
     */
    public Farm getCurrentFarm() {
        return getCurrentFarmOptional()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No farm found. Please create a farm or check FarmSeeder."));
    }

    public Optional<Farm> getCurrentFarmOptional() {
        return farmRepository.findAll().stream()
                .filter(Farm::isActive)
                .findFirst();
    }

    public Long getCurrentFarmId() {
        return getCurrentFarm().getId();
    }
}