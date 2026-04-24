package Server.model;

import Common.Model.item.Item;
import Common.Model.user.User;
import Common.Enum.AuctionState;

import java.time.LocalDateTime;

public class Auction {

    private final long auctionID;
    private final Item item;

    private User currentWinner;
    private AuctionState state = AuctionState.OPEN;

    public Auction(long auctionID, Item item) {
        this.auctionID = auctionID;
        this.item = item;
    }

    public long getAuctionID() { return auctionID; }
    public Item getItem() { return item; }

    public User getCurrentWinner() { return currentWinner; }
    public void setCurrentWinner(User user) { this.currentWinner = user; }

    public AuctionState getState() { return state; }
    public void setState(AuctionState state) { this.state = state; }

    public long getCurrentPrice() { return item.getCurrentPrice(); }
    public LocalDateTime getEndTime() { return item.getEndTime(); }

}