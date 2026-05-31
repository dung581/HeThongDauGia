package Server.service;

import Common.DataBase.entities.*;
import Common.DataBase.repository.AuctionRepository;
import Common.DataBase.repository.ItemRepository;
import Common.DataBase.repository.RepoUseInService.UserAccountRepository;
import Common.Enum.AuctionState;

import Common.Enum.ItemStatus;
import Common.Model.user.UserAccount;

import java.time.LocalDateTime;
import java.util.List;
public class AuctionService{

    private final AuctionRepository repo = new AuctionRepository();
    private final StakeService stakeService = new StakeService();
    private final ItemRepository itemRepo= new ItemRepository();
    private final AccountService accountService = new AccountService();

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

    // Workflow Admin duyệt item PENDING và mở phiên đấu giá trong cùng một nghiệp vụ.
    public Auction approveAndCreateSession(long itemId, LocalDateTime endTime) {
        Item item = itemRepo.getItemById(itemId);
        if (item == null) throw new RuntimeException("Item not found");
        if (item.getStatus() != ItemStatus.PENDING) {
            throw new RuntimeException("Item must be PENDING to approve and start auction");
        }

        item.setStatus(ItemStatus.APPROVED);
        item.setMota("Da duyet");
        itemRepo.update(item);

        return createSession(itemId, endTime);
    }

    public List<Auction> getActive() {
        return repo.getActive();
    }

    public List<Auction> getAll() {
        return repo.getAll();
    }

    public Auction getById(long id) {
        return repo.getById(id);
    }

    public void closeSession(long sessionId) {

        Auction auction = repo.getById(sessionId);
        if (auction == null) {
            throw new RuntimeException("Auction not found");
        }
        if (auction.getState() != AuctionState.RUNNING) {
            return;
        }

        long winnerId = auction.getCurrent_user_id();
        long price = auction.getCurrent_price();

        Item item = itemRepo.getItemById(auction.getItem_id());
        if (item == null) {
            throw new RuntimeException("Item not found");
        }

        if (winnerId != 0) {

            // Trừ tiền đã khóa của người thắng, sau đó cộng đúng giá thắng cho seller.
            accountService.deductLockedFunds(winnerId, price);
            if (item.getOwner_user_id() > 0 && item.getOwner_user_id() != winnerId) {
                accountService.creditSaleProceeds(item.getOwner_user_id(), price);
            }

            Stake s = stakeService.getActiveStake(sessionId, winnerId);
            if (s != null) {
                stakeService.markWon(s.getId());
            }

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

    // Chỉ đóng phiên nếu thời gian kết thúc đã qua; controller dùng để không tự giữ rule hết hạn.
    public boolean closeIfExpired(long sessionId) {
        Auction auction = repo.getById(sessionId);
        if (auction == null) {
            throw new RuntimeException("Auction not found");
        }
        if (auction.getEndTime() == null || auction.getEndTime().isAfter(LocalDateTime.now())) {
            return false;
        }
        closeSession(sessionId);
        return true;
    }

    public UserAccount declareWinner(long sessionId) {
        Auction a = repo.getById(sessionId);
        if (a.getCurrent_user_id() == 0) return null;

        return new UserAccountRepository().getUserAccount(a.getCurrent_user_id());
    }
}
