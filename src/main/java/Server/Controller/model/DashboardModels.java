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

// Một dòng phiên đã kết thúc trên dashboard Bidder.
public static class EndedSessionRow {
    // Id phiên, dùng để tránh thêm trùng dòng ended.
    private final long sessionId;
    // Tên sản phẩm trong phiên đã kết thúc.
    private final String itemName;
    // Mô tả sản phẩm, hiển thị dưới tên item.
    private final String description;
    // Giá cuối cùng của phiên, đã format để hiển thị.
    private final String finalPrice;
    // Bước giá tối thiểu đã được format.
    private final String minIncrement;
    // Người thắng hoặc người dẫn cuối cùng.
    private final String winner;
    // Trạng thái cuối của phiên, ví dụ ENDED/PAID/CANCELED.
    private final String status;

    public EndedSessionRow(long sessionId, String itemName, String description, String finalPrice, String minIncrement, String winner, String status) {
        this.sessionId = sessionId;
        this.itemName = itemName;
        this.description = description;
        this.finalPrice = finalPrice;
        this.minIncrement = minIncrement;
        this.winner = winner;
        this.status = status;
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

    public String getFinalPrice() {
        return finalPrice;
    }

    public String getMinIncrement() {
        return minIncrement;
    }

    public String getWinner() {
        return winner;
    }

    public String getStatus() {
        return status;
    }
}

// Một dòng tóm tắt phiên đấu giá, dùng chung cho dashboard Admin và Seller.
public static class SessionOverviewRow {
    // Id phiên để mở chi tiết hoặc màn kết quả.
    private final long sessionId;
    // Id sản phẩm thuộc phiên.
    private final long itemId;
    // Tên sản phẩm.
    private final String itemName;
    // Mô tả sản phẩm.
    private final String description;
    // Giá hiện tại/cuối cùng đã format.
    private final String price;
    // Bước giá tối thiểu đã được format.
    private final String minIncrement;
    // Người đang dẫn hoặc người thắng.
    private final String leader;
    // Trạng thái phiên.
    private final String status;

    public SessionOverviewRow(long sessionId, long itemId, String itemName, String description, String price, String minIncrement, String leader, String status) {
        this.sessionId = sessionId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.description = description;
        this.price = price;
        this.minIncrement = minIncrement;
        this.leader = leader;
        this.status = status;
    }

    public long getSessionId() {
        return sessionId;
    }

    public long getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public String getDescription() {
        return description;
    }

    public String getPrice() {
        return price;
    }

    public String getMinIncrement() {
        return minIncrement;
    }

    public String getLeader() {
        return leader;
    }

    public String getStatus() {
        return status;
    }
}

// Một dòng tóm tắt sản phẩm, dùng cho dashboard Admin/Seller.
public static class ItemOverviewRow {
    // Id sản phẩm để tra cứu hoặc mở màn quản lý.
    private final Long id;
    // Tên sản phẩm.
    private final String itemName;
    // Mô tả sản phẩm.
    private final String description;
    // Chủ sở hữu sản phẩm, thường dùng ở màn Admin.
    private final String owner;
    // Giá khởi điểm đã format.
    private final String price;
    // Bước giá tối thiểu đã format.
    private final String minIncrement;
    // Trạng thái sản phẩm: PENDING, APPROVED, IN_AUCTION, SOLD...
    private final String status;

    public ItemOverviewRow(Long id, String itemName, String description, String owner, String price, String minIncrement, String status) {
        this.id = id;
        this.itemName = itemName;
        this.description = description;
        this.owner = owner;
        this.price = price;
        this.minIncrement = minIncrement;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getItemName() {
        return itemName;
    }

    public String getDescription() {
        return description;
    }

    public String getOwner() {
        return owner;
    }

    public String getPrice() {
        return price;
    }

    public String getMinIncrement() {
        return minIncrement;
    }

    public String getStatus() {
        return status;
    }
}

// Một sản phẩm mà Bidder đã thắng, hiển thị ở vùng "sản phẩm sở hữu".
public static class OwnedProductRow {
    // Id sản phẩm đã thắng.
    private final Long itemId;
    // Id phiên tạo ra kết quả thắng.
    private final Long sessionId;
    // Tên sản phẩm.
    private final String itemName;
    // Mô tả ngắn hiển thị dưới tên sản phẩm.
    private final String description;
    // Giá thắng đã format.
    private final String finalPrice;
    // Trạng thái phiên/sản phẩm sau khi thắng.
    private final String status;

    public OwnedProductRow(Long itemId, Long sessionId, String itemName, String description, String finalPrice, String status) {
        this.itemId = itemId;
        this.sessionId = sessionId;
        this.itemName = itemName;
        this.description = description;
        this.finalPrice = finalPrice;
        this.status = status;
    }

    public Long getItemId() {
        return itemId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public String getItemName() {
        return itemName;
    }

    public String getDescription() {
        return description;
    }

    public String getFinalPrice() {
        return finalPrice;
    }

    public String getStatus() {
        return status;
    }
}

// Gói dữ liệu để render toàn bộ dashboard Admin sau khi background Task load xong.
public static class AdminDashboardData {
    // Số phiên đang chạy.
    public final long activeSessions;
    // Số sản phẩm đang chờ duyệt.
    public final long pendingItems;
    // Tổng số sản phẩm trong hệ thống.
    public final long totalItems;
    // Tổng số user trong hệ thống.
    public final long totalUsers;
    // Danh sách phiên hiển thị ở khu Admin.
    public final List<SessionOverviewRow> sessionRows;
    // Danh sách sản phẩm pending hiển thị ở khu Admin.
    public final List<ItemOverviewRow> pendingRows;

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

// Gói dữ liệu để render toàn bộ dashboard Seller sau khi background Task load xong.
public static class SellerDashboardData {
    // Tổng số sản phẩm của seller hiện tại.
    public final long totalItems;
    // Số sản phẩm đang chờ duyệt.
    public final long pendingItems;
    // Số sản phẩm đang trong phiên đấu giá.
    public final long inAuctionItems;
    // Số sản phẩm đã bán.
    public final long soldItems;
    // Danh sách sản phẩm của seller.
    public final List<ItemOverviewRow> itemRows;
    // Danh sách phiên liên quan tới sản phẩm của seller.
    public final List<SessionOverviewRow> sessionRows;

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

// Gói dữ liệu để render toàn bộ dashboard Bidder sau khi background Task load xong.
public static class BidderDashboardData {
    // Số dư còn dùng được để đặt giá.
    public final long availableBalance;
    // Số tiền đang bị khóa trong các stake.
    public final long lockedBalance;
    // Số stake đang khóa.
    public final long lockedStakeCount;
    // Số phiên mà bidder hiện đang dẫn.
    public final long leadingSessionCount;
    // Số phiên đang chạy.
    public final int liveSessionCount;
    // Số phiên đã kết thúc.
    public final int endedSessionCount;
    // Danh sách phiên đang chạy.
    public final List<LiveSessionRow> liveSessions;
    // Danh sách phiên đã kết thúc.
    public final List<EndedSessionRow> endedSessions;
    // Danh sách sản phẩm bidder đã thắng.
    public final List<OwnedProductRow> ownedProducts;

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
