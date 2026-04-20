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

public class AuctionManager {
    private static AuctionManager instance;
    private final Map<String, Auction> activeAuctions;
    private final ScheduledExecutorService timerService;
    private AuctionManager() {
        activeAuctions = new ConcurrentHashMap<>();
        timerService = Executors.newScheduledThreadPool(1);
        startTimer();
    }
    // singleton pattern
    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public void startAuction(Auction auction){
        if (auction.getState() == AuctionState.OPEN){
            auction.setState(AuctionState.RUNNING);
            activeAuctions.put(auction.getAuctionID(), auction);
            System.out.println("Bắt đầu phiên đấu giá 🎉🎉");
        }
    }
    //Đóng phiên đấu giá, tìm người thắng và TRỪ TIỀN
    private void endAuction(Auction auction) {
        // synchronized để tránh vừa đóng auction vừa nhận bid ở giây cuối cùng
        synchronized (auction) {
            // Đảm bảo không đóng 2 lần
            if (auction.getState() != AuctionState.RUNNING) return;
            auction.setState(AuctionState.FINISH);
            System.out.println("Phiên đấu giá [" + auction.getAuctionID() + "] đã KẾT THÚC!");
            User winner = auction.getCurrentWinner();
            if (winner != null) {
                long finalPrice = auction.getItem().getCurrentPrice();
                System.out.println("Người chiến thắng: " + winner.getName() + " với giá " + finalPrice + " VND 😍😍");
                // Trừ tiền người thắng cuộc (Truyền số âm vào updateMoney)
                winner.updateMoney(-finalPrice);
                System.out.println("Đã thanh toán! Số dư còn lại của " + winner.getName() + ": " + winner.getMoney() + " VND" +"💸💸");
                auction.setState(AuctionState.PAID);
            } else {
                System.out.println("Không có ai trả giá cho sản phẩm này. 😔 ");
                auction.setState(AuctionState.CANCELED);
            }
            // Gỡ khỏi bộ nhớ quản lý thời gian
            activeAuctions.remove(auction.getAuctionID());
            // TODO (Mạng): Gửi thông báo kết thúc phiên kèm tên người thắng về cho mọi Client
        }
    }
    public void handleClientBidRequest(String auctionID, User bidder, long amount) {
        Auction auction = activeAuctions.get(auctionID);
        if (auction == null) {
            System.out.println("Không tìm thấy phiên đấu giá [" + auctionID + "].");
            return;
        }
        try {
            // Gọi hàm placeBid (đã chứa logic kiểm tra tiền, giá, trạng thái)
            auction.placeBid(bidder, amount);
            System.out.println("Thành công: " + bidder.getName() + " đặt giá hợp lệ " + amount + " VND!");

            // TODO (Mạng): Kích hoạt Observer đẩy thông báo giá mới qua Socket cho toàn bộ Client

        } catch (AuctionClosedException e) {
            System.out.println("Lỗi: " + e.getMessage());
        } catch (InvalidBidException e) {
            System.out.println("Lỗi: " + e.getMessage());
        } catch (NotEnoughMoneyException e) {
            System.out.println("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Lỗi(Server Error): " + e.getMessage());
        }
    }
    // 1 giây quét 1 lần xem có phiên nào hết giờ không ( hetcuu nếu quá nhiều auction :)))))
    private void startTimer() {
        timerService.scheduleAtFixedRate(() -> {
            LocalDateTime now = LocalDateTime.now();
            // Duyệt qua tất cả phiên đang chạy
            for (Auction auction : activeAuctions.values()) {
                if (auction.getState() == AuctionState.RUNNING) {
                    if (now.isAfter(auction.getEndTime())) {
                        endAuction(auction);
                    }
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
    //Hàm gọi khi tắt Server để giải phóng luồng
    public void shutdown() {
        if (timerService != null && !timerService.isShutdown()) {
            timerService.shutdown();
            System.out.println("AuctionManager đã dừng an toàn.");
        }
    }
}