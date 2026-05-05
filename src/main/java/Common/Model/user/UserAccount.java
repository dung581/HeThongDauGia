package Common.Model.user;

import Common.Enum.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserAccount {
    private long userId;
    private String fullname;
    private long balance;
    private long lockedBalance;

    private static long currentUserId;
    private static String currentUsername;
    private static String currentFullname;
    private static UserRole currentRole;

    public long getAvailableBalance() {
        return balance - lockedBalance;
    }

    public static void setSession(long userId, String username, String fullname, UserRole role) {
        currentUserId = userId;
        currentUsername = username;
        currentFullname = fullname;
        currentRole = role;
    }

    public static void clearSession() {
        currentUserId = 0L;
        currentUsername = null;
        currentFullname = null;
        currentRole = null;
    }

    public static long getUserId() {
        return currentUserId;
    }

    public static String getCurrentUsername() {
        return currentUsername;
    }

    public static String getCurrentFullname() {
        return currentFullname;
    }

    public static UserRole getCurrentRole() {
        return currentRole;
    }
}
