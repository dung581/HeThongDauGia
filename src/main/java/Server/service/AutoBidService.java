package Server.service;

import Common.DataBase.entities.Auction;
import Common.DataBase.entities.Autobid;
import Common.DataBase.repository.AuctionRepository;
import Common.DataBase.repository.AutoBidRepository;
import Common.Enum.AuctionState;

import java.util.Comparator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class AutoBidService {

    private final AutoBidRepository repo = new AutoBidRepository();
    private final AuctionRepository auctionRepo = new AuctionRepository();

    private void deactivate(long id) {
        repo.updateActive(id, false);
    }

    public Autobid configureAndActivate(long userId, long itemId, long maxPrice) {
        Optional<Autobid> latest = getLatestByUserAndItem(userId, itemId);

        if (latest.isPresent()) {
            Autobid saved = latest.get();
            repo.updateMaxAndActive(saved.getId(), maxPrice, true);
            saved.setMax_price(maxPrice);
            saved.set_active(true);
            return saved;
        }

        return createAutobid(userId, itemId, maxPrice, true);
    }

    // Bật autobid rồi đọc lại bản ghi mới nhất để controller cập nhật UI ngay.
    public Optional<Autobid> configureAndGetLatest(long userId, long itemId, long maxPrice) {
        configureAndActivate(userId, itemId, maxPrice);
        return getLatestByUserAndItem(userId, itemId);
    }

    public void deactivateByUserAndItem(long userId, long itemId) {
        Autobid autobid = getLatestByUserAndItem(userId, itemId)
                .orElseThrow(() -> new RuntimeException("AutoBid not found"));
        deactivate(autobid.getId());
    }

    // Tắt autobid rồi trả lại trạng thái mới nhất để controller chỉ việc render UI.
    public Optional<Autobid> deactivateAndGetLatest(long userId, long itemId) {
        deactivateByUserAndItem(userId, itemId);
        return getLatestByUserAndItem(userId, itemId);
    }

    public Optional<Autobid> getLatestByUserAndItem(long userId, long itemId) {
        return Optional.ofNullable(repo.getLatestByUserAndItem(userId, itemId));
    }

    private Autobid createAutobid(long userId, long itemId, long maxPrice, boolean active) {
        Autobid ab = new Autobid();
        ab.setUser_id(userId);
        ab.setItem_id(itemId);
        ab.setMax_price(maxPrice);
        ab.set_active(active);

        return repo.saveAutobid(ab);
    }

    // Resume chuỗi autobid của toàn phiên rồi trả lại trạng thái autobid của user hiện tại để UI render.
    public AutoBidActionResult resumeForSessionAndGetLatest(
            long userId,
            long sessionId,
            long itemId,
            long minIncrement
    ) {
        boolean placedBid = resumeForSession(sessionId, itemId, minIncrement);
        Optional<Autobid> latestAutobid = getLatestByUserAndItem(userId, itemId);
        String message = placedBid ? "Auto bid đã xử lý giá mới." : null;
        return new AutoBidActionResult(latestAutobid, placedBid, message);
    }

    // Resume autobid dựa trên leader và giá hiện tại trong DB; không phụ thuộc account nào đang đăng nhập.
    public boolean resumeForSession(
            long sessionId,
            long itemId,
            long minIncrement
    ) {
        boolean placedAnyBid = false;

        // Resolve chuỗi autobid bằng các bước nhảy lớn, tránh ghi hàng trăm bid khi minIncrement nhỏ.
        for (int guard = 0; guard < 10; guard++) {
            Auction latestSession = auctionRepo.getById(sessionId);
            if (latestSession == null || latestSession.getState() != AuctionState.RUNNING) {
                return placedAnyBid;
            }
            if (latestSession.getCurrent_user_id() <= 0) {
                return placedAnyBid;
            }

            boolean placedBid = trigger(
                    itemId,
                    latestSession.getCurrent_price(),
                    latestSession.getCurrent_user_id(),
                    minIncrement
            );
            if (!placedBid) {
                return placedAnyBid;
            }
            placedAnyBid = true;
        }

        return placedAnyBid;
    }

    public boolean trigger(long itemId, long currentPrice, long currentUserId, long minIncrement) {
        long step = minIncrement > 0 ? minIncrement : 1L;
        long minNextPrice;
        try {
            minNextPrice = Math.addExact(currentPrice, step);
        } catch (ArithmeticException e) {
            return false;
        }
        BidService bidService = new BidService();
        List<Autobid> activeBids = repo.getActiveByItemId(itemId).stream()
                .filter(Autobid::is_active)
                .filter(autobid -> autobid.getMax_price() >= minNextPrice)
                .sorted(Comparator
                        .comparingLong(Autobid::getMax_price)
                        .reversed()
                        .thenComparingLong(Autobid::getId))
                .toList();

        Autobid nextBidder = activeBids.stream()
                // Auto bid chỉ phản ứng với bid của người khác, không tự đẩy giá của chính người vừa đặt.
                .filter(autobid -> autobid.getUser_id() != currentUserId)
                .findFirst()
                .orElse(null);
        if (nextBidder == null) {
            return false;
        }

        long strongestOpponentMax = currentPrice;
        for (Autobid autobid : activeBids) {
            if (autobid.getUser_id() != nextBidder.getUser_id()) {
                strongestOpponentMax = Math.max(strongestOpponentMax, autobid.getMax_price());
            }
        }

        long competitivePrice = addOrCap(Math.max(currentPrice, strongestOpponentMax), step);
        long nextPrice = Math.min(nextBidder.getMax_price(), competitivePrice);
        if (nextPrice < minNextPrice) {
            return false;
        }

        try {
            bidService.placeBid(nextBidder.getUser_id(), itemId, nextPrice);
            return true;
        } catch (RuntimeException e) {
            // Auto bid lỗi không được làm fail lệnh đặt giá thủ công vừa thành công.
            return false;
        }
    }

    // Cộng có chặn tràn số để auto bid không làm vỡ luồng khi giá cực lớn.
    private long addOrCap(long value, long increment) {
        try {
            return Math.addExact(value, increment);
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE;
        }
    }

    public List<Autobid> getByUserId(long userId) {
        return repo.getByUserId(userId);
    }

    public void autoTime(Auction auction) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = auction.getEndTime();

        // Nếu bid trong vòng 5 phút cuối
        if (now.isAfter(endTime.minusMinutes(5)) && now.isBefore(endTime)) {
            auction.setEndTime(endTime.plusMinutes(5));
            repo.updateEndTime(auction.getId(), auction.getEndTime());
        }
    }

    // Kết quả service trả về cho màn chi tiết phiên sau khi bật autobid.
        public record AutoBidActionResult(Optional<Autobid> autobid, boolean immediateBidPlaced, String message) {
            public AutoBidActionResult(Optional<Autobid> autobid, boolean immediateBidPlaced, String message) {
                this.autobid = autobid == null ? Optional.empty() : autobid;
                this.immediateBidPlaced = immediateBidPlaced;
                this.message = message;
            }
        }

}
