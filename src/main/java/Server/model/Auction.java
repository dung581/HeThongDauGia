package Server.model;

import Common.Model.Item;
import Common.Model.User;
import Common.Enum.AuctionState;
import Common.Model.BidTransaction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Auction {

    private final String auctionID;
    private final Item item;
    private final List<BidTransaction> bidHistory = new ArrayList<>();

    private User currentWinner;
    private AuctionState state = AuctionState.OPEN;

    public Auction(String auctionID, Item item) {
        this.auctionID = auctionID;
        this.item = item;
    }

    public String getAuctionID() { return auctionID; }
    public Item getItem() { return item; }

    public User getCurrentWinner() { return currentWinner; }
    public void setCurrentWinner(User user) { this.currentWinner = user; }

    public AuctionState getState() { return state; }
    public void setState(AuctionState state) { this.state = state; }

    public long getCurrentPrice() { return item.getCurrentPrice(); }

    public LocalDateTime getEndTime() { return item.getEndTime(); }

    public void addBid(BidTransaction bid) {
        bidHistory.add(bid);
    }

    public List<BidTransaction> getBidHistory() {
        return Collections.unmodifiableList(bidHistory);
    }
}