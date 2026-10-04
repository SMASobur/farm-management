package tc.impact.farmmanagement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tc.impact.farmmanagement.dto.request.AcceptInviteRequest;
import tc.impact.farmmanagement.dto.request.CreateInviteRequest;
import tc.impact.farmmanagement.dto.response.ApiResponse;
import tc.impact.farmmanagement.dto.response.AuthResponse;
import tc.impact.farmmanagement.dto.response.InviteResponse;
import tc.impact.farmmanagement.service.InviteService;

import java.util.List;

@RestController
@RequestMapping("/api/invites")
@RequiredArgsConstructor
public class InviteController {

    private final InviteService inviteService;

    // ============ Authenticated endpoints (OWNER/ADMIN) ============

    @PostMapping
    public ResponseEntity<ApiResponse<InviteResponse>> create(@Valid @RequestBody CreateInviteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Invite created", inviteService.createInvite(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InviteResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(inviteService.listInvites()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<InviteResponse>> revoke(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Invite revoked", inviteService.revokeInvite(id)));
    }
}