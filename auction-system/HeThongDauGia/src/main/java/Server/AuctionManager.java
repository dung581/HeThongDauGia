package Server;

import Common.User;
import Exceptions.AuctionClosedException;
import Exceptions.NotEnoughMoneyException;
import Exceptions.InvalidBidException;
import Common.AuctionState;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionManager {
    // 1. Singleton Instance
    private static AuctionManager instance;

    // 2. Lưu trữ các phiên đấu giá đang chạy (Dùng ConcurrentHashMap để an toàn với đa luồng)
    private final Map<String, Auction> activeAuctions;

    // 3. Service chạy ngầm đếm thời gian
    private final ScheduledExecutorService timerService;

    // Constructor private
    private AuctionManager() {
        activeAuctions = new ConcurrentHashMap<>();
        timerService = Executors.newScheduledThreadPool(1);
        startTimer(); // Khởi động đồng hồ ngay khi Manager được tạo
    }

    // Hàm lấy Instance duy nhất
    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    // ================= CHỨC NĂNG CỐT LÕI =================

    //Mở một phiên đấu giá mới và đưa vào danh sách theo dõi thời gian
    public void startNewAuction(Auction auction) {
        auction.setState(AuctionState.RUNNING);
        activeAuctions.put(auction.getAuctionID(), auction);
        System.out.println("Đã mở phiên đấu giá: " + auction.getAuctionID() + " - Kết thúc dự kiến: " + auction.getEndTime());
    }

    // lý yêu cầu đặt giá (Bid) từ Client gửi lên

    public void handleClientBidRequest(String auctionID, User bidder, long amount) {
        Auction auction = activeAuctions.get(auctionID);

        if (auction == null) {
            System.out.println("Lỗi 404: Không tìm thấy phiên đấu giá [" + auctionID + "].");
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

    // XỬ LÝ THỜI GIAN CHẠY NGẦM

    //Luồng chạy ngầm: Mỗi giây quét 1 lần xem có phiên nào hết giờ không
    private void startTimer() {
        timerService.scheduleAtFixedRate(() -> {
            LocalDateTime now = LocalDateTime.now();

            // Duyệt qua tất cả phiên đang chạy
            for (Auction auction : activeAuctions.values()) {
                if (auction.getState() == AuctionState.RUNNING) {

                    // Nếu thời gian hiện tại vượt qua thời gian kết thúc của phiên
                    if (now.isAfter(auction.getEndTime())) {
                        closeAuction(auction);
                    }
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    //Đóng phiên đấu giá, tìm người thắng và TRỪ TIỀN
    private void closeAuction(Auction auction) {
        // Dùng synchronized để tránh xung đột với người đang bid ở những milli-giây cuối
        synchronized (auction) {
            // Đảm bảo không đóng 2 lần
            if (auction.getState() != AuctionState.RUNNING) return;

            auction.setState(AuctionState.FINISH);
            System.out.println("Phiên đấu giá [" + auction.getAuctionID() + "] đã KẾT THÚC!");

            User winner = auction.getCurrentWinner();

            if (winner != null) {
                long finalPrice = auction.getCurrentPrice(); // Hoặc lấy từ auction.getItem().getCurrentPrice()
                System.out.println("🏆 Người chiến thắng: " + winner.getName() + " với giá " + finalPrice + " VND");

                // Trừ tiền người thắng cuộc (Truyền số âm vào updateMoney)
                winner.updateMoney(-finalPrice);
                System.out.println("💰 Đã thanh toán! Số dư còn lại của " + winner.getName() + ": " + winner.getMoney() + " VND");

                auction.setState(AuctionState.PAID);
            } else {
                System.out.println("😔 Không có ai trả giá cho sản phẩm này.");
                auction.setState(AuctionState.CANCELED);
            }

            // Gỡ khỏi bộ nhớ quản lý thời gian
            activeAuctions.remove(auction.getAuctionID());

            // TODO (Mạng): Gửi thông báo kết thúc phiên kèm tên người thắng về cho mọi Client
        }
    }

    //Hàm gọi khi tắt Server để giải phóng luồng

    public void shutdown() {
        if (timerService != null && !timerService.isShutdown()) {
            timerService.shutdown();
            System.out.println("AuctionManager đã dừng an toàn.");
        }
    }
}