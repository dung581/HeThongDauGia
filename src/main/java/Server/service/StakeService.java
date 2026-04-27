package Server.service;
import Common.DataBase.entities.Auction;
import Common.DataBase.entities.Stake;
import Common.DataBase.repository.AuctionRepository;
import Common.DataBase.repository.StakeRepository;
import Common.Enum.StakeStatus;
import java.util.List;

public class StakeService {

    private StakeRepository repo = new StakeRepository();
    private AuctionRepository auctionRepo = new AuctionRepository();
    private AccountService accountService = new AccountService();

    public Stake createStake(long userId, long itemId, long amount) {

        Auction auction = auctionRepo.getByItemId(itemId);

        accountService.lockFunds(userId, amount);

        Stake s = new Stake();
        s.setAution_id(auction.getId());
        s.setUser_id(userId);
        s.setAmount(amount);
        s.setStatus(StakeStatus.LOCKED);

        repo.saveStake(s);

        return s;
    }

    public void releaseStake(long stakeId) {

        Stake s = repo.getById(stakeId);
        if (s == null) return;

        accountService.releaseFunds(s.getUser_id(), s.getAmount());

        repo.updateStatus(stakeId, StakeStatus.LOST);
    }

    public List<Stake> getUserStakes(long userId) {
        return repo.getByUserId(userId);
    }

    // 🔥 helper cực quan trọng
    public Stake getActiveStake(long auctionId, long userId) {
        return repo.getByAuctionIdAndUserIdAndStatus(
                auctionId, userId, StakeStatus.LOCKED
        );
    }
}