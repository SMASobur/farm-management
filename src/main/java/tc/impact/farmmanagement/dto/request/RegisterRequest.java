package tc.impact.farmmanagement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100)
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @Size(max = 150)
    private String fullName;

    @Email(message = "Invalid email")
    @Size(max = 150)
    private String email;

    @Size(max = 20)
    private String phone;

    // ============ Farm creation ============

    @NotBlank(message = "Farm name is required")
    @Size(max = 150)
    private String farmName;

    @Size(max = 255)
    private String farmLocation;
}