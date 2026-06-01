package Server.Controller;

import Common.DataBase.entities.Auction;
import Common.DataBase.entities.Item;
import Common.Enum.AuctionState;
import Server.Controller.model.DashboardModels.EndedSessionRow;
import Server.Controller.model.DashboardModels.ItemOverviewRow;
import Server.Controller.model.DashboardModels.LiveSessionRow;
import Server.Controller.model.DashboardModels.OwnedProductRow;
import Server.Controller.model.DashboardModels.SessionOverviewRow;
import Server.service.AccountService;

import java.time.LocalDateTime;
import java.util.List;

// Nhóm chuyển entity sang model hiển thị của dashboard.
final class DashboardDataMapper {
    private final DashBoardController controller;

    DashboardDataMapper(DashBoardController controller) {
        this.controller = controller;
    }

    // Helper cho dashboard: phiên live là RUNNING và chưa quá end_time.
    boolean isLiveSession(Auction session) {
        return session.getState() == AuctionState.RUNNING
                && session.getEndTime() != null
                && session.getEndTime().isAfter(LocalDateTime.now());
    }

    // Helper cho dashboard: phiên ended là không RUNNING hoặc đã quá end_time.
    boolean isEndedSession(Auction session) {
        return session.getState() != AuctionState.RUNNING
                || session.getEndTime() == null
                || !session.getEndTime().isAfter(LocalDateTime.now());
    }

    // Đổi trạng thái session thành text ngắn để render ở dashboard.
    String displaySessionStatus(Auction session) {
        if (session.getState() == AuctionState.RUNNING && !isLiveSession(session)) {
            return "ENDED";
        }
        return session.getState() == null ? "" : session.getState().name();
    }

    // Tạo row tổng quan phiên từ dữ liệu service trả về.
    SessionOverviewRow toSessionOverviewRow(Auction session, List<Item> items) {
        String leader = session.getCurrent_user_id() == 0 ? "-" : String.valueOf(session.getCurrent_user_id());
        return new SessionOverviewRow(
                session.getId(),
                session.getItem_id(),
                findItemName(items, session.getItem_id()),
                findItemDescription(items, session.getItem_id()),
                controller.formatMoney(session.getCurrent_price()),
                findItemMinIncrement(items, session.getItem_id()),
                leader,
                displaySessionStatus(session)
        );
    }

    // Tạo row tổng quan item cho ListView dashboard.
    ItemOverviewRow toItemOverviewRow(Item item, boolean includeOwner) {
        return new ItemOverviewRow(
                item.getId(),
                item.getFullname(),
                controller.nullToText(item.getDescription(), "Không có mô tả"),
                includeOwner ? String.valueOf(item.getOwner_user_id()) : "",
                controller.formatMoney(item.getBeginPrice()),
                controller.formatMoney(getEffectiveMinIncrement(item)),
                item.getStatus() == null ? "" : item.getStatus().name()
        );
    }

    // Tạo row phiên live của Bidder; nếu user đang dẫn thì hiển thị "Ban".
    LiveSessionRow toLiveSessionRow(
            Auction session,
            long userId,
            List<Item> items,
            List<AccountService.ManagedAccount> accounts
    ) {
        String leader = session.getCurrent_user_id() == 0
                ? "-"
                : session.getCurrent_user_id() == userId ? "Ban" : String.valueOf(session.getCurrent_user_id());
        Item item = findItem(items, session.getItem_id());
        int totalBids = controller.bidService.getHistoryBySession(session.getId()).size();
        return new LiveSessionRow(
                session.getId(),
                findItemName(items, session.getItem_id()),
                findItemDescription(items, session.getItem_id()),
                findSellerDisplay(item, accounts),
                item == null ? "0" : controller.formatMoney(item.getBeginPrice()),
                controller.formatMoney(session.getCurrent_price()),
                findItemMinIncrement(items, session.getItem_id()),
                leader,
                totalBids,
                session.getEndTime()
        );
    }

    // Tạo row phiên ended của Bidder; nếu user thắng/dẫn cuối thì hiển thị "Ban".
    EndedSessionRow toEndedSessionRow(Auction session, long userId, List<Item> items) {
        String winner = session.getCurrent_user_id() == 0
                ? "-"
                : session.getCurrent_user_id() == userId ? "Ban" : String.valueOf(session.getCurrent_user_id());
        String status = session.getState() == AuctionState.RUNNING ? "ENDED" : session.getState().name();
        return new EndedSessionRow(
                session.getId(),
                findItemName(items, session.getItem_id()),
                findItemDescription(items, session.getItem_id()),
                controller.formatMoney(session.getCurrent_price()),
                findItemMinIncrement(items, session.getItem_id()),
                winner,
                status
        );
    }

    // Tạo row sản phẩm Bidder đã thắng để render ở khu owned products.
    OwnedProductRow toOwnedProductRow(Auction session, List<Item> items) {
        Item item = findItem(items, session.getItem_id());
        String itemName = item == null || item.getFullname() == null || item.getFullname().isBlank()
                ? "Item " + session.getItem_id()
                : item.getFullname();
        String description = item == null || item.getDescription() == null || item.getDescription().isBlank()
                ? "San pham da thang tu phien #" + session.getId()
                : item.getDescription();
        String status = session.getState() == AuctionState.PAID ? "DA THANG" : "WON";
        return new OwnedProductRow(
                session.getItem_id(),
                session.getId(),
                itemName,
                description,
                controller.formatMoney(session.getCurrent_price()),
                status
        );
    }

    // Tìm tên item theo itemId trong danh sách đã load sẵn.
    String findItemName(List<Item> items, long itemId) {
        if (items == null) {
            return "Item " + itemId;
        }
        return items.stream()
                .filter(item -> item.getId() == itemId)
                .map(Item::getFullname)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse("Item " + itemId);
    }

    // Tìm mô tả item theo itemId trong danh sách đã load sẵn.
    String findItemDescription(List<Item> items, long itemId) {
        if (items == null) {
            return "Không có mô tả";
        }
        return items.stream()
                .filter(item -> item.getId() == itemId)
                .map(Item::getDescription)
                .filter(description -> description != null && !description.isBlank())
                .findFirst()
                .orElse("Không có mô tả");
    }

    // Tìm bước giá tối thiểu, fallback 1 cho dữ liệu cũ chưa có minIncrement.
    String findItemMinIncrement(List<Item> items, long itemId) {
        return controller.formatMoney(getEffectiveMinIncrement(findItem(items, itemId)));
    }

    // Chuẩn hóa minIncrement cho dữ liệu cũ.
    long getEffectiveMinIncrement(Item item) {
        return item == null || item.getMinIncrement() <= 0 ? 1L : item.getMinIncrement();
    }

    // Tìm item theo id trong list đã có để không query lặp.
    Item findItem(List<Item> items, long itemId) {
        if (items == null) {
            return null;
        }
        return items.stream()
                .filter(item -> item.getId() == itemId)
                .findFirst()
                .orElse(null);
    }

    // Hiển thị username seller trong card phiên live; fallback về owner id nếu chưa tìm được account.
    String findSellerDisplay(Item item, List<AccountService.ManagedAccount> accounts) {
        if (item == null || item.getOwner_user_id() <= 0) {
            return "@seller";
        }
        if (accounts != null) {
            for (AccountService.ManagedAccount account : accounts) {
                if (account.getUserId() != null
                        && account.getUserId() == item.getOwner_user_id()
                        && account.getUsername() != null
                        && !account.getUsername().isBlank()) {
                    return "@" + account.getUsername();
                }
            }
        }
        return "@seller_" + item.getOwner_user_id();
    }
}
