package common;

import java.time.LocalDateTime;

public class BidTransaction {
    private String transactionID;
    private User bidder;
    private Item item;
    private long bidAmount; //số tiền đặt giá
    private LocalDateTime timestamp; //thời gian đặt

    public BidTransaction(String transactionID, User bidder, Item item, long bidAmount, LocalDateTime timestamp) {
        this.transactionID = transactionID;
        this.bidder = bidder;
        this.item = item;
        this.bidAmount = bidAmount;
        this.timestamp = timestamp;
    }

    public String getTransactionID() {
        return transactionID;
    }

    public User getBidder() {
        return bidder;
    }

    public Item getItem() {
        return item;
    }

    public long getBidAmount() {
        return bidAmount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
