package Server.service;

import Common.DataBase.entities.Auction;
import Common.DataBase.entities.Bid;
import Common.DataBase.entities.Item;
import Common.DataBase.entities.Stake;
import Common.DataBase.repository.AuctionRepository;
import Common.DataBase.repository.BidRepository;
import Common.DataBase.repository.ItemRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static Common.Enum.AuctionState.RUNNING;

public class BidService {

    private static final Object BID_LOCK = new Object();

    private final AuctionRepository auctionRepo = new AuctionRepository();
    private final BidRepository bidRepo = new BidRepository();
    private final ItemRepository itemRepo = new ItemRepository();
    private final StakeService stakeService = new StakeService();
    private final AutoBidService autoBidService = new AutoBidService();

    public Bid placeBid(long userId, long itemId, long price) {
        synchronized (BID_LOCK) {
            return placeBidLocked(userId, itemId, price);
        }
    }

    private Bid placeBidLocked(long userId, long itemId, long price) {
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

        // Release stake cũ nếu đang có người dẫn trước.
        if (oldUser != 0) {
            Stake oldStake = stakeService.getActiveStake(auction.getId(), oldUser);
            if (oldStake != null) {
                stakeService.releaseStake(oldStake);
            }
        }

        // Tạo stake mới bằng auctionId đã có để tránh query lại auction.
        stakeService.createStakeForAuction(auction.getId(), userId, itemId, price);

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
        auction.setCurrent_user_id(userId);
        auction.setCurrent_price(price);

        // Auto bid được controller gọi ở task nền để bid thủ công không bị kẹt trong chuỗi auto bid dài.
        autoBidService.autoTime(auction);

        return b;
    }

    public List<Bid> getHistoryBySession(long sessionId) {
        List<Bid> bids = bidRepo.getBySessionId(sessionId);
        bids.sort(Comparator.comparingLong(Bid::getPrice).reversed());
        return bids;
    }
}
