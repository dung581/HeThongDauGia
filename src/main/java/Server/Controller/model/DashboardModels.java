package Server.Controller.model;

import javafx.beans.property.SimpleStringProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
 * File này chỉ chứa các class dữ liệu phụ cho DashBoardController.
 *
 * Quy ước:
 * - XxxRow: dữ liệu để render 1 dòng/card trong ListView hoặc VBox.
 * - XxxDashboardData: dữ liệu tổng hợp load từ DB trong background Task,
 *   sau đó controller dùng để render toàn bộ dashboard.
 *
 * Các class này không xử lý sự kiện UI và không gọi service/repository.
 */

public final class DashboardModels {

    private DashboardModels() {
    }

// Một dòng phiên đang chạy trên dashboard Bidder.
public static class LiveSessionRow {
    // Id phiên, dùng khi click mở chi tiết phiên hoặc chuyển sang trạng thái ended.
    private final long sessionId;
    // Tên sản phẩm đang được đấu giá.
    private final String itemName;
    // Mô tả sản phẩm, hiển thị dưới tên item.
    private final String description;
    // Giá hiện tại đã được format để hiển thị.
    private final String currentPrice;
    // Bước giá tối thiểu đã được format.
    private final String minIncrement;
    // Người đang dẫn phiên, dạng text hiển thị.
    private final String leader;
    // Thời điểm kết thúc phiên, dùng để tính countdown.
    private final LocalDateTime endTime;
    // Text countdown thay đổi theo timer, ví dụ "05:30".
    private final SimpleStringProperty timeLeft = new SimpleStringProperty("-");

    public LiveSessionRow(long sessionId, String itemName, String description, String currentPrice, String minIncrement, String leader, LocalDateTime endTime) {
        this.sessionId = sessionId;
        this.itemName = itemName;
        this.description = description;
        this.currentPrice = currentPrice;
        this.minIncrement = minIncrement;
        this.leader = leader;
        this.endTime = endTime;
    }

    public long getSessionId() {
        return sessionId;
    }

    public String getItemName() {
        return itemName;
    }

    public String getDescription() {
        return description;
    }

    public String getCurrentPrice() {
        return currentPrice;
    }

    public String getMinIncrement() {
        return minIncrement;
    }

    public String getLeader() {
        return leader;
    }

    public String getTimeLeft() {
        return timeLeft.get();
    }

    public void setTimeLeft(String timeLeft) {
        this.timeLeft.set(timeLeft == null ? "" : timeLeft);
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public EndedSessionRow toEndedSessionRow() {
        return new EndedSessionRow(sessionId, itemName, description, currentPrice, minIncrement, leader, "ENDED");
    }
}

    /**
     * @param sessionId    Id phiên, dùng để tránh thêm trùng dòng ended.
     * @param itemName     Tên sản phẩm trong phiên đã kết thúc.
     * @param description  Mô tả sản phẩm, hiển thị dưới tên item.
     * @param finalPrice   Giá cuối cùng của phiên, đã format để hiển thị.
     * @param minIncrement Bước giá tối thiểu đã được format.
     * @param winner       Người thắng hoặc người dẫn cuối cùng.
     * @param status       Trạng thái cuối của phiên, ví dụ ENDED/PAID/CANCELED.
     */ // Một dòng phiên đã kết thúc trên dashboard Bidder.
    public record EndedSessionRow(long sessionId, String itemName, String description, String finalPrice,
                                  String minIncrement, String winner, String status) {
    }

    /**
     * @param sessionId    Id phiên để mở chi tiết hoặc màn kết quả.
     * @param itemId       Id sản phẩm thuộc phiên.
     * @param itemName     Tên sản phẩm.
     * @param description  Mô tả sản phẩm.
     * @param price        Giá hiện tại/cuối cùng đã format.
     * @param minIncrement Bước giá tối thiểu đã được format.
     * @param leader       Người đang dẫn hoặc người thắng.
     * @param status       Trạng thái phiên.
     */ // Một dòng tóm tắt phiên đấu giá, dùng chung cho dashboard Admin và Seller.
    public record SessionOverviewRow(long sessionId, long itemId, String itemName, String description, String price,
                                     String minIncrement, String leader, String status) {
    }

    /**
     * @param id           Id sản phẩm để tra cứu hoặc mở màn quản lý.
     * @param itemName     Tên sản phẩm.
     * @param description  Mô tả sản phẩm.
     * @param owner        Chủ sở hữu sản phẩm, thường dùng ở màn Admin.
     * @param price        Giá khởi điểm đã format.
     * @param minIncrement Bước giá tối thiểu đã format.
     * @param status       Trạng thái sản phẩm: PENDING, APPROVED, IN_AUCTION, SOLD...
     */ // Một dòng tóm tắt sản phẩm, dùng cho dashboard Admin/Seller.
    public record ItemOverviewRow(Long id, String itemName, String description, String owner, String price,
                                  String minIncrement, String status) {
    }

    /**
     * @param itemId      Id sản phẩm đã thắng.
     * @param sessionId   Id phiên tạo ra kết quả thắng.
     * @param itemName    Tên sản phẩm.
     * @param description Mô tả ngắn hiển thị dưới tên sản phẩm.
     * @param finalPrice  Giá thắng đã format.
     * @param status      Trạng thái phiên/sản phẩm sau khi thắng.
     */ // Một sản phẩm mà Bidder đã thắng, hiển thị ở vùng "sản phẩm sở hữu".
    public record OwnedProductRow(Long itemId, Long sessionId, String itemName, String description, String finalPrice,
                                  String status) {
    }

    /**
     * @param activeSessions Số phiên đang chạy.
     * @param pendingItems   Số sản phẩm đang chờ duyệt.
     * @param totalItems     Tổng số sản phẩm trong hệ thống.
     * @param totalUsers     Tổng số user trong hệ thống.
     * @param sessionRows    Danh sách phiên hiển thị ở khu Admin.
     * @param pendingRows    Danh sách sản phẩm pending hiển thị ở khu Admin.
     */ // Gói dữ liệu để render toàn bộ dashboard Admin sau khi background Task load xong.
    public record AdminDashboardData(long activeSessions, long pendingItems, long totalItems, long totalUsers,
                                     List<SessionOverviewRow> sessionRows, List<ItemOverviewRow> pendingRows) {
        public AdminDashboardData(
                long activeSessions,
                long pendingItems,
                long totalItems,
                long totalUsers,
                List<SessionOverviewRow> sessionRows,
                List<ItemOverviewRow> pendingRows
        ) {
            this.activeSessions = activeSessions;
            this.pendingItems = pendingItems;
            this.totalItems = totalItems;
            this.totalUsers = totalUsers;
            this.sessionRows = sessionRows == null ? new ArrayList<>() : sessionRows;
            this.pendingRows = pendingRows == null ? new ArrayList<>() : pendingRows;
        }
    }

    /**
     * @param totalItems     Tổng số sản phẩm của seller hiện tại.
     * @param pendingItems   Số sản phẩm đang chờ duyệt.
     * @param inAuctionItems Số sản phẩm đang trong phiên đấu giá.
     * @param soldItems      Số sản phẩm đã bán.
     * @param itemRows       Danh sách sản phẩm của seller.
     * @param sessionRows    Danh sách phiên liên quan tới sản phẩm của seller.
     */ // Gói dữ liệu để render toàn bộ dashboard Seller sau khi background Task load xong.
    public record SellerDashboardData(long totalItems, long pendingItems, long inAuctionItems, long soldItems,
                                      List<ItemOverviewRow> itemRows, List<SessionOverviewRow> sessionRows) {
        public SellerDashboardData(
                long totalItems,
                long pendingItems,
                long inAuctionItems,
                long soldItems,
                List<ItemOverviewRow> itemRows,
                List<SessionOverviewRow> sessionRows
        ) {
            this.totalItems = totalItems;
            this.pendingItems = pendingItems;
            this.inAuctionItems = inAuctionItems;
            this.soldItems = soldItems;
            this.itemRows = itemRows == null ? new ArrayList<>() : itemRows;
            this.sessionRows = sessionRows == null ? new ArrayList<>() : sessionRows;
        }
    }

    /**
     * @param availableBalance    Số dư còn dùng được để đặt giá.
     * @param lockedBalance       Số tiền đang bị khóa trong các stake.
     * @param lockedStakeCount    Số stake đang khóa.
     * @param leadingSessionCount Số phiên mà bidder hiện đang dẫn.
     * @param liveSessionCount    Số phiên đang chạy.
     * @param endedSessionCount   Số phiên đã kết thúc.
     * @param liveSessions        Danh sách phiên đang chạy.
     * @param endedSessions       Danh sách phiên đã kết thúc.
     * @param ownedProducts       Danh sách sản phẩm bidder đã thắng.
     */ // Gói dữ liệu để render toàn bộ dashboard Bidder sau khi background Task load xong.
    public record BidderDashboardData(long availableBalance, long lockedBalance, long lockedStakeCount,
                                      long leadingSessionCount, int liveSessionCount, int endedSessionCount,
                                      List<LiveSessionRow> liveSessions, List<EndedSessionRow> endedSessions,
                                      List<OwnedProductRow> ownedProducts) {
        public BidderDashboardData(
                long availableBalance,
                long lockedBalance,
                long lockedStakeCount,
                long leadingSessionCount,
                int liveSessionCount,
                int endedSessionCount,
                List<LiveSessionRow> liveSessions,
                List<EndedSessionRow> endedSessions,
                List<OwnedProductRow> ownedProducts
        ) {
            this.availableBalance = availableBalance;
            this.lockedBalance = lockedBalance;
            this.lockedStakeCount = lockedStakeCount;
            this.leadingSessionCount = leadingSessionCount;
            this.liveSessionCount = liveSessionCount;
            this.endedSessionCount = endedSessionCount;
            this.liveSessions = liveSessions == null ? new ArrayList<>() : liveSessions;
            this.endedSessions = endedSessions == null ? new ArrayList<>() : endedSessions;
            this.ownedProducts = ownedProducts == null ? new ArrayList<>() : ownedProducts;
        }
    }
}
