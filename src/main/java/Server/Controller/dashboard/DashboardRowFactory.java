package Server.Controller.dashboard;

import Server.Controller.model.DashboardModels.EndedSessionRow;
import Server.Controller.model.DashboardModels.ItemOverviewRow;
import Server.Controller.model.DashboardModels.LiveSessionRow;
import Server.Controller.model.DashboardModels.OwnedProductRow;
import Server.Controller.model.DashboardModels.SessionOverviewRow;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

// Nhóm dựng Node/Card dùng chung cho các bảng dashboard.
final class DashboardRowFactory {
    private final DashBoardController controller;

    DashboardRowFactory(DashBoardController controller) {
        this.controller = controller;
    }

    // Tạo một thanh phiên đấu giá gồm tên item, id phiên, giá, leader và trạng thái.
    Node createAdminSessionNode(SessionOverviewRow row) {
        boolean live = isLiveLikeStatus(row.getStatus());
        return createMarketCard(
                row.getItemName(),
                controller.nullToText(row.getDescription(), "Không có mô tả"),
                "Phiên #" + row.getSessionId(),
                "Sản phẩm #" + row.getItemId(),
                statusPillText(row.getStatus()),
                "CURRENT BID",
                row.getPrice(),
                "LEADER",
                row.getLeader(),
                "STEP",
                row.getMinIncrement(),
                live ? "Join ->" : "Xem ->",
                source -> controller.openSessionDetailFromNode(source, row.getSessionId()),
                live
        );
    }

    // Tạo card phiên đang chạy cho dashboard Bidder.
    Node createLiveSessionCard(LiveSessionRow row) {
        HBox card = new HBox(16.0);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10.0, 16.0, 10.0, 16.0));
        card.getStyleClass().add("live-auction-card");
        card.setCursor(Cursor.HAND);
        card.setOnMouseClicked(event -> {
            // Nếu click xuất phát từ nút Join thì để nút tự xử lý, tránh mở chi tiết phiên 2 lần liên tiếp.
            if (!isButtonTarget(event.getTarget())) {
                controller.openSessionDetailFromNode(card, row.getSessionId());
            }
        });

        VBox main = new VBox(6.0);
        main.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(main, Priority.ALWAYS);

        Label title = new Label(row.getItemName());
        title.getStyleClass().add("live-auction-title");
        title.setWrapText(true);

        HBox meta = new HBox(12.0);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label seller = new Label("by " + row.getSeller());
        seller.getStyleClass().add("live-auction-meta");
        Label separator = new Label("|");
        separator.getStyleClass().add("live-auction-meta");
        Label session = new Label("Session #" + row.getSessionId());
        session.getStyleClass().add("live-auction-meta");
        meta.getChildren().addAll(seller, separator, session);

        HBox stats = new HBox(36.0);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().addAll(
                createLiveStat("LIVE", "START PRICE", row.getStartPrice(), "live-auction-start-price"),
                createLiveStat(null, "CURRENT BID", row.getCurrentPrice(), "live-auction-current-price"),
                createLiveStat(null, "TOTAL BIDS", String.valueOf(row.getTotalBids()), "live-auction-total-bids")
        );

        main.getChildren().addAll(title, meta, stats);

        VBox actionBox = new VBox(12.0);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        Button joinButton = new Button("Join ->");
        joinButton.getStyleClass().add("live-auction-join-button");
        joinButton.setOnAction(event -> {
            controller.openSessionDetailFromNode(joinButton, row.getSessionId());
            event.consume();
        });

        VBox timerBox = new VBox(2.0);
        timerBox.setAlignment(Pos.CENTER);
        timerBox.getStyleClass().add("live-auction-timer-box");
        Label timerTitle = new Label("ENDS IN");
        timerTitle.getStyleClass().add("live-auction-stat-label");
        Label time = new Label(row.getTimeLeft());
        time.getStyleClass().add("live-auction-timer");
        timerBox.getChildren().addAll(timerTitle, time);
        actionBox.getChildren().addAll(joinButton, timerBox);

        card.getChildren().addAll(main, actionBox);
        return card;
    }

    // Kiểm tra target click có nằm trong Button hay không, vì target đôi khi là text con của Button.
    private boolean isButtonTarget(Object target) {
        if (!(target instanceof Node node)) {
            return false;
        }
        while (node != null) {
            if (node instanceof Button) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    // Tạo một cụm chỉ số trong card phiên đang chạy: nhãn nhỏ ở trên, giá trị lớn ở dưới.
    private VBox createLiveStat(String pillText, String labelText, String valueText, String valueStyleClass) {
        VBox box = new VBox(3.0);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMinWidth(100.0);

        if (pillText != null && !pillText.isBlank()) {
            Label pill = new Label(pillText);
            pill.getStyleClass().add("live-auction-live-pill");
            box.getChildren().add(pill);
        }

        Label label = new Label(labelText);
        label.getStyleClass().add("live-auction-stat-label");
        Label value = new Label(valueText);
        value.getStyleClass().add(valueStyleClass);
        box.getChildren().addAll(label, value);
        return box;
    }

    // Tạo card phiên đã kết thúc cho dashboard Bidder.
    Node createEndedSessionCard(EndedSessionRow row) {
        return createMarketCard(
                row.getItemName(),
                controller.nullToText(row.getDescription(), "Không có mô tả"),
                "Winner " + row.getWinner(),
                "Phiên #" + row.getSessionId(),
                statusPillText(row.getStatus()),
                "FINAL PRICE",
                row.getFinalPrice(),
                "WINNER",
                row.getWinner(),
                "STEP",
                row.getMinIncrement(),
                "Xem ->",
                source -> controller.openWinnerFromNode(source, row.getSessionId()),
                false
        );
    }

    // Tạo card item tổng quan dùng cho dashboard Admin/Seller.
    Node createItemOverviewCard(ItemOverviewRow row, boolean showOwner) {
        String ownerMeta = showOwner && row.getOwner() != null && !row.getOwner().isBlank()
                ? "Seller #" + row.getOwner()
                : "Sản phẩm #" + row.getId();
        return createMarketCard(
                row.getItemName(),
                controller.nullToText(row.getDescription(), "Không có mô tả"),
                ownerMeta,
                "Sản phẩm #" + row.getId(),
                statusPillText(row.getStatus()),
                "START PRICE",
                row.getPrice(),
                "STEP",
                row.getMinIncrement(),
                "ITEM ID",
                "#" + row.getId(),
                null,
                null,
                isLiveLikeStatus(row.getStatus())
        );
    }

    // Tạo một thanh sản phẩm gồm tên, mô tả, id phiên/item, giá thắng và trạng thái.
    Node createOwnedProductNode(OwnedProductRow row) {
        return createMarketCard(
                row.getItemName(),
                controller.nullToText(row.getDescription(), "Không có mô tả"),
                "Phiên #" + row.getSessionId(),
                "Sản phẩm #" + row.getItemId(),
                statusPillText(row.getStatus()),
                "FINAL PRICE",
                row.getFinalPrice(),
                "SESSION",
                "#" + row.getSessionId(),
                "ITEM",
                "#" + row.getItemId(),
                "Xem ->",
                source -> controller.openWinnerFromNode(source, row.getSessionId()),
                false
        );
    }

    // Tạo card compact dùng chung cho item/phiên phụ: giống mẫu auction card nhưng không thay đổi nghiệp vụ click.
    private Node createMarketCard(
            String titleText,
            String descriptionText,
            String metaLeftText,
            String metaRightText,
            String pillText,
            String firstLabel,
            String firstValue,
            String secondLabel,
            String secondValue,
            String thirdLabel,
            String thirdValue,
            String actionText,
            java.util.function.Consumer<Node> action,
            boolean live
    ) {
        VBox card = new VBox(10.0);
        card.setPadding(new Insets(18.0, 24.0, 18.0, 24.0));
        card.getStyleClass().addAll("market-card", live ? "live-auction-card" : "auction-card-muted");
        card.setCursor(action == null ? Cursor.DEFAULT : Cursor.HAND);
        if (action != null) {
            card.setOnMouseClicked(event -> {
                // Card vẫn click được, nhưng click lên nút con không được bắn thêm event mở màn lần hai.
                if (!isButtonTarget(event.getTarget())) {
                    action.accept(card);
                }
            });
        }

        HBox header = new HBox(14.0);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(controller.nullToText(titleText, "Item"));
        title.getStyleClass().add("live-auction-title");
        title.setWrapText(true);
        HBox.setHgrow(title, Priority.ALWAYS);
        header.getChildren().add(title);

        if (action != null && actionText != null && !actionText.isBlank()) {
            Button actionButton = new Button(actionText);
            actionButton.getStyleClass().add("live-auction-join-button");
            actionButton.setOnAction(event -> {
                action.accept(actionButton);
                event.consume();
            });
            header.getChildren().add(actionButton);
        }

        Label description = new Label(controller.nullToText(descriptionText, "Không có mô tả"));
        description.getStyleClass().add("product-description");
        description.setWrapText(true);

        HBox meta = new HBox(14.0);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label left = new Label(controller.nullToText(metaLeftText, ""));
        left.getStyleClass().add("live-auction-meta");
        meta.getChildren().add(left);
        if (metaRightText != null && !metaRightText.isBlank()) {
            Label separator = new Label("|");
            separator.getStyleClass().add("live-auction-meta");
            Label right = new Label(metaRightText);
            right.getStyleClass().add("live-auction-meta");
            meta.getChildren().addAll(separator, right);
        }

        HBox stats = new HBox(24.0);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().addAll(
                createMarketStat(pillText, firstLabel, firstValue, "live-auction-start-price"),
                createMarketStat(null, secondLabel, secondValue, "live-auction-current-price"),
                createMarketStat(null, thirdLabel, thirdValue, "live-auction-total-bids")
        );

        card.getChildren().addAll(header, meta, description, stats);
        return card;
    }

    // Cụm số liệu compact cho các card phụ, nhỏ hơn card phiên live để không tràn ở cột phải.
    private VBox createMarketStat(String pillText, String labelText, String valueText, String valueStyleClass) {
        VBox box = new VBox(4.0);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMinWidth(92.0);

        if (pillText != null && !pillText.isBlank()) {
            Label pill = new Label(pillText);
            pill.getStyleClass().add("live-auction-live-pill");
            box.getChildren().add(pill);
        }

        Label label = new Label(controller.nullToText(labelText, ""));
        label.getStyleClass().add("live-auction-stat-label");
        Label value = new Label(controller.nullToText(valueText, "-"));
        value.getStyleClass().add(valueStyleClass);
        value.setWrapText(true);
        box.getChildren().addAll(label, value);
        return box;
    }

    // Các trạng thái này đại diện cho card đang diễn ra nên dùng viền xanh như mẫu.
    private boolean isLiveLikeStatus(String status) {
        if (status == null) {
            return false;
        }
        return status.equalsIgnoreCase("RUNNING") || status.equalsIgnoreCase("IN_AUCTION");
    }

    // Pill trên card: phiên/item đang chạy hiện LIVE, trạng thái còn lại giữ nguyên để không mất thông tin.
    private String statusPillText(String status) {
        return isLiveLikeStatus(status) ? "LIVE" : controller.nullToText(status, "STATUS");
    }
}
