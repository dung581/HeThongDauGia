package Server.controller;

import Common.Model.user.User;
import Server.service.AuctionService;
import Server.service.AuctionManager;
import Common.Exceptions.AuctionClosedException;
import Common.Exceptions.InvalidBidException;
import Common.Exceptions.NotEnoughMoneyException;

public class AuctionController {

    private final AuctionManager repo;
    private final AuctionService service;

    public AuctionController(AuctionManager repo, AuctionService service) {
        this.repo = repo;
        this.service = service;
    }

    public void handleBid(String auctionID, User user, long amount) {
        var auction = repo.findById(auctionID);
        if (auction == null) {
            System.out.println("Không tồn tại auction");
            return;
        }
        try {
            service.placeBid(auction, user, amount);
        } catch (AuctionClosedException | InvalidBidException | NotEnoughMoneyException e) {
            System.out.println("Lỗi: " + e.getMessage());
        }
    }
}