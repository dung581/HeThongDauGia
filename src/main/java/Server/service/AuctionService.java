package Server.service;

import Common.DataBase.entities.*;
import Common.DataBase.repository.AccountRepository;
import Common.DataBase.repository.AuctionRepository;
import Common.DataBase.repository.BidRepository;
import Common.DataBase.repository.RepoUseInService.UserAccountRepository;
import Common.DataBase.repository.StakeRepository;
import Common.Enum.AuctionState;

import Common.Enum.ItemStatus;
import Common.Model.user.UserAccount;
import Server.service.Exceptions.AuctionClosedException;
import Server.service.Exceptions.InvalidBidException;
import Server.service.Exceptions.NotEnoughMoneyException;
import Server.service.Exceptions.ReturnMessage;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

import static Common.Enum.ItemStatus.PENDING;
import static Common.Enum.ItemStatus.SOLD;

public class AuctionService {
    private AccountRepository AR= new AccountRepository();
    private AuctionRepository AucRe= new AuctionRepository();
    private StakeRepository StaRe= new StakeRepository();
    private BidRepository BidRe= new BidRepository();
    private static final String[] MSG = {
            " đang dẫn đầu cuộc đấu giá!🔥🔥",
            " quá vjpppppp prooo! Giá này chưa ai vượt qua! 🎉🎉",
            " vừa chiếm vị trí số 1! 🚀🚀🚀",
            " đang thống trị phiên đấu giá! 👑👑"
    };

    private static final Random RANDOM = new Random();
    public List<Auction> getActive() {
        return AucRe.getActive();
    }
    public Auction createSession(long itemId, LocalDateTime endTime) {
        Auction a = new Auction();

        a.setItem_id(itemId);
        a.setCurrent_user_id(0); // chưa có người bid
        a.setCurrent_price(0);   // hoặc giá khởi điểm
        a.setStartTime(LocalDateTime.now());
        a.setEndTime(endTime);
        a.setState(AuctionState.RUNNING);

        AucRe.save(a);

        return a; // 🔥 đây chính là "→ Session"
    }

    public synchronized void placeBid(Auction auction, UserAccount bidder, long amount)
            throws AuctionClosedException, InvalidBidException, NotEnoughMoneyException {
        if (auction.getState() != AuctionState.RUNNING) {
            throw new AuctionClosedException(ReturnMessage.AUCTION_CLOSED);
        }
        if (bidder.getAvailableBalance()<amount) {
            throw new NotEnoughMoneyException(ReturnMessage.NOT_ENOUGH_MONEY);
        }
        Stake fixLockedBalance= StaRe.getStakeByAuctionId(auction.getCurrent_user_id());


        //thằng cũ được trả tiền
        Account oldacc= AR.getAccountByUserId(auction.getCurrent_user_id());
        oldacc.setBalance(oldacc.getBalance()+auction.getCurrent_price());
        oldacc.setLocked_balance(oldacc.getLocked_balance()-auction.getCurrent_price());

        auction.setCurrent_price(amount);
        //update stake
        fixLockedBalance.setUser_id(bidder.getUserId());
        fixLockedBalance.setAmount(amount);
        //thằng mới bị khóa thêm tiền
        Account newacc= AR.getAccountByUserId(bidder.getUserId());
        newacc.setLocked_balance(newacc.getLocked_balance()+amount);
        newacc.setBalance(newacc.getBalance()-amount);
        auction.setCurrent_user_id(bidder.getUserId());

        System.out.println(bidder.getFullname() + MSG[RANDOM.nextInt(MSG.length)]);


        // Anti-sniping
        long secondsLeft = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getEndTime());

        if (secondsLeft >= 0 && secondsLeft <= 30) {
            auction.setEndTime(auction.getEndTime().plusSeconds(60));
            System.out.println("Gia hạn thêm 60s");
        }
        AucRe.updateCurrentBid(auction.getId(), auction.getCurrent_user_id(), auction.getCurrent_price());
        AR.update(oldacc);
        AR.update(newacc);
        StaRe.updateUserAmountStatus(auction.getId(), bidder.getUserId(), amount, SOLD);
    }

    public void declareWinner(Auction auction) {
        long winnerID = auction.getCurrent_user_id();

        if (winnerID != 0) {
            Account a=AR.getAccountByUserId(winnerID);
            if (a == null) {
                throw new RuntimeException("Account not found");
            }
            long price = auction.getCurrent_price();
            a.setLocked_balance(a.getLocked_balance()-price);
            AR.update(a);
            bid newbid= new bid();
            newbid.setAuction_id(auction.getId());
            newbid.setPrice(auction.getCurrent_price());
            newbid.setItem_id(auction.getItem_id());
            newbid.setUser_id(auction.getCurrent_user_id());
            auction.setState(AuctionState.PAID);
            BidRe.update(newbid);
            StaRe.updateStatus(auction.getId(), PENDING);
        } else {
            auction.setState(AuctionState.CANCELED);
        }

    }
}