package Server.Controller.dashboard;

import Server.Controller.model.DashboardModels.EndedSessionRow;
import Server.Controller.model.DashboardModels.ItemOverviewRow;
import Server.Controller.model.DashboardModels.LiveSessionRow;
import Server.Controller.model.DashboardModels.SessionOverviewRow;
import javafx.scene.control.ListCell;

// Nhóm cấu hình ListView cho dashboard Bidder/Admin/Seller.
final class DashboardTableSection {
    private final DashBoardController controller;

    DashboardTableSection(DashBoardController controller) {
        this.controller = controller;
    }

    // Cấu hình các danh sách riêng của dashboard Bidder.
    void configureBidderTables() {
        if (controller.liveSessionTable != null) {
            controller.liveSessionTable.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(LiveSessionRow row, boolean empty) {
                    super.updateItem(row, empty);
                    setText(null);
                    setGraphic(empty || row == null ? null : controller.createLiveSessionCard(row));
                }
            });
        }

        if (controller.endedSessionTable != null) {
            controller.endedSessionTable.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(EndedSessionRow row, boolean empty) {
                    super.updateItem(row, empty);
                    setText(null);
                    setGraphic(empty || row == null ? null : controller.createEndedSessionCard(row));
                }
            });
        }
    }

    // Cấu hình danh sách item đang chờ duyệt của Admin.
    void configureAdminTables() {
        if (controller.adminPendingItemTable != null) {
            controller.adminPendingItemTable.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(ItemOverviewRow row, boolean empty) {
                    super.updateItem(row, empty);
                    setText(null);
                    setGraphic(empty || row == null ? null : controller.createItemOverviewCard(row, true));
                }
            });
        }
    }

    // Cấu hình danh sách item và phiên liên quan của Seller.
    void configureSellerTables() {
        if (controller.sellerItemTable != null) {
            controller.sellerItemTable.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(ItemOverviewRow row, boolean empty) {
                    super.updateItem(row, empty);
                    setText(null);
                    setGraphic(empty || row == null ? null : controller.createItemOverviewCard(row, false));
                }
            });
        }

        if (controller.sellerSessionTable != null) {
            controller.sellerSessionTable.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(SessionOverviewRow row, boolean empty) {
                    super.updateItem(row, empty);
                    setText(null);
                    setGraphic(empty || row == null ? null : controller.createAdminSessionNode(row));
                }
            });
        }
    }
}
