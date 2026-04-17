package Server;

import Common.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import Exceptions.AuctionClosedException;
import Exceptions.InvalidBidException;
import Exceptions.NotEnoughMoneyException;

public class Auction {
    private static final String[] successMessage = {
            " đang dẫn đầu cuộc đấu giá!🔥🔥",
            " quá vjpppppp prooo! Giá này chưa ai vượt qua! 🎉🎉",
            " vừa chiếm vị trí số 1! 🚀🚀🚀",
            " đang thống trị phiên đấu giá! 👑👑"
    };
    private String auctionID;
    private Item item;
    private List<BidTransaction> bidTransactionHistory;
    private User currentWinner;
    private AuctionState state = AuctionState.OPEN;
    private long currentPrice;
    private static final Random random = new Random();


    public Auction(String auctionID, Item item) {
        this.auctionID = auctionID;
        this.item = item;
        this.bidTransactionHistory = new ArrayList<>();
        this.currentWinner = null;
        this.currentPrice = item.getStartPrice();
    }

    public AuctionState getState(){
        return state;
    }

    public void setState(AuctionState state){
        this.state = state;
    }
    // tránh nhiều người đặt cùng lúc (synchronized) và cập nhật phiên đấu giá
    public synchronized void placeBid(User bidder, long bidAmount) throws AuctionClosedException, InvalidBidException, NotEnoughMoneyException {
        // 1. Kiểm tra trạng thái
        if (this.getState() != AuctionState.RUNNING) {
            throw new AuctionClosedException("Thất bại! Phiên đấu giá cho sản phẩm '" + item.getItemName() + "' hiện không mở.");
        }
        if (!bidder.canBid(bidAmount)) {
            // Ném lỗi kèm theo số dư hiện tại để thông báo cho người dùng biết
            throw new NotEnoughMoneyException("Từ chối: Tài khoản của bạn (" + bidder.getMoney() + " VND) không đủ để đặt mức giá " + bidAmount + " VND.");
        }

        //  Kiểm tra giá (nếu giá sai, Item sẽ tự động ném InvalidBidException ở đây)
        this.item.updatePrice(bidAmount);

        // Cập nhật người dẫn đầu và lịch sử
        this.currentWinner = bidder;
        bidTransactionHistory.add(new BidTransaction(bidder, bidAmount, LocalDateTime.now()));        String msg = successMessage[random.nextInt(successMessage.length)];
        System.out.println(bidder.getName() + msg + " | Giá hiện tại: " + currentPrice);
        // Nếu bid trong 30 giây cuối cùng, cộng thêm 60 giây vào Item
        long secondsLeft = java.time.temporal.ChronoUnit.SECONDS.between(LocalDateTime.now(), item.getEndTime());
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

    public long getCurrentPrice() {
        return currentPrice;
    }
    
    public LocalDateTime getEndTime() {
        return this.item.getEndTime();
    }


}