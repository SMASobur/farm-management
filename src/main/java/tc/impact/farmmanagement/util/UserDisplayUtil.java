package tc.impact.farmmanagement.util;

import tc.impact.farmmanagement.entity.User;

public final class UserDisplayUtil {

    private UserDisplayUtil() {

    }
    public static String displayName(User user) {
        if (user == null) return null;
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        return user.getUsername();
    }
}