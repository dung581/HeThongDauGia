package Server.service;

import Common.DataBase.entities.*;
import Common.DataBase.repository.AccountRepository;
import Common.DataBase.repository.AuctionRepository;
import Common.DataBase.repository.BidRepository;
import Common.DataBase.repository.RepoUseInService.UserAccountRepository;
import Common.DataBase.repository.StakeRepository;
import Common.Enum.AuctionState;

import Common.Enum.ItemStatus;
import Common.Model.user.UserAccount;
import Server.service.Exceptions.AuctionClosedException;
import Server.service.Exceptions.InvalidBidException;
import Server.service.Exceptions.NotEnoughMoneyException;
import Server.service.Exceptions.ReturnMessage;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
public class AuctionService{

    private AuctionRepository repo = new AuctionRepository();
    private StakeService stakeService = new StakeService();
    private AccountRepository accountRepo = new AccountRepository();

    public Auction createSession(long itemId, LocalDateTime endTime) {
        Auction a = new Auction();

        a.setItem_id(itemId);
        a.setCurrent_user_id(0);
        a.setCurrent_price(0);
        a.setStartTime(LocalDateTime.now());
        a.setEndTime(endTime);
        a.setState(AuctionState.RUNNING);

        repo.save(a);
        return a;
    }

    public List<Auction> getActive() {
        return repo.getActive();
    }

    public Auction getById(long id) {
        return repo.getById(id);
    }

    public void closeSession(long sessionId) {

        Auction auction = repo.getById(sessionId);

        long winnerId = auction.getCurrent_user_id();
        long price = auction.getCurrent_price();

        if (winnerId != 0) {

            Account acc = accountRepo.getAccountByUserId(winnerId);

            acc.setLocked_balance(acc.getLocked_balance() - price);
            accountRepo.update(acc);

            Stake s = stakeService.getActiveStake(sessionId, winnerId);
            if (s != null) {
                // winner
                stakeService.getActiveStake(sessionId, winnerId);
            }

            auction.setState(AuctionState.PAID);

        } else {
            auction.setState(AuctionState.CANCELED);
        }

        repo.update(auction);
    }

    public UserAccount declareWinner(long sessionId) {
        Auction a = repo.getById(sessionId);
        if (a.getCurrent_user_id() == 0) return null;

        return new UserAccountRepository().getUserAccount(a.getCurrent_user_id());
    }
}