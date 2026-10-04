package tc.impact.farmmanagement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tc.impact.farmmanagement.entity.User;

@Data
public class CreateInviteRequest {

    @Size(max = 150)
    private String invitedName;

    @Email(message = "Invalid email")
    @Size(max = 150)
    private String invitedEmail;

    @NotNull(message = "Role is required")
    private User.Role role;

    /**
     * Days until expiry. Defaults to 7 if not provided.
     * Max 30.
     */
    private Integer expiryDays;
}