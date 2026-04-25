package Common.Model.user;

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

        // logic tiện dùng
        public long getAvailableBalance() {
            return balance - lockedBalance;
        }
    }
