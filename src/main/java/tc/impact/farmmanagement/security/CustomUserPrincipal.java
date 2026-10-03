package tc.impact.farmmanagement.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import tc.impact.farmmanagement.entity.User;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security principal.
 * Carries the User entity + farm ID + role, so controllers/services can
 * always determine "which farm" is making the request.
 */
@Getter
@AllArgsConstructor
public class CustomUserPrincipal implements UserDetails {

    private final User user;

    public Long getUserId() {
        return user.getId();
    }

    public Long getFarmId() {
        return user.getFarm().getId();
    }

    public String getRole() {
        return user.getRole().name();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}