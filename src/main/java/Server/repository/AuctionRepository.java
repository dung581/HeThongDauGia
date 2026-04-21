package Server.repository;

import Server.model.Auction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionRepository {

    private final Map<String, Auction> auctions = new ConcurrentHashMap<>();

    public void save(Auction auction) {
        auctions.put(auction.getAuctionID(), auction);
    }

    public Auction findById(String id) {
        return auctions.get(id);
    }

    public void remove(String id) {
        auctions.remove(id);
    }

    public Map<String, Auction> findAll() {
        return auctions;
    }
}