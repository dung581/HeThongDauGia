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
        return requireAccount(userId);
    }

    public void deposit(long userId, long amount) {
        if (amount <= 0) throw new RuntimeException("Invalid amount");

        Account acc = requireAccount(userId);
        acc.setBalance(acc.getBalance() + amount);
        repo.update(acc);
    }

    public void creditSaleProceeds(long sellerId, long amount) {
        if (amount <= 0) throw new RuntimeException("Invalid amount");

        Account acc = requireAccount(sellerId);
        acc.setBalance(acc.getBalance() + amount);
        repo.update(acc);
    }

    public void lockFunds(long userId, long amount) {
        Account acc = requireAccount(userId);

        long available = acc.getBalance() - acc.getLocked_balance();
        if (available < amount) throw new RuntimeException("Not enough money");

        acc.setLocked_balance(acc.getLocked_balance() + amount);

        repo.update(acc);
    }

    public void releaseFunds(long userId, long amount) {
        Account acc = requireAccount(userId);

        long releasable = Math.min(acc.getLocked_balance(), amount);
        acc.setLocked_balance(acc.getLocked_balance() - releasable);

        repo.update(acc);
    }

    public void deductLockedFunds(long userId, long amount) {
        Account acc = requireAccount(userId);

        if (acc.getBalance() < amount) {
            throw new RuntimeException("Balance is not enough");
        }

        long lockedToDeduct = Math.min(acc.getLocked_balance(), amount);
        acc.setLocked_balance(acc.getLocked_balance() - lockedToDeduct);
        acc.setBalance(acc.getBalance() - amount);

        repo.update(acc);
    }

    public long getAvailable(long userId) {
        Account acc = requireAccount(userId);
        return acc.getBalance() - acc.getLocked_balance();
    }

    private Account requireAccount(long userId) {
        Account acc = repo.getAccountByUserId(userId);
        if (acc == null) {
            User user = userRepository.getUserById(userId);
            if (user == null) {
                throw new RuntimeException("User not found for userId=" + userId);
            }

            Account created = new Account();
            created.setUser_id(userId);
            created.setBalance(0L);
            created.setLocked_balance(0L);
            repo.CreateAccount(created);

            acc = repo.getAccountByUserId(userId);
            if (acc == null) {
                throw new RuntimeException("Could not create account for userId=" + userId);
            }
        }
        return acc;
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
        private final Long balance;
        private final Long lockedBalance;

        public ManagedAccount(Long userId, String username, String fullname, String role, Long balance, Long lockedBalance) {
            this.userId = userId;
            this.username = username;
            this.fullname = fullname;
            this.role = role;
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

        public Long getBalance() {
            return balance;
        }

        public Long getLockedBalance() {
            return lockedBalance;
        }
    }
}
