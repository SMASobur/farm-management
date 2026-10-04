package tc.impact.farmmanagement.dto.response;

import lombok.Data;
import tc.impact.farmmanagement.entity.Invite;
import tc.impact.farmmanagement.entity.User;

import java.time.LocalDateTime;

@Data
public class InviteResponse {
    private Long id;
    private String code;
    private String invitedName;
    private String invitedEmail;
    private User.Role role;
    private Invite.InviteStatus status;
    private String createdByName;
    private LocalDateTime expiresAt;
    private LocalDateTime acceptedAt;
    private String acceptedByName;
    private LocalDateTime createdAt;
    private boolean expired;
    private String shareableMessage;   // friendly share text
}