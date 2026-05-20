package Server.Controller;

import Client.Controller.UILogin;
import Common.DataBase.entities.Auction;
import Common.DataBase.entities.Item;
import Common.DataBase.repository.ItemRepository;
import Common.Enum.AuctionState;
import Common.Enum.UserRole;
import Common.Model.user.UserAccount;
import Server.service.AuctionService;
import Server.service.BidService;
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
import lombok.Getter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

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
    private ItemRepository Repo = new ItemRepository();
    private final AuctionService auctionService = new AuctionService();

    //chua list phien dau gia
    private List<Auction> allAuctions = new ArrayList<>();
    private List<Item> items = new ArrayList<>();
    private Map<Long, Item> itemById = new HashMap<>();
    private Map<Long, String> names = new HashMap<>();

    @FXML
    public void initialize() {
        // gán dữ liệu cho từng cột
        colId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getId()));
        colItemId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getItem_id()));
        colCurrentUserId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getCurrent_user_id()));
        colCurrentPrice.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getCurrent_price()));
        colStartTime.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getStartTime()));
        colEndTime.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getEndTime()));
        colState.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getState()));
        colItemName.setCellValueFactory(data -> new SimpleStringProperty(names.get(data.getValue().getItem_id())));


        //nhan phan hoi khi an vao 1 phien dau gia
        table.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selectedAuction) -> {

                    if (selectedAuction != null) {
                        Item item = itemById.get(selectedAuction.getItem_id());
                        idLabel.setText(String.valueOf(item.getId()));
                        tenLabel.setText(item.getFullname());
                        thongtinLabel.setText(item.getDescription());
                        giaLabel.setText(String.valueOf(item.getBeginPrice()));
                        trangthaiLabel.setText(item.getStatus().toString());
                    }
                }
        );
        loadAuctionDataAsync();
    }

    @FXML
    private void loadAuctionDataAsync() {
        Task<LoadAuctionData> task = new Task<>() {
            @Override
            protected LoadAuctionData call() {
                List<Auction> auctions = auctionService.getActive();
                items = Repo.getAllItem();
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
            //lay cai ham call ben tren tra ve kìa
            LoadAuctionData result = task.getValue();
            //xoa list auction va cap nhat list moi
            itemById = result.getItemById();
            names = result.getNames();

            allAuctions.clear();
            allAuctions.addAll(result.auctions);
            // gan du lieu len bang
            table.getItems().setAll(allAuctions);
        });

        Thread worker = new Thread(task, "auction-list-load");
        worker.setDaemon(true);
        worker.start();
    }
    private static class LoadAuctionData {
        private final List<Auction> auctions;
        private final Map<Long, Item> itemById;
        private final Map<Long, String> names;
        private LoadAuctionData(List<Auction> auctions, Map<Long, String> names , Map<Long, Item> items) {
            this.auctions = auctions;
            this.itemById = items;
            this.names = names;
        }
        public Map<Long, Item> getItemById(){return itemById;}
        public  Map<Long, String> getNames(){return names;}
    }
    //hien thi loi -----------------------------------------------------------------------------------------------------
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
    // nut tro lại -----------------------------------------------------------------------------------------------------
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
    private void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, UILogin.APP_WIDTH, UILogin.APP_HEIGHT));
        stage.show();
    }
    //------------------------------------------------------------------------------------------------------------------
}

