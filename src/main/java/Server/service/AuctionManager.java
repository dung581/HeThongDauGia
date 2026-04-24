package Server.service;

import Server.model.Auction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {

    private final Map<Long, Auction> auctions = new ConcurrentHashMap<>();

    public void save(Auction auction) {
        auctions.put(auction.getAuctionID(), auction);
    }

    public Auction findById(long id) {
        return auctions.get(id);
    }

    public void remove(long id) {
        auctions.remove(id);
    }

    public Map<Long, Auction> findAll() {
        return auctions;
    }
}