package Server.service;

import Common.DataBase.entities.Account;
import Common.DataBase.repository.AccountRepository;

public class AccountService {

    private AccountRepository accountRepo = new AccountRepository();

    // =========================
    // 1. GET BALANCE (full info)
    // =========================
    public Account getBalance(long userId) {
        Account acc = accountRepo.getAccountByUserId(userId);

        if (acc == null) {
            throw new RuntimeException("Account not found");
        }

        return acc;
    }

    // =========================
    // 2. DEPOSIT
    // =========================
    public void deposit(long userId, long amount) {

        if (amount <= 0) {
            throw new RuntimeException("Invalid amount");
        }

        Account acc = accountRepo.getAccountByUserId(userId);

        if (acc == null) {
            throw new RuntimeException("Account not found");
        }

        acc.setBalance(acc.getBalance() + amount);

        accountRepo.update(acc);
    }

    // =========================
    // 3. LOCK FUNDS (khi bid)
    // =========================
    public void lockFunds(long userId, long amount) {

        if (amount <= 0) {
            throw new RuntimeException("Invalid amount");
        }

        Account acc = accountRepo.getAccountByUserId(userId);

        if (acc == null) {
            throw new RuntimeException("Account not found");
        }

        long available = acc.getBalance() - acc.getLocked_balance();

        if (available < amount) {
            throw new RuntimeException("Not enough available balance");
        }

        acc.setBalance(acc.getBalance() - amount);
        acc.setLocked_balance(acc.getLocked_balance() + amount);

        accountRepo.update(acc);
    }

    // =========================
    // 4. RELEASE FUNDS (khi bị outbid)
    // =========================
    public void releaseFunds(long userId, long amount) {

        if (amount <= 0) {
            throw new RuntimeException("Invalid amount");
        }

        Account acc = accountRepo.getAccountByUserId(userId);

        if (acc == null) {
            throw new RuntimeException("Account not found");
        }

        if (acc.getLocked_balance() < amount) {
            throw new RuntimeException("Locked balance not enough");
        }

        acc.setLocked_balance(acc.getLocked_balance() - amount);
        acc.setBalance(acc.getBalance() + amount);

        accountRepo.update(acc);
    }

    // =========================
    // 5. GET AVAILABLE
    // =========================
    public long getAvailable(long userId) {
        Account acc = accountRepo.getAccountByUserId(userId);

        if (acc == null) {
            throw new RuntimeException("Account not found");
        }

        return acc.getBalance() - acc.getLocked_balance();
    }
}