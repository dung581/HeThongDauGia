package Server.Controller;

import Common.DataBase.entities.Auction;
import Common.DataBase.entities.Item;
import Common.DataBase.repository.ItemRepository;
import Common.Enum.AuctionState;
import Common.Enum.UserRole;
import Common.Model.user.UserAccount;
import Server.service.AuctionService;
import Server.service.BidService;
import Server.service.StakeService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DanhSachDauGiaController {
    @FXML
    private TableView<Auction> table;

    @FXML
    private TableColumn<Auction, Long> colId;

    @FXML
    private TableColumn<Auction, Long> colItemId;

    @FXML
    private TableColumn<Auction, Long> colCurrentUserId;

    @FXML
    private TableColumn<Auction, Long> colCurrentPrice;

    @FXML
    private TableColumn<Auction, LocalDateTime> colStartTime;

    @FXML
    private TableColumn<Auction, LocalDateTime> colEndTime;

    @FXML
    private TableColumn<Auction, AuctionState> colState;

    @FXML
    private TableColumn<Auction, String> colItemName;


    @FXML private Label idLabel;
    @FXML private Label tenLabel;
    @FXML private Label giaLabel;
    @FXML private Label trangthaiLabel;
    @FXML private Label thongtinLabel;


    @FXML private TextField tiencuoc;

    private BidService bidService = new BidService();
    private StakeService stakeService = new StakeService();
    private ItemRepository Repo = new ItemRepository();
    private final AuctionService auctionService = new AuctionService();
    private final List<Auction> allAuctions = new ArrayList<>();
    private final Map<Long, String> itemNameById = new HashMap<>();
    private final Map<Long, Item> itemById = new HashMap<>();

    @FXML
    public void initialize() {
        // gắn giữ liệu vào bảng
        colId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getId()));
        colItemId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getItem_id()));
        colCurrentUserId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getCurrent_user_id()));
        colCurrentPrice.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getCurrent_price()));
        colStartTime.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getStartTime()));
        colEndTime.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getEndTime()));
        colState.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getState()));
        colItemName.setCellValueFactory(data ->
                new SimpleStringProperty(itemNameById.getOrDefault(data.getValue().getItem_id(), "N/A")));


        //nhan phan hoi khi an vao 1 phien dau gia
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldAuction, newAuction) -> {
            if (newAuction != null) {
                // lay thong tin sản phầm de hiển thị
                Item item = itemById.get(newAuction.getItem_id());
                if (item != null) {
                    idLabel.setText(String.valueOf(item.getId()));
                    tenLabel.setText(item.getFullname());
                    thongtinLabel.setText(item.getDescription());
                    giaLabel.setText(String.valueOf(item.getBeginPrice()));
                    trangthaiLabel.setText(item.getStatus().toString());
                }
            }
            loadAuctionDataAsync();
        });
    }


    //ham load data luu vao cac hashmap
    private void loadAuctionDataAsync() {
        Task<LoadAuctionData> task = new Task<>() {
            @Override
            protected LoadAuctionData call() {
                List<Auction> auctions = auctionService.getActive();
                List<Item> items = Repo.getAllItem();
                Map<Long, String> names = new HashMap<>();
                Map<Long, Item> itemMap = new HashMap<>();

                for (Item item : items) {
                    names.put(item.getId(), item.getFullname());
                    itemMap.put(item.getId(), item);
                }
                return new LoadAuctionData(auctions, names, itemMap);
            }
        };

        task.setOnSucceeded(event -> {
            LoadAuctionData result = task.getValue();
            allAuctions.clear();
            allAuctions.addAll(result.auctions);
            itemNameById.clear();
            itemNameById.putAll(result.itemNames);
            itemById.clear();
            itemById.putAll(result.items);

        });

        task.setOnFailed(event -> {
            allAuctions.clear();
            itemNameById.clear();

        });

        Thread worker = new Thread(task, "auction-list-load");
        worker.setDaemon(true);
        worker.start();
    }

    //nut tro lai
    public void trolai(ActionEvent actionEvent) throws IOException {
        UserRole role = UserAccount.getCurrentRole();
        if (role == UserRole.ADMIN) {
            switchScene(actionEvent, "/com/template/hellfx/dashboard - Admin.fxml");
        } else if (role == UserRole.SELLER) {
            switchScene(actionEvent, "/com/template/hellfx/dashboard - Seller.fxml");
        } else {
            switchScene(actionEvent, "/com/template/hellfx/dashboard-Bidder.fxml");
        }
    }

    //nut xac nhan
    public void submit() {
        UserRole role = UserAccount.getCurrentRole();
        if (role == UserRole.SELLER) {
            showWarning("Seller khong duoc mua/dau gia.");
            return;
        }

        Auction selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Vui long chon san pham truoc khi dat gia.");
            return;
        }

        try {
            long tiendaugia = Integer.parseInt(tiencuoc.getText().trim());
            long accountid = UserAccount.getUserId();
            bidService.placeBid(accountid, selected.getId(), tiendaugia);

            //tao lich su dat gia
            stakeService.createStake(accountid ,selected.getItem_id(), tiendaugia );

            showInfo("Dat gia thanh cong.");
        } catch (NumberFormatException e) {
            showWarning("So tien dat gia khong hop le.");
        } catch (Exception e) {
            showWarning("Dat gia that bai: " + e.getMessage());
        }
    }

    private static class LoadAuctionData {
        private final List<Auction> auctions;
        private final Map<Long, String> itemNames;
        private final Map<Long, Item> items;

        private LoadAuctionData(List<Auction> auctions, Map<Long, String> itemNames,Map<Long, Item> items) {
            this.auctions = auctions;
            this.itemNames = itemNames;
            this.items = items;
        }
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Thong bao");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thong bao");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, UILogin.APP_WIDTH, UILogin.APP_HEIGHT));
        stage.show();
    }
}

