package Server.core;

import Common.Enum.AuctionState;
import Common.Model.User;
import Exceptions.AuctionClosedException;
import Exceptions.NotEnoughMoneyException;
import Exceptions.InvalidBidException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Singleton quản lý toàn bộ phiên đấu giá đang chạy trên server.
 *
 * FIX so với bản cũ:
 *   1. Đổi FINISH -> FINISHED theo enum mới.
 *   2. Catch gộp 3 custom exceptions bằng multi-catch (giảm boilerplate).
 */
public class AuctionManager {

    private static AuctionManager instance;

    private final Map<String, Server.core.Auction> activeAuctions;
    private final ScheduledExecutorService timerService;

    private AuctionManager() {
        this.activeAuctions = new ConcurrentHashMap<>();
        this.timerService = Executors.newScheduledThreadPool(1);
        startTimer();
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public void startAuction(Server.core.Auction auction) {
        if (auction.getState() == AuctionState.OPEN) {
            auction.setState(AuctionState.RUNNING);
            activeAuctions.put(auction.getAuctionID(), auction);
            System.out.println("Bắt đầu phiên đấu giá 🎉🎉");
        }
    }

    /** Đóng phiên, xác định người thắng, thanh toán. */
    private void endAuction(Server.core.Auction auction) {
        synchronized (auction) {
            // Tránh đóng 2 lần
            if (auction.getState() != AuctionState.RUNNING) return;

            auction.setState(AuctionState.FINISHED);
            System.out.println("Phiên đấu giá [" + auction.getAuctionID() + "] đã KẾT THÚC!");

            User winner = auction.getCurrentWinner();
            if (winner != null) {
                long finalPrice = auction.getItem().getCurrentPrice();
                System.out.println("Người chiến thắng: " + winner.getName()
                        + " với giá " + finalPrice + " VND 😍😍");
                winner.updateMoney(-finalPrice);
                System.out.println("Đã thanh toán! Số dư còn lại của "
                        + winner.getName() + ": " + winner.getMoney() + " VND 💸💸");
                auction.setState(AuctionState.PAID);
            } else {
                System.out.println("Không có ai trả giá cho sản phẩm này. 😔");
                auction.setState(AuctionState.CANCELED);
            }

            activeAuctions.remove(auction.getAuctionID());
            // TODO (Mạng): broadcast event AUCTION_FINISHED cho tất cả client
        }
    }

    public void handleClientBidRequest(String auctionID, User bidder, long amount) {
        Server.core.Auction auction = activeAuctions.get(auctionID);
        if (auction == null) {
            System.out.println("Không tìm thấy phiên đấu giá [" + auctionID + "].");
            return;
        }

        try {
            auction.placeBid(bidder, amount);
            System.out.println("Thành công: " + bidder.getName()
                    + " đặt giá hợp lệ " + amount + " VND!");
            // TODO (Mạng): broadcast event NEW_BID cho tất cả client đang xem phiên này

        } catch (AuctionClosedException | InvalidBidException | NotEnoughMoneyException e) {
            // Multi-catch: 3 lỗi nghiệp vụ xử lý giống nhau
            System.out.println("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Lỗi (Server Error): " + e.getMessage());
        }
    }

    /** Timer quét 1 lần/giây để đóng các phiên hết hạn. */
    private void startTimer() {
        timerService.scheduleAtFixedRate(() -> {
            try {
                LocalDateTime now = LocalDateTime.now();
                for (Auction auction : activeAuctions.values()) {
                    if (auction.getState() == AuctionState.RUNNING
                            && now.isAfter(auction.getEndTime())) {
                        endAuction(auction);
                    }
                }
            } catch (Exception e) {
                // Phải catch mọi Exception trong ScheduledExecutorService,
                // nếu không task sẽ bị silently cancel.
                System.err.println("Lỗi trong timer: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void shutdown() {
        if (timerService != null && !timerService.isShutdown()) {
            timerService.shutdown();
            System.out.println("AuctionManager đã dừng an toàn.");
        }
    }
}