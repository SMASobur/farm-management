package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.request.AcceptInviteRequest;
import tc.impact.farmmanagement.dto.request.CreateInviteRequest;
import tc.impact.farmmanagement.dto.response.AuthResponse;
import tc.impact.farmmanagement.dto.response.InviteResponse;
import tc.impact.farmmanagement.entity.Farm;
import tc.impact.farmmanagement.entity.Invite;
import tc.impact.farmmanagement.entity.User;
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
import tc.impact.farmmanagement.repository.FarmRepository;
import tc.impact.farmmanagement.repository.InviteRepository;
import tc.impact.farmmanagement.repository.UserRepository;
import tc.impact.farmmanagement.security.CustomUserPrincipal;
import tc.impact.farmmanagement.security.JwtService;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InviteService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I
    private static final int RANDOM_PART_LENGTH = 6;
    private static final int DEFAULT_EXPIRY_DAYS = 7;
    private static final int MAX_EXPIRY_DAYS = 30;

    private final InviteRepository inviteRepository;
    private final UserRepository userRepository;
    private final FarmRepository farmRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecureRandom random = new SecureRandom();

    // ============ Create Invite ============

    @Transactional
    public InviteResponse createInvite(CreateInviteRequest request) {
        CustomUserPrincipal principal = currentPrincipal();
        User creator = principal.getUser();
        Farm farm = creator.getFarm();

        // Only OWNER and ADMIN can invite
        if (creator.getRole() != User.Role.OWNER && creator.getRole() != User.Role.ADMIN) {
            throw new BusinessException("Only OWNER or ADMIN can create invites");
        }

        // Only OWNER can invite other ADMINs
        if (request.getRole() == User.Role.ADMIN && creator.getRole() != User.Role.OWNER) {
            throw new BusinessException("Only OWNER can invite ADMIN users");
        }

        // Cannot invite OWNER (only one owner per farm)
        if (request.getRole() == User.Role.OWNER) {
            throw new BusinessException("Cannot invite another OWNER. There can be only one OWNER per farm.");
        }

        int expiryDays = request.getExpiryDays() != null ? request.getExpiryDays() : DEFAULT_EXPIRY_DAYS;
        if (expiryDays < 1 || expiryDays > MAX_EXPIRY_DAYS) {
            throw new BusinessException("Expiry must be between 1 and " + MAX_EXPIRY_DAYS + " days");
        }

        String code = generateUniqueCode(farm);

        Invite invite = new Invite();
        invite.setFarm(farm);
        invite.setCreatedBy(creator);
        invite.setCode(code);
        invite.setInvitedName(request.getInvitedName());
        invite.setInvitedEmail(request.getInvitedEmail());
        invite.setRole(request.getRole());
        invite.setStatus(Invite.InviteStatus.PENDING);
        invite.setExpiresAt(LocalDateTime.now().plusDays(expiryDays));

        Invite saved = inviteRepository.save(invite);
        return toResponse(saved);
    }

    // ============ List Invites ============

    @Transactional(readOnly = true)
    public List<InviteResponse> listInvites() {
        Long farmId = currentPrincipal().getFarmId();
        return inviteRepository.findByFarmIdOrderByCreatedAtDesc(farmId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ============ Revoke Invite ============

    @Transactional
    public InviteResponse revokeInvite(Long inviteId) {
        CustomUserPrincipal principal = currentPrincipal();
        Invite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));

        if (!invite.getFarm().getId().equals(principal.getFarmId())) {
            throw new ResourceNotFoundException("Invite not found");
        }

        if (invite.getStatus() != Invite.InviteStatus.PENDING) {
            throw new BusinessException("Only PENDING invites can be revoked");
        }

        invite.setStatus(Invite.InviteStatus.REVOKED);
        return toResponse(inviteRepository.save(invite));
    }

    // ============ Accept Invite (public) ============

    @Transactional
    public AuthResponse acceptInvite(AcceptInviteRequest request) {
        Invite invite = inviteRepository.findByCode(request.getCode().toUpperCase())
                .orElseThrow(() -> new BusinessException("Invalid invite code"));

        if (!invite.isUsable()) {
            if (invite.getStatus() == Invite.InviteStatus.REVOKED) {
                throw new BusinessException("This invite has been revoked");
            }
            if (invite.getStatus() == Invite.InviteStatus.ACCEPTED) {
                throw new BusinessException("This invite has already been used");
            }
            if (invite.isExpired()) {
                throw new BusinessException("This invite has expired");
            }
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username '" + request.getUsername() + "' is already taken");
        }

        // Create the user in the invite's farm
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName() != null ? request.getFullName() : invite.getInvitedName());
        user.setEmail(request.getEmail() != null ? request.getEmail() : invite.getInvitedEmail());
        user.setPhone(request.getPhone());
        user.setFarm(invite.getFarm());
        user.setRole(invite.getRole());
        user.setActive(true);
        User savedUser = userRepository.save(user);

        // Mark invite as accepted
        invite.setStatus(Invite.InviteStatus.ACCEPTED);
        invite.setAcceptedAt(LocalDateTime.now());
        invite.setAcceptedBy(savedUser);
        inviteRepository.save(invite);

        // Generate JWT
        CustomUserPrincipal principal = new CustomUserPrincipal(savedUser);
        String token = jwtService.generateToken(principal);

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setExpiresInMs(jwtService.getExpirationMs());
        response.setUser(toUserInfo(savedUser));
        return response;
    }

    // ============ Helpers ============

    private CustomUserPrincipal currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (CustomUserPrincipal) auth.getPrincipal();
    }

    private String generateUniqueCode(Farm farm) {
        String prefix = generateFarmPrefix(farm.getCode());
        for (int attempt = 0; attempt < 10; attempt++) {
            String randomPart = randomString(RANDOM_PART_LENGTH);
            String code = prefix + "-" + randomPart;
            if (!inviteRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new BusinessException("Failed to generate unique invite code");
    }

    private String generateFarmPrefix(String farmCode) {
        String cleaned = farmCode.replaceAll("[^A-Z0-9]", "");
        return cleaned.length() > 6 ? cleaned.substring(0, 6) : cleaned;
    }

    private String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    private InviteResponse toResponse(Invite invite) {
        InviteResponse r = new InviteResponse();
        r.setId(invite.getId());
        r.setCode(invite.getCode());
        r.setInvitedName(invite.getInvitedName());
        r.setInvitedEmail(invite.getInvitedEmail());
        r.setRole(invite.getRole());
        r.setStatus(invite.getStatus());
        r.setCreatedByName(invite.getCreatedBy().getUsername());
        r.setExpiresAt(invite.getExpiresAt());
        r.setAcceptedAt(invite.getAcceptedAt());
        if (invite.getAcceptedBy() != null) {
            r.setAcceptedByName(invite.getAcceptedBy().getUsername());
        }
        r.setCreatedAt(invite.getCreatedAt());
        r.setExpired(invite.isExpired());

        // Build shareable message
        String farmName = invite.getFarm().getName();
        r.setShareableMessage(
                "You have been invited to join '" + farmName + "' on Farm Management.\n" +
                        "Use this code to join: " + invite.getCode() + "\n" +
                        "Expires on: " + invite.getExpiresAt().toLocalDate()
        );

        return r;
    }

    private AuthResponse.UserInfo toUserInfo(User user) {
        AuthResponse.UserInfo info = new AuthResponse.UserInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setFullName(user.getFullName());
        info.setEmail(user.getEmail());
        info.setRole(user.getRole().name());
        info.setFarmId(user.getFarm().getId());
        info.setFarmName(user.getFarm().getName());
        info.setFarmCode(user.getFarm().getCode());
        return info;
    }
}