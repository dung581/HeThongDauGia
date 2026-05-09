package Server.Controller;

import Common.DataBase.entities.Auction;
import Common.DataBase.entities.Item;
import Common.DataBase.repository.ItemRepository;
import Common.Enum.AuctionState;
import Common.Enum.UserRole;
import Common.Model.user.UserAccount;
import Server.service.AuctionService;
import Server.service.BidService;
import Server.service.ItemService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
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

import static Client.util.NavigationUtil.switchScene;

public class danhSachDauGiaController {
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

    private TextField tiencuoc;

    private ItemRepository Repo = new ItemRepository();
    private final AuctionService auctionService = new AuctionService();

    @FXML
    public void initialize() {
        // Gán dữ liệu cho từng cột
        colId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getId()));
        colItemId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getItem_id()));
        colCurrentUserId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getCurrent_user_id()));
        colCurrentPrice.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getCurrent_price()));
        colStartTime.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getStartTime()));
        colEndTime.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getEndTime()));
        colState.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getState()));
        colItemName.setCellValueFactory(data -> {
            Item item = Repo.getItemById(data.getValue().getItem_id());
            return new SimpleStringProperty(item != null ? item.getFullname() : "N/A");
        });

        //nhan phan hoi khi an vao 1 phien dau gia
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldAuction, newAuction) -> {
                    if (newAuction != null) {
                        // Lấy thông tin sản phẩm từ phiên đấu giá

                        Item item = Repo.getItemById(newAuction.getItem_id());

                        // Hiển thị thông tin sản phẩm
                        idLabel.setText(String.valueOf(item.getId()));
                        tenLabel.setText(item.getFullname());
                        thongtinLabel.setText(item.getDescription());
                        giaLabel.setText(String.valueOf(item.getBeginPrice()));
                        trangthaiLabel.setText(item.getStatus().toString());
                    }
                });
        // Load dữ liệu từ service
        loadData();
    }


    public void loadData() {
        table.getItems().setAll(auctionService.getActive());
    }

    private final BidService bidService;
    {
        bidService = new BidService();
    }


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
            int tiendaugia = Integer.parseInt(tiencuoc.getText().trim());
            long accountid = UserAccount.getUserId();
            bidService.placeBid(accountid, selected.getId(), tiendaugia);
            showInfo("Dat gia thanh cong.");
        } catch (NumberFormatException e) {
            showWarning("So tien dat gia khong hop le.");
        } catch (Exception e) {
            showWarning("Dat gia that bai: " + e.getMessage());
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
        stage.setScene(new Scene(root));
        stage.show();
    }
}

