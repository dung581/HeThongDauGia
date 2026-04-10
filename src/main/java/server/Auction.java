package server;

import common.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

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
    public synchronized boolean placeBid(User bidder, long bidAmount){
        if (this.getState() != AuctionState.OPEN){
            System.out.println("Phiên đấu giá chưa mở");
            return false;
        }
        if (bidAmount <= currentPrice){
            System.out.println("Giá đặt phải cao hơn giá hiện tại");
            return false;
        }
        // cập nhật người dẫn đầu
        this.currentPrice = bidAmount;
        this.currentWinner = bidder;
        // cập nhật lịch sử đấu giá
        BidTransaction bidTransaction = new BidTransaction(bidder, bidAmount, LocalDateTime.now());
        bidTransactionHistory.add(bidTransaction);
        String msg = successMessage[random.nextInt(successMessage.length)];
        System.out.println(bidder.getName() + msg + " | Giá hiện tại: " + currentPrice);
        // +++++ ++++++
        return true;
    }
}