package Server.Controller.account;

import Common.DataBase.entities.Stake;
import Common.Model.user.UserAccount;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

// Nhóm lịch sử stake của tài khoản cá nhân.
final class AccountStakeSection {
    private final AccountController controller;

    AccountStakeSection(AccountController controller) {
        this.controller = controller;
    }

    // Cấu hình lịch sử stake thành danh sách card.
    void configureStakeList() {
        if (controller.stakeTable == null) {
            return;
        }
        controller.stakeTable.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Stake stake, boolean empty) {
                super.updateItem(stake, empty);
                setText(null);
                setGraphic(empty || stake == null ? null : controller.createStakeCard(stake));
            }
        });
    }

    // Tạo card hiển thị một stake.
    Node createStakeCard(Stake stake) {
        HBox row = new HBox(14.0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14.0, 16.0, 14.0, 16.0));
        row.getStyleClass().add("data-row");

        VBox main = new VBox(5.0);
        HBox.setHgrow(main, Priority.ALWAYS);
        Label title = new Label("Đặt cọc #" + stake.getId());
        title.getStyleClass().add("data-title");
        HBox meta = new HBox(10.0);
        Label auction = new Label("Đấu giá #" + stake.getAution_id());
        auction.getStyleClass().add("data-meta");
        Label item = new Label("Sản phẩm #" + stake.getLocked_item_id());
        item.getStyleClass().add("data-meta");
        Label user = new Label("Người dùng #" + stake.getUser_id());
        user.getStyleClass().add("data-meta");
        meta.getChildren().addAll(auction, item, user);
        main.getChildren().addAll(title, meta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox value = new VBox(6.0);
        value.setAlignment(Pos.CENTER_RIGHT);
        Label amount = new Label(String.format("%,d", stake.getAmount()));
        amount.getStyleClass().add("data-money");
        Label status = new Label(stake.getStatus() == null ? "" : stake.getStatus().name());
        status.getStyleClass().add("data-pill");
        value.getChildren().addAll(amount, status);

        row.getChildren().addAll(main, spacer, value);
        return row;
    }

    // Cấu hình tìm kiếm/lọc lịch sử stake cho tài khoản cá nhân.
    void configureStakeFilters() {
        if (controller.stakeStatusFilter != null) {
            controller.stakeStatusFilter.setItems(FXCollections.observableArrayList(AccountController.ALL_FILTER, "LOCKED", "RELEASED", "WON"));
            if (controller.stakeStatusFilter.getValue() == null) {
                controller.stakeStatusFilter.setValue(AccountController.ALL_FILTER);
            }
            controller.stakeStatusFilter.setOnAction(event -> controller.applyStakeFilters());
        }

        if (controller.stakeSearchField != null) {
            controller.stakeSearchField.textProperty()
                    .addListener((observable, oldValue, newValue) -> controller.applyStakeFilters());
        }
    }

    // Tải lịch sử stake của user hiện tại trên background thread.
    void loadStakeDataAsync() {
        Task<List<Stake>> task = new Task<>() {
            @Override
            protected List<Stake> call() {
                return controller.stakeService.getUserStakes(UserAccount.getUserId());
            }
        };

        task.setOnSucceeded(event -> {
            controller.allStakeRows.clear();
            controller.allStakeRows.addAll(task.getValue());
            controller.applyStakeFilters();
        });

        task.setOnFailed(event -> {
            if (controller.stakeTable != null) {
                controller.stakeTable.setPlaceholder(new Label("Không tải được lịch sử đặt cọc."));
            }
        });

        Thread worker = new Thread(task, "account-stake-load");
        worker.setDaemon(true);
        worker.start();
    }

    // Lọc lịch sử stake theo keyword và trạng thái.
    void applyStakeFilters() {
        if (controller.stakeTable == null) {
            return;
        }

        String query = controller.normalize(controller.stakeSearchField == null ? "" : controller.stakeSearchField.getText());
        String status = controller.stakeStatusFilter == null ? AccountController.ALL_FILTER : controller.stakeStatusFilter.getValue();
        List<Stake> rows = new ArrayList<>();

        for (Stake stake : controller.allStakeRows) {
            if (!matchesStakeStatus(stake, status)) {
                continue;
            }
            if (!matchesStakeSearch(stake, query)) {
                continue;
            }
            rows.add(stake);
        }

        controller.stakeTable.setItems(FXCollections.observableArrayList(rows));
    }

    // Kiểm tra trạng thái stake có khớp bộ lọc không.
    boolean matchesStakeStatus(Stake stake, String selectedStatus) {
        if (selectedStatus == null || selectedStatus.equals(AccountController.ALL_FILTER)) {
            return true;
        }
        return stake.getStatus() != null && stake.getStatus().name().equals(selectedStatus);
    }

    // Kiểm tra keyword theo ID stake, auction, item, user, số tiền và trạng thái.
    boolean matchesStakeSearch(Stake stake, String query) {
        if (query.isEmpty()) {
            return true;
        }

        String target = String.join(" ",
                String.valueOf(stake.getId()),
                String.valueOf(stake.getAution_id()),
                String.valueOf(stake.getLocked_item_id()),
                String.valueOf(stake.getUser_id()),
                String.valueOf(stake.getAmount()),
                stake.getStatus() == null ? "" : stake.getStatus().name()
        );
        return controller.normalize(target).contains(query);
    }
}
