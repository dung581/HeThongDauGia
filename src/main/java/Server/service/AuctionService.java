package Server.service;

import Common.Enum.AuctionState;

import Common.Model.transaction.BidTransaction;
import Common.Model.user.User;
import Server.model.Auction;
import Common.Exceptions.AuctionClosedException;
import Common.Exceptions.InvalidBidException;
import Common.Exceptions.NotEnoughMoneyException;
import Common.Exceptions.ReturnMessage;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;

public class AuctionService {
    private static final String[] MSG = {
            " đang dẫn đầu cuộc đấu giá!🔥🔥",
            " quá vjpppppp prooo! Giá này chưa ai vượt qua! 🎉🎉",
            " vừa chiếm vị trí số 1! 🚀🚀🚀",
            " đang thống trị phiên đấu giá! 👑👑"
    };

    private static final Random RANDOM = new Random();

    public synchronized void placeBid(Auction auction, User bidder, long amount)
            throws AuctionClosedException, InvalidBidException, NotEnoughMoneyException {
        if (auction.getState() != AuctionState.RUNNING) {
            throw new AuctionClosedException(ReturnMessage.AUCTION_CLOSED);
        }

        if (!bidder.canBid(amount)) {
            throw new NotEnoughMoneyException(ReturnMessage.NOT_ENOUGH_MONEY);
        }

        auction.getItem().updatePrice(amount);

        auction.setCurrentWinner(bidder);

        System.out.println(bidder.getName() + MSG[RANDOM.nextInt(MSG.length)]);

        // Anti-sniping
        long secondsLeft = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getEndTime());

        if (secondsLeft >= 0 && secondsLeft <= 30) {
            auction.getItem().extendEndTime(60);
            System.out.println("Gia hạn thêm 60s");
        }
    }

    public void finalizeAuction(Auction auction) {
        User winner = auction.getCurrentWinner();

        if (winner != null) {
            long price = auction.getCurrentPrice();
            winner.updateMoney(-price);
            auction.setState(AuctionState.PAID);
        } else {
            auction.setState(AuctionState.CANCELED);
        }
    }
}