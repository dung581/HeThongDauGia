package Server.service;

import Common.DataBase.entities.*;
import Common.DataBase.repository.*;
import Common.DataBase.repository.RepoUseInService.UserAccountRepository;
import Common.Enum.AuctionState;
import Common.Enum.ItemStatus;
import Common.Model.user.UserAccount;

import java.util.List;

public class BidService {
    private AuctionRepository auctionRepo = new AuctionRepository();
    private BidRepository bidRepo = new BidRepository();
    private AccountRepository accountRepo = new AccountRepository();
    private StakeRepository stakeRepo = new StakeRepository();
    private UserAccountRepository userRepo = new UserAccountRepository();
    private AutoBidRepository autoBidRepo = new AutoBidRepository();

    // =========================
    // 1. PLACE BID
    // =========================
    public Bid placeBid(long userId, long itemId, long price) {

        Auction auction = auctionRepo.getByItemId(itemId);

        if (auction == null) {
            throw new RuntimeException("Auction not found");
        }

        if (auction.getState() != AuctionState.RUNNING) {
            throw new RuntimeException("Auction closed");
        }

        if (price <= auction.getCurrent_price()) {
            throw new RuntimeException("Price must be higher");
        }

        UserAccount bidder = userRepo.getUserAccount(userId);

        if (bidder.getAvailableBalance() < price) {
            throw new RuntimeException("Not enough money");
        }

        long oldUser = auction.getCurrent_user_id();
        long oldPrice = auction.getCurrent_price();

        // =====================
        // unlock thằng cũ
        // =====================
        if (oldUser != 0) {
            Account oldAcc = accountRepo.getAccountByUserId(oldUser);

            oldAcc.setBalance(oldAcc.getBalance() + oldPrice);
            oldAcc.setLocked_balance(oldAcc.getLocked_balance() - oldPrice);

            accountRepo.update(oldAcc);

            Stake oldStake = stakeRepo.getStakeByAuctionIdAndUserId(auction.getId(), oldUser);
            if (oldStake != null) {
                stakeRepo.updateStatus(oldStake.getId(), ItemStatus.LOST);
            }
        }

        // =====================
        // lock tiền thằng mới
        // =====================
        Account newAcc = accountRepo.getAccountByUserId(userId);

        newAcc.setBalance(newAcc.getBalance() - price);
        newAcc.setLocked_balance(newAcc.getLocked_balance() + price);

        accountRepo.update(newAcc);

        // =====================
        // tạo stake mới
        // =====================
        Stake s = new Stake();
        s.setAution_id(auction.getId());
        s.setUser_id(userId);
        s.setAmount(price);
        s.setStatus(ItemStatus.LOCKED);

        stakeRepo.saveStake(s);

        // =====================
        // insert bid
        // =====================
        Bid b = new Bid();
        b.setAuction_id(auction.getId());
        b.setUser_id(userId);
        b.setItem_id(itemId);
        b.setPrice(price);

        bidRepo.saveBid(b);

        // =====================
        // update auction
        // =====================
        auctionRepo.updateCurrentBid(auction.getId(), userId, price);

        // =====================
        // auto bid trigger
        // =====================
        processAutoBids(itemId, price);

        return b;
    }

    // =========================
    // 2. GET HISTORY
    // =========================
    public List<Bid> getHistory(long itemId) {
        return bidRepo.getByItemId(itemId);
    }

    // =========================
    // 3. PROCESS AUTO BIDS
    // =========================
    public void processAutoBids(long itemId, long newPrice) {

        List<Autobid> autos = autoBidRepo.getAllAutobid();

        Auction auction = auctionRepo.getByItemId(itemId);

        for (Autobid ab : autos) {

            if (!ab.is_active()) continue;

            if (ab.getItem_id() != itemId) continue;

            if (ab.getMax_price() <= newPrice) continue;

            if (ab.getUser_id() == auction.getCurrent_user_id()) continue;

            long nextPrice = newPrice + 1; // step đơn giản

            if (nextPrice <= ab.getMax_price()) {

                // recursive bid
                placeBid(ab.getUser_id(), itemId, nextPrice);
                break; // chỉ cần 1 thằng phản ứng
            }
        }
    }
}
