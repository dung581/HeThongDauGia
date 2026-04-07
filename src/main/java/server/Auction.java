package server;

import common.*;
import java.util.List;
import java.util.ArrayList;

public class Auction {
    private String auctionID;
    private Item item;
    private List<BidTransaction> bidTransactionHistory;
    private User winnerID;
    private String status;

    public Auction(String auctionID, Item item, List<BidTransaction> bidTransactionHistory, User winnerID, String status) {
        this.auctionID = auctionID;
        this.item = item;
        this.bidTransactionHistory = new ArrayList<>();
        this.winnerID = winnerID;
        this.status = "OPEN";
    }
}
