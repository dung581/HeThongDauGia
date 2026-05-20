package Server.service;

import Common.DataBase.entities.Account;
import Common.DataBase.entities.User;
import Common.DataBase.repository.AccountRepository;
import Common.DataBase.repository.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountService {

    private AccountRepository repo = new AccountRepository();
    private UserRepository userRepository = new UserRepository();

    public Account getBalance(long userId) {
        Account acc = repo.getAccountByUserId(userId);
        if (acc == null) throw new RuntimeException("Account not found");
        return acc;
    }

    public void deposit(long userId, long amount) {
        if (amount <= 0) throw new RuntimeException("Invalid amount");

        Account acc = repo.getAccountByUserId(userId);
        acc.setBalance(acc.getBalance() + amount);
        repo.update(acc);
    }

    public void lockFunds(long userId, long amount) {
        Account acc = repo.getAccountByUserId(userId);

        long available = acc.getBalance() - acc.getLocked_balance();
        if (available < amount) throw new RuntimeException("Not enough money");

        acc.setBalance(acc.getBalance() - amount);
        acc.setLocked_balance(acc.getLocked_balance() + amount);

        repo.update(acc);
    }

    public void releaseFunds(long userId, long amount) {
        Account acc = repo.getAccountByUserId(userId);

        acc.setLocked_balance(acc.getLocked_balance() - amount);
        acc.setBalance(acc.getBalance() + amount);

        repo.update(acc);
    }

    public long getAvailable(long userId) {
        Account acc = repo.getAccountByUserId(userId);
        return acc.getBalance() - acc.getLocked_balance();
    }

    public List<ManagedAccount> listManagedAccounts() {
        List<User> users = userRepository.getallUser();
        List<Account> accounts = repo.getAllAccount();
        Map<Long, Account> accountByUserId = new HashMap<>();
        for (Account account : accounts) {
            accountByUserId.put(account.getUser_id(), account);
        }

        List<ManagedAccount> rows = new ArrayList<>(users.size());
        for (User user : users) {
            Account account = accountByUserId.get(user.getId());
            long balance = account == null ? 0 : account.getBalance();
            long locked = account == null ? 0 : account.getLocked_balance();
            rows.add(new ManagedAccount(
                    user.getId(),
                    user.getUsername(),
                    user.getFullname(),
                    user.getRole() == null ? "" : user.getRole().name(),
                    user.getPassword(),
                    balance,
                    locked
            ));
        }
        return rows;
    }

    public static class ManagedAccount {
        private final Long userId;
        private final String username;
        private final String fullname;
        private final String role;
        private final String password;
        private final Long balance;
        private final Long lockedBalance;

        public ManagedAccount(Long userId, String username, String fullname, String role, String password, Long balance, Long lockedBalance) {
            this.userId = userId;
            this.username = username;
            this.fullname = fullname;
            this.role = role;
            this.password = password;
            this.balance = balance;
            this.lockedBalance = lockedBalance;
        }

        public Long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getFullname() {
            return fullname;
        }

        public String getRole() {
            return role;
        }

        public String getPassword() {
            return password;
        }

        public Long getBalance() {
            return balance;
        }

        public Long getLockedBalance() {
            return lockedBalance;
        }
    }
}
