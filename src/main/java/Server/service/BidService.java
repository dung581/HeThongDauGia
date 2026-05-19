package Server.service;

import Common.DataBase.entities.Auction;
import Common.DataBase.entities.Bid;
import Common.DataBase.entities.Item;
import Common.DataBase.entities.Stake;
import Common.DataBase.repository.AuctionRepository;
import Common.DataBase.repository.BidRepository;
import Common.DataBase.repository.ItemRepository;
import Common.Enum.AuctionState;

import java.time.LocalDateTime;
import java.util.List;

import static Common.Enum.AuctionState.RUNNING;

public class BidService {

    private AuctionRepository auctionRepo = new AuctionRepository();
    private BidRepository bidRepo = new BidRepository();
    private ItemRepository itemRepo = new ItemRepository();
    private StakeService stakeService = new StakeService();
    private AutoBidService AutobidService = new AutoBidService();

    public synchronized Bid placeBid(long userId, long itemId, long price) {

        Auction auction = auctionRepo.getByItemId(itemId);
        if (auction == null) throw new RuntimeException("Auction not found");
        if (auction.getState() != RUNNING)
            throw new RuntimeException("Auction closed");

        Item item = itemRepo.getItemById(itemId);
        if (item == null) throw new RuntimeException("Item not found");
        long minIncrement = item.getMinIncrement() > 0 ? item.getMinIncrement() : 1L;
        long minAllowed = auction.getCurrent_price() + minIncrement;

        if (price < minAllowed) {
            throw new RuntimeException("Price must be at least " + minAllowed + " (step " + minIncrement + ")");
        }

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
        b.setCreated_at(LocalDateTime.now());
        bidRepo.saveBid(b);

        // 🔥 update auction
        auctionRepo.updateCurrentBid(auction.getId(), userId, price);

        // 🔥 auto bid
        AutobidService.trigger(itemId, price);
        // tu dong gia han thoi gian
        AutobidService.autoTime(auctionRepo.getById(auction.getId()));

        return b;
    }

    public List<Bid> getHistory(long itemId) {
        return bidRepo.getByItemId(itemId);
    }
}
