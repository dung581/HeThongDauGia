package Server.service;

import Common.DataBase.entities.*;
import Common.DataBase.repository.AccountRepository;
import Common.DataBase.repository.AuctionRepository;
import Common.DataBase.repository.ItemRepository;
import Common.DataBase.repository.RepoUseInService.UserAccountRepository;
import Common.Enum.AuctionState;

import Common.Enum.ItemStatus;
import Common.Model.user.UserAccount;

import java.time.LocalDateTime;
import java.util.List;
public class AuctionService{

    private AuctionRepository repo = new AuctionRepository();
    private StakeService stakeService = new StakeService();
    private AccountRepository accountRepo = new AccountRepository();
    private ItemRepository itemRepo= new ItemRepository();

    public Auction createSession(long itemId, LocalDateTime endTime) {

        Item item = itemRepo.getItemById(itemId);
        if (item == null) throw new RuntimeException("Item not found");

        if (item.getStatus() != ItemStatus.APPROVED) {
            throw new RuntimeException("Item must be APPROVED to start auction");
        }

        Auction a = new Auction();
        a.setItem_id(itemId);
        a.setStartTime(LocalDateTime.now());
        a.setEndTime(endTime);
        a.setCurrent_price(item.getBeginPrice());
        a.setState(AuctionState.RUNNING);

        repo.save(a);

        // 🔥 update item
        item.setStatus(ItemStatus.IN_AUCTION);
        itemRepo.update(item);

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

        // 🔥 lấy item
        Item item = itemRepo.getItemById(auction.getItem_id());

        if (winnerId != 0) {

            Account acc = accountRepo.getAccountByUserId(winnerId);

            acc.setLocked_balance(acc.getLocked_balance() - price);
            accountRepo.update(acc);

            Stake s = stakeService.getActiveStake(sessionId, winnerId);

            auction.setState(AuctionState.PAID);

            item.setStatus(ItemStatus.SOLD);

        } else {
            auction.setState(AuctionState.CANCELED);

            item.setStatus(ItemStatus.APPROVED);
        }

        repo.update(auction);

        // 🔥 update item
        itemRepo.update(item);
    }

    public UserAccount declareWinner(long sessionId) {
        Auction a = repo.getById(sessionId);
        if (a.getCurrent_user_id() == 0) return null;

        return new UserAccountRepository().getUserAccount(a.getCurrent_user_id());
    }
}