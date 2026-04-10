package server;

import common.*;
import java.util.List;
import java.util.ArrayList;

public class Auction {
    private String auctionID;
    private Item item;
    private List<BidTransaction> bidTransactionHistory;
    private User currentWinner;
    private AuctionState state = AuctionState.OPEN;
    private long currentPrice;


    public Auction(String auctionID, Item item) {
        this.auctionID = auctionID;
        this.item = item;
        this.bidTransactionHistory = new ArrayList<>();
        this.currentWinner = currentWinner;
        this.currentPrice = item.getStartPrice();
    }

    public AuctionState getState(){
        return state;
    }

    public void setState(AuctionState state){
        this.state = state;
    }
}
