package tc.impact.farmmanagement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tc.impact.farmmanagement.dto.request.LoginRequest;
import tc.impact.farmmanagement.dto.request.RegisterRequest;
import tc.impact.farmmanagement.dto.response.ApiResponse;
import tc.impact.farmmanagement.dto.response.AuthResponse;
import tc.impact.farmmanagement.entity.Farm;
import tc.impact.farmmanagement.entity.User;
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.repository.FarmRepository;
import tc.impact.farmmanagement.repository.UserRepository;
import tc.impact.farmmanagement.security.CustomUserPrincipal;
import tc.impact.farmmanagement.security.JwtService;
import tc.impact.farmmanagement.dto.request.ChangePasswordRequest;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;

import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final FarmRepository farmRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // ============ Login ============

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        CustomUserPrincipal principal = (CustomUserPrincipal) auth.getPrincipal();
        String token = jwtService.generateToken(principal);

        AuthResponse response = buildAuthResponse(principal, token);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    // ============ Register ============

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username '" + request.getUsername() + "' is already taken");
        }

        // Create a new farm for this owner
        String farmCode = generateFarmCode(request.getFarmName());
        if (farmRepository.existsByCode(farmCode)) {
            farmCode = farmCode + "-" + System.currentTimeMillis();
        }

        Farm farm = new Farm();
        farm.setCode(farmCode);
        farm.setName(request.getFarmName());
        farm.setLocation(request.getFarmLocation());
        farm.setOwnerName(request.getFullName());
        farm.setPlan(Farm.SubscriptionPlan.FREE);
        farm.setActive(true);
        Farm savedFarm = farmRepository.save(farm);

        // Create the owner user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setFarm(savedFarm);
        user.setRole(User.Role.OWNER);
        user.setActive(true);
        User savedUser = userRepository.save(user);

        CustomUserPrincipal principal = new CustomUserPrincipal(savedUser);
        String token = jwtService.generateToken(principal);

        AuthResponse response = buildAuthResponse(principal, token);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }

    // ============ Change Password ============

    @PostMapping("/change-password")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserPrincipal principal = (CustomUserPrincipal) auth.getPrincipal();
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }

        // Prevent reusing the same password
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    // ============ Me ============

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse.UserInfo>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserPrincipal principal = (CustomUserPrincipal) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(buildUserInfo(principal)));
    }

    // ============ Helpers ============

    private AuthResponse buildAuthResponse(CustomUserPrincipal principal, String token) {
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setExpiresInMs(jwtService.getExpirationMs());
        response.setUser(buildUserInfo(principal));
        return response;
    }

    private AuthResponse.UserInfo buildUserInfo(CustomUserPrincipal principal) {
        User u = principal.getUser();
        AuthResponse.UserInfo info = new AuthResponse.UserInfo();
        info.setId(u.getId());
        info.setUsername(u.getUsername());
        info.setFullName(u.getFullName());
        info.setEmail(u.getEmail());
        info.setRole(u.getRole().name());
        info.setFarmId(u.getFarm().getId());
        info.setFarmName(u.getFarm().getName());
        info.setFarmCode(u.getFarm().getCode());
        return info;
    }

    private String generateFarmCode(String farmName) {
        String base = farmName.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "-");
        if (base.length() > 40) base = base.substring(0, 40);
        return base.isEmpty() ? "FARM" : base;
    }
}