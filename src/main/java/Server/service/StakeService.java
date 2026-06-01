package Server.service;

import Common.DataBase.entities.Stake;
import Common.DataBase.repository.StakeRepository;
import Common.Enum.StakeStatus;

import java.util.List;

public class StakeService {

    private StakeRepository repo = new StakeRepository();
    private AccountService accountService = new AccountService();

    // Tạo stake khi caller đã có auctionId, tránh query lại auction trong luồng đặt giá.
    public Stake createStakeForAuction(long auctionId, long userId, long itemId, long amount) {
        accountService.lockFunds(userId, amount);

        Stake s = new Stake();
        s.setAution_id(auctionId);
        s.setLocked_item_id(itemId);
        s.setUser_id(userId);
        s.setAmount(amount);
        s.setStatus(StakeStatus.LOCKED);

        try {
            repo.saveStake(s);
        } catch (RuntimeException e) {
            accountService.releaseFunds(userId, amount);
            throw e;
        }

        return s;
    }

    public void releaseStake(long stakeId) {
        releaseStake(repo.getById(stakeId));
    }

    // Release trực tiếp object stake đã có sẵn để không cần query lại theo id.
    public void releaseStake(Stake s) {
        if (s == null) return;
        if (s.getStatus() != StakeStatus.LOCKED) return;

        accountService.releaseFunds(s.getUser_id(), s.getAmount());

        repo.updateStatus(s.getId(), StakeStatus.RELEASED);
    }

    public void markWon(long stakeId) {
        repo.updateStatus(stakeId, StakeStatus.WON);
    }

    // list lich dat gia
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
