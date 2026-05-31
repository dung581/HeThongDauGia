package testutil;

import Common.DataBase.entities.*;
import Common.DataBase.repository.*;
import Common.Enum.ItemStatus;
import Common.Enum.StakeStatus;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Các fake repository in-memory, kế thừa repository thật và override method
 * để dùng dữ liệu trong bộ nhớ thay vì gọi database.
 *
 * Mỗi fake ghi lại số lần gọi / tham số gần nhất (thay cho verify() của Mockito).
 */
public final class FakeRepositories {

    private FakeRepositories() {
    }

    // ===================== ACCOUNT =====================
    public static class FakeAccountRepository extends AccountRepository {
        public final Map<Long, Account> byUserId = new HashMap<>();
        public int updateCalls = 0;
        public int createCalls = 0;
        private long seq = 0;

        public Account put(Account a) {
            byUserId.put(a.getUser_id(), a);
            return a;
        }

        @Override
        public Account getAccountByUserId(long user_id) {
            return byUserId.get(user_id);
        }

        @Override
        public void update(Account a) {
            updateCalls++;
            byUserId.put(a.getUser_id(), a);
        }

        @Override
        public void CreateAccount(Account account) {
            createCalls++;
            if (account.getId() == 0) {
                account.setId(++seq);
            }
            byUserId.put(account.getUser_id(), account);
        }

        @Override
        public List<Account> getAllAccount() {
            return new ArrayList<>(byUserId.values());
        }
    }

    // ===================== USER =====================
    public static class FakeUserRepository extends UserRepository {
        public final Map<String, User> byUsername = new HashMap<>();
        public final Map<Long, User> byId = new HashMap<>();
        public int createCalls = 0;
        public User lastCreated;
        public Long lastUpdatePwdUserId;
        public String lastUpdatePwdValue;
        private long seq = 100;

        public User put(User u) {
            byUsername.put(u.getUsername(), u);
            byId.put(u.getId(), u);
            return u;
        }

        @Override
        public User findByUsername(String username) {
            return byUsername.get(username);
        }

        @Override
        public User getUserById(long id) {
            return byId.get(id);
        }

        @Override
        public boolean existsByUsername(String username) {
            return byUsername.containsKey(username);
        }

        @Override
        public void updatePassword(long userId, String password) {
            lastUpdatePwdUserId = userId;
            lastUpdatePwdValue = password;
            User u = byId.get(userId);
            if (u != null) {
                u.setPassword(password);
            }
        }

        @Override
        public User createUser(User user) {
            createCalls++;
            if (user.getId() == 0) {
                user.setId(++seq);
            }
            put(user);
            lastCreated = user;
            return user;
        }

        @Override
        public List<User> getallUser() {
            return new ArrayList<>(byUsername.values());
        }
    }

    // ===================== ITEM =====================
    public static class FakeItemRepository extends ItemRepository {
        public final Map<Long, Item> byId = new HashMap<>();
        public int updateCalls = 0;
        public int saveCalls = 0;
        public Long lastRejectedId;
        public String lastRejectReason;
        private long seq = 0;

        public Item put(Item i) {
            byId.put(i.getId(), i);
            return i;
        }

        @Override
        public Item getItemById(long id) {
            return byId.get(id);
        }

        @Override
        public void saveItem(Item item) {
            saveCalls++;
            if (item.getId() == 0) {
                item.setId(++seq);
            }
            byId.put(item.getId(), item);
        }

        @Override
        public void update(Item item) {
            updateCalls++;
            byId.put(item.getId(), item);
        }

        @Override
        public void rejectWithReason(long id, String reason) {
            lastRejectedId = id;
            lastRejectReason = reason;
        }

        @Override
        public List<Item> getByStatus(ItemStatus status) {
            return byId.values().stream()
                    .filter(i -> i.getStatus() == status)
                    .collect(Collectors.toList());
        }

        @Override
        public List<Item> getByOwnerUserId(long ownerUserId) {
            return byId.values().stream()
                    .filter(i -> i.getOwner_user_id() == ownerUserId)
                    .collect(Collectors.toList());
        }

        @Override
        public List<Item> getAllItem() {
            return new ArrayList<>(byId.values());
        }
    }

    // ===================== AUCTION =====================
    public static class FakeAuctionRepository extends AuctionRepository {
        public final Map<Long, Auction> byId = new HashMap<>();
        public final Map<Long, Auction> byItemId = new HashMap<>();
        public int updateCalls = 0;
        public int saveCalls = 0;
        public Long lastUpdatedAuctionId;
        public Long lastUpdatedUserId;
        public Long lastUpdatedPrice;
        private long seq = 0;

        public Auction put(Auction a) {
            byId.put(a.getId(), a);
            byItemId.put(a.getItem_id(), a);
            return a;
        }

        @Override
        public Auction getById(long id) {
            return byId.get(id);
        }

        @Override
        public Auction getByItemId(long itemId) {
            return byItemId.get(itemId);
        }

        @Override
        public void save(Auction a) {
            saveCalls++;
            if (a.getId() == 0) {
                a.setId(++seq);
            }
            put(a);
        }

        @Override
        public void update(Auction a) {
            updateCalls++;
            put(a);
        }

        @Override
        public void updateCurrentBid(long auctionId, long userId, long price) {
            lastUpdatedAuctionId = auctionId;
            lastUpdatedUserId = userId;
            lastUpdatedPrice = price;
            Auction a = byId.get(auctionId);
            if (a != null) {
                a.setCurrent_user_id(userId);
                a.setCurrent_price(price);
            }
        }

        @Override
        public List<Auction> getActive() {
            return new ArrayList<>(byId.values());
        }

        @Override
        public List<Auction> getAll() {
            return new ArrayList<>(byId.values());
        }
    }

    // ===================== BID =====================
    public static class FakeBidRepository extends BidRepository {
        public final List<Bid> saved = new ArrayList<>();
        public int saveCalls = 0;

        @Override
        public void saveBid(Bid b) {
            saveCalls++;
            saved.add(b);
        }

        @Override
        public List<Bid> getBySessionId(long sessionId) {
            return saved.stream()
                    .filter(b -> b.getAuction_id() == sessionId)
                    .collect(Collectors.toList());
        }
    }

    // ===================== STAKE =====================
    public static class FakeStakeRepository extends StakeRepository {
        public final Map<Long, Stake> byId = new HashMap<>();
        public int saveCalls = 0;
        public boolean failOnSave = false; // dùng để test rollback
        public final List<Object[]> statusUpdates = new ArrayList<>(); // {stakeId, StakeStatus}
        private long seq = 0;

        @Override
        public void saveStake(Stake stake) {
            if (failOnSave) {
                throw new RuntimeException("DB error (gia lap loi luu stake)");
            }
            saveCalls++;
            if (stake.getId() == 0) {
                stake.setId(++seq);
            }
            byId.put(stake.getId(), stake);
        }

        @Override
        public Stake getById(long id) {
            return byId.get(id);
        }

        @Override
        public void updateStatus(long stakeId, StakeStatus status) {
            statusUpdates.add(new Object[]{stakeId, status});
            Stake s = byId.get(stakeId);
            if (s != null) {
                s.setStatus(status);
            }
        }

        @Override
        public Stake getByAuctionIdAndUserIdAndStatus(long auctionId, long userId, StakeStatus status) {
            return byId.values().stream()
                    .filter(s -> s.getAution_id() == auctionId
                            && s.getUser_id() == userId
                            && s.getStatus() == status)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<Stake> getByUserId(long userId) {
            return byId.values().stream()
                    .filter(s -> s.getUser_id() == userId)
                    .collect(Collectors.toList());
        }
    }

    // ===================== AUTOBID =====================
    public static class FakeAutoBidRepository extends AutoBidRepository {
        public final Map<Long, Autobid> byId = new HashMap<>();
        public int saveCalls = 0;
        public final List<Object[]> activeUpdates = new ArrayList<>();    // {id, boolean}
        public final List<Object[]> maxActiveUpdates = new ArrayList<>(); // {id, maxPrice, boolean}
        public final List<Object[]> endTimeUpdates = new ArrayList<>();   // {auctionId, LocalDateTime}
        private long seq = 0;

        public Autobid put(Autobid a) {
            byId.put(a.getId(), a);
            return a;
        }

        @Override
        public Autobid saveAutobid(Autobid a) {
            saveCalls++;
            if (a.getId() == 0) {
                a.setId(++seq);
            }
            byId.put(a.getId(), a);
            return a;
        }

        @Override
        public List<Autobid> getByUserId(long userId) {
            return byId.values().stream()
                    .filter(ab -> ab.getUser_id() == userId)
                    .collect(Collectors.toList());
        }

        @Override
        public List<Autobid> getActiveByItemId(long itemId) {
            return byId.values().stream()
                    .filter(ab -> ab.getItem_id() == itemId && ab.is_active())
                    .collect(Collectors.toList());
        }

        @Override
        public Autobid getLatestByUserAndItem(long userId, long itemId) {
            return byId.values().stream()
                    .filter(ab -> ab.getUser_id() == userId && ab.getItem_id() == itemId)
                    .max(Comparator.comparingLong(Autobid::getId))
                    .orElse(null);
        }

        @Override
        public void updateActive(long id, boolean isActive) {
            activeUpdates.add(new Object[]{id, isActive});
            Autobid ab = byId.get(id);
            if (ab != null) {
                ab.set_active(isActive);
            }
        }

        @Override
        public void updateMaxAndActive(long id, long maxPrice, boolean isActive) {
            maxActiveUpdates.add(new Object[]{id, maxPrice, isActive});
            Autobid ab = byId.get(id);
            if (ab != null) {
                ab.setMax_price(maxPrice);
                ab.set_active(isActive);
            }
        }

        @Override
        public void updateEndTime(long auctionId, LocalDateTime newEndTime) {
            endTimeUpdates.add(new Object[]{auctionId, newEndTime});
        }
    }
}