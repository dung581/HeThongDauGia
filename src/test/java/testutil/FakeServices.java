package testutil;

import Common.DataBase.entities.Auction;
import Common.DataBase.entities.Stake;
import Server.service.AccountService;
import Server.service.AutoBidService;
import Server.service.StakeService;

import java.util.ArrayList;
import java.util.List;

/**
 * Fake các service phụ thuộc (được service khác khởi tạo bên trong).
 * Ghi lại lời gọi và cho phép cấu hình giá trị trả về.
 */
public final class FakeServices {

    private FakeServices() {
    }

    // ===================== ACCOUNT SERVICE =====================
    public static class FakeAccountService extends AccountService {
        public final List<long[]> lockCalls = new ArrayList<>();    // {userId, amount}
        public final List<long[]> releaseCalls = new ArrayList<>(); // {userId, amount}
        public final List<long[]> deductCalls = new ArrayList<>();  // {userId, amount}
        public final List<long[]> creditCalls = new ArrayList<>();  // {sellerId, amount}
        public boolean throwOnLock = false;

        @Override
        public void lockFunds(long userId, long amount) {
            if (throwOnLock) {
                throw new RuntimeException("Not enough money (gia lap)");
            }
            lockCalls.add(new long[]{userId, amount});
        }

        @Override
        public void releaseFunds(long userId, long amount) {
            releaseCalls.add(new long[]{userId, amount});
        }

        @Override
        public void deductLockedFunds(long userId, long amount) {
            deductCalls.add(new long[]{userId, amount});
        }

        @Override
        public void creditSaleProceeds(long sellerId, long amount) {
            creditCalls.add(new long[]{sellerId, amount});
        }
    }

    // ===================== STAKE SERVICE =====================
    public static class FakeStakeService extends StakeService {
        public final List<long[]> createCalls = new ArrayList<>(); // {auctionId, userId, itemId, amount}
        public final List<Long> releaseByIdCalls = new ArrayList<>();
        public final List<Stake> releaseByObjCalls = new ArrayList<>();
        public final List<Long> wonCalls = new ArrayList<>();
        public Stake activeStakeToReturn = null;
        private long seq = 900;

        @Override
        public Stake createStakeForAuction(long auctionId, long userId, long itemId, long amount) {
            createCalls.add(new long[]{auctionId, userId, itemId, amount});
            Stake s = new Stake();
            s.setId(++seq);
            s.setAution_id(auctionId);
            s.setUser_id(userId);
            s.setLocked_item_id(itemId);
            s.setAmount(amount);
            return s;
        }

        @Override
        public void releaseStake(long stakeId) {
            releaseByIdCalls.add(stakeId);
        }

        @Override
        public void releaseStake(Stake s) {
            releaseByObjCalls.add(s);
        }

        @Override
        public void markWon(long stakeId) {
            wonCalls.add(stakeId);
        }

        @Override
        public Stake getActiveStake(long auctionId, long userId) {
            return activeStakeToReturn;
        }
    }

    // ===================== AUTOBID SERVICE =====================
    public static class FakeAutoBidService extends AutoBidService {
        public int autoTimeCalls = 0;
        public final List<long[]> triggerCalls = new ArrayList<>(); // {itemId, price, userId, minIncrement}

        @Override
        public void autoTime(Auction auction) {
            autoTimeCalls++;
        }

        @Override
        public boolean trigger(long itemId, long currentPrice, long currentUserId, long minIncrement) {
            triggerCalls.add(new long[]{itemId, currentPrice, currentUserId, minIncrement});
            return false;
        }
    }
}