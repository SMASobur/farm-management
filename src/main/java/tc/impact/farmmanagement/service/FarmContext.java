package tc.impact.farmmanagement.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import tc.impact.farmmanagement.entity.Farm;
import tc.impact.farmmanagement.entity.User;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
import tc.impact.farmmanagement.security.CustomUserPrincipal;


/**
 * Provides the "current farm" for the request.
 *
 * For now (pre-JWT), it defaults to the first active farm in the DB.
 * Once JWT auth is in place, this will read the farm from the SecurityContext.
 */
@Service
public class FarmContext {

    /**
     * Returns the current farm.
     * Pre-JWT: defaults to the first active farm.
     * Post-JWT: will read from SecurityContext.
     */
    public Farm getCurrentFarm() {
        CustomUserPrincipal principal = getPrincipal();
        return principal.getUser().getFarm();
    }

    public Long getCurrentFarmId() {
        return getPrincipal().getFarmId();
    }

    public User getCurrentUser() {
        CustomUserPrincipal principal = getPrincipal();
        return principal.getUser();
    }

    private CustomUserPrincipal getPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserPrincipal)) {
            throw new ResourceNotFoundException("No authenticated user");
        }
        return (CustomUserPrincipal) auth.getPrincipal();
    }
}