package Server.core;

import Common.Enum.AuctionState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import Common.Model.BidTransaction;
import Common.Model.Item;
import Common.Model.User;
import Exceptions.AuctionClosedException;
import Exceptions.InvalidBidException;
import Exceptions.NotEnoughMoneyException;

/**
 * Một phiên đấu giá cụ thể cho 1 Item.
 *
 * FIX so với bản cũ:
 *   1. Bỏ field currentPrice trùng với item.currentPrice (luôn out-of-sync).
 *      Giờ getCurrentPrice() delegate sang item.getCurrentPrice().
 *   2. Cập nhật BidTransaction constructor mới (có thêm Item).
 *   3. Unmodifiable getter cho bidTransactionHistory để tránh bên ngoài sửa list.
 */
public class Auction {

    private static final String[] SUCCESS_MESSAGES = {
            " đang dẫn đầu cuộc đấu giá!🔥🔥",
            " quá vjpppppp prooo! Giá này chưa ai vượt qua! 🎉🎉",
            " vừa chiếm vị trí số 1! 🚀🚀🚀",
            " đang thống trị phiên đấu giá! 👑👑"
    };

    private final String auctionID;
    private final Item item;
    private final List<BidTransaction> bidTransactionHistory;
    private User currentWinner;
    private AuctionState state = AuctionState.OPEN;

    private static final Random RANDOM = new Random();

    public Auction(String auctionID, Item item) {
        this.auctionID = auctionID;
        this.item = item;
        this.bidTransactionHistory = new ArrayList<>();
        this.currentWinner = null;
    }

    public AuctionState getState() {
        return state;
    }

    public void setState(AuctionState state) {
        this.state = state;
    }

    /**
     * Đặt giá. synchronized để tránh nhiều bidder update cùng lúc
     * (concurrency requirement của đề bài).
     */
    public synchronized void placeBid(User bidder, long bidAmount)
            throws AuctionClosedException, InvalidBidException, NotEnoughMoneyException {

        if (this.state != AuctionState.RUNNING) {
            throw new AuctionClosedException(
                    "Thất bại! Phiên đấu giá cho sản phẩm '"
                            + item.getItemName() + "' hiện không mở.");
        }

        if (!bidder.canBid(bidAmount)) {
            throw new NotEnoughMoneyException(
                    "Từ chối: Tài khoản của bạn (" + bidder.getMoney()
                            + " VND) không đủ để đặt mức giá " + bidAmount + " VND.");
        }

        // Nếu không hợp lệ, Item.updatePrice() ném InvalidBidException ra ngoài.
        this.item.updatePrice(bidAmount);

        this.currentWinner = bidder;
        bidTransactionHistory.add(new BidTransaction(bidder, item, bidAmount, LocalDateTime.now()));

        String msg = SUCCESS_MESSAGES[RANDOM.nextInt(SUCCESS_MESSAGES.length)];
        System.out.println(bidder.getName() + msg + " | Giá hiện tại: " + getCurrentPrice());

        // Anti-sniping: bid trong 30s cuối -> gia hạn 60s
        long secondsLeft = java.time.temporal.ChronoUnit.SECONDS.between(
                LocalDateTime.now(), item.getEndTime());
        if (secondsLeft >= 0 && secondsLeft <= 30) {
            item.extendEndTime(60);
            System.out.println("Phiên đấu giá được gia hạn thêm 60s.");
        }
    }

    public String getAuctionID() {
        return auctionID;
    }

    public Item getItem() {
        return item;
    }

    public User getCurrentWinner() {
        return currentWinner;
    }

    /** FIX: trả giá hiện tại từ item (nguồn sự thật duy nhất). */
    public long getCurrentPrice() {
        return item.getCurrentPrice();
    }

    public LocalDateTime getEndTime() {
        return this.item.getEndTime();
    }

    /** Trả về bản sao read-only của lịch sử bid. */
    public List<BidTransaction> getBidTransactionHistory() {
        return Collections.unmodifiableList(bidTransactionHistory);
    }
}