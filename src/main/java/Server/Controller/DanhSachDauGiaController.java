package Server.Controller;

import Client.Controller.UILogin;
import Client.util.AlertUtil;
import Common.DataBase.entities.Auction;
import Common.DataBase.entities.Item;
import Common.DataBase.repository.ItemRepository;
import Common.Enum.AuctionState;
import Common.Enum.UserRole;
import Common.Model.user.UserAccount;
import Server.service.AuctionService;
<<<<<<< HEAD
import Server.service.BidService;
=======
>>>>>>> a2a2de4 (chinh giao dien)
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
<<<<<<< HEAD

    @FXML private TextField tiencuoc;
    private BidService bidService = new BidService();
=======
    @FXML private Label lblPageInfo;
    @FXML private Node browseItemsNav;
    @FXML private Node uploadItemNav;

    @FXML private TextField txtPageInput;

>>>>>>> a2a2de4 (chinh giao dien)
    private ItemRepository Repo = new ItemRepository();
    private final AuctionService auctionService = new AuctionService();

    //chua list phien dau gia
    private List<Auction> allAuctions = new ArrayList<>();
    private List<Item> items = new ArrayList<>();
    private Map<Long, Item> itemById = new HashMap<>();
    private Map<Long, String> names = new HashMap<>();

    @FXML
    public void initialize() {
<<<<<<< HEAD
        // gán dữ liệu cho từng cột
=======
        // GÃ¡n dá»¯ liá»‡u cho tá»«ng cá»™t
        configureRoleUi();
>>>>>>> a2a2de4 (chinh giao dien)
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
<<<<<<< HEAD
=======

    private void configureRoleUi() {
        UserRole role = UserAccount.getCurrentRole();
        setVisibleManaged(browseItemsNav, role == UserRole.ADMIN);
        setVisibleManaged(uploadItemNav, role == UserRole.SELLER);
    }

    private void setVisibleManaged(Node node, boolean visible) {
        if (node == null) return;
        node.setVisible(visible);
        node.setManaged(visible);
    }
    public void loadData() {
        List<Auction> auctions = auctionService.getActive();

        table.getItems().setAll(auctions);

        LocalDateTime now = LocalDateTime.now();

        for (Auction a : auctions) {
            if (!now.isBefore(a.getEndTime())) {
                auctionService.closeSession(a.getId());
            }
        }
    }
>>>>>>> a2a2de4 (chinh giao dien)

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
<<<<<<< HEAD
            // gan du lieu len bang
            table.getItems().setAll(allAuctions);
=======
            itemNameById.clear();
            itemNameById.putAll(result.itemNames);
            itemById.clear();
            itemById.putAll(result.items);
            renderPage(1);
        });

        task.setOnFailed(event -> {
            allAuctions.clear();
            itemNameById.clear();
            itemById.clear();
            renderPage(1);
>>>>>>> a2a2de4 (chinh giao dien)
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
<<<<<<< HEAD
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
=======

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

    @FXML
    public void goToSessions(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/danhSachDauGia.fxml");
    }

    @FXML
    public void goToBrowseItems(ActionEvent actionEvent) throws IOException {
        if (UserAccount.getCurrentRole() != UserRole.ADMIN) {
            AlertUtil.showError("Chuc nang nay chi danh cho Admin.");
            return;
        }
        switchScene(actionEvent, "/com/template/hellfx/ItemBrowse.fxml");
    }

    @FXML
    public void goToAccount(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/account.fxml");
    }

    @FXML
    public void goToDeposit(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/Deposit.fxml");
    }

    @FXML
    public void goToUploadItem(ActionEvent actionEvent) throws IOException {
        if (UserAccount.getCurrentRole() != UserRole.SELLER) {
            AlertUtil.showError("Chuc nang nay chi danh cho nguoi ban.");
            return;
        }
        switchScene(actionEvent, "/com/template/hellfx/SellerProducts.fxml");
    }

    @FXML
    public void openSessionDetail(ActionEvent actionEvent) throws IOException {
        Auction selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showError("Vui long chon mot phien dau gia.");
            return;
        }

        SessionDetailController.setSessionId(selected.getId());
        switchScene(actionEvent, "/com/template/hellfx/session-detail.fxml");
>>>>>>> a2a2de4 (chinh giao dien)
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
        replaceSceneRoot(stage, root);
    }

    private void replaceSceneRoot(Stage stage, Parent root) {
        Scene currentScene = stage.getScene();
        if (currentScene == null) {
            stage.setScene(new Scene(root, UILogin.APP_WIDTH, UILogin.APP_HEIGHT));
        } else {
            currentScene.setRoot(root);
        }
        stage.show();
    }
<<<<<<< HEAD
    //------------------------------------------------------------------------------------------------------------------
}

=======
}
>>>>>>> a2a2de4 (chinh giao dien)
