package Server.service;

import Common.DataBase.DbConnection;
import Common.DataBase.entities.Account;
import Common.DataBase.repository.AccountRepository;

import java.sql.Connection;

public class AccountService {

    private AccountRepository repo = new AccountRepository();

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
}