package Server.service;

import Common.DataBase.entities.Auction;
import Common.DataBase.entities.Autobid;
import Common.DataBase.entities.Bid;
import Common.DataBase.entities.Stake;
import Common.DataBase.repository.AuctionRepository;
import Common.DataBase.repository.AutoBidRepository;
import Common.DataBase.repository.BidRepository;
import Common.DataBase.repository.StakeRepository;
import Common.Enum.AuctionState;
import Common.Enum.ItemStatus;

import java.util.List;
public class BidService {

    private AuctionRepository auctionRepo = new AuctionRepository();
    private BidRepository bidRepo = new BidRepository();
    private StakeService stakeService = new StakeService();
    private AutoBidService AutobidService = new AutoBidService();

    public synchronized Bid placeBid(long userId, long itemId, long price) {

        Auction auction = auctionRepo.getByItemId(itemId);

        if (auction == null) throw new RuntimeException("Auction not found");
        if (auction.getState() != AuctionState.RUNNING)
            throw new RuntimeException("Auction closed");

        if (price <= auction.getCurrent_price())
            throw new RuntimeException("Price must be higher");

        long oldUser = auction.getCurrent_user_id();

        // 🔥 release thằng cũ
        if (oldUser != 0) {
            Stake oldStake = stakeService.getActiveStake(auction.getId(), oldUser);
            if (oldStake != null) {
                stakeService.releaseStake(oldStake.getId());
            }
        }

        // 🔥 tạo stake mới (lock tiền)
        stakeService.createStake(userId, itemId, price);

        // 🔥 lưu bid
        Bid b = new Bid();
        b.setAuction_id(auction.getId());
        b.setItem_id(itemId);
        b.setUser_id(userId);
        b.setPrice(price);
        bidRepo.saveBid(b);

        // 🔥 update auction
        auctionRepo.updateCurrentBid(auction.getId(), userId, price);

        // 🔥 auto bid
        AutobidService.trigger(itemId, price);

        return b;
    }

    public List<Bid> getHistory(long itemId) {
        return bidRepo.getByItemId(itemId);
    }
}