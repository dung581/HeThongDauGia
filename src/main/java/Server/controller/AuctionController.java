package Server.controller;

import Common.Model.User;
import Server.service.AuctionService;
import Server.repository.AuctionRepository;
import Server.service.Exceptions.AuctionClosedException;
import Server.service.Exceptions.InvalidBidException;
import Server.service.Exceptions.NotEnoughMoneyException;

import static com.sun.tools.classfile.Attribute.Exceptions;

public class AuctionController {

    private final AuctionRepository repo;
    private final AuctionService service;

    public AuctionController(AuctionRepository repo, AuctionService service) {
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