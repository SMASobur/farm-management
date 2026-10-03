package tc.impact.farmmanagement.dto.response;

import lombok.Data;

@Data
public class AuthResponse {
    private String token;
    private String tokenType = "Bearer";
    private Long expiresInMs;
    private UserInfo user;

    @Data
    public static class UserInfo {
        private Long id;
        private String username;
        private String fullName;
        private String email;
        private String role;
        private Long farmId;
        private String farmName;
        private String farmCode;
    }
}