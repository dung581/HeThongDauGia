package Client;

import Common.DataBase.entities.Bid;
import Common.DataBase.entities.Item;
import Common.DataBase.repository.AuctionRepository;
import Common.Model.user.UserAccount;
import Server.service.AuctionService;
import Server.service.BidService;
import Server.service.ItemService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;


public class danhSachDauGiaController {
    @FXML
    private TableView<Item> table;
    @FXML
    private TableColumn<Item, Long> colId;
    @FXML
    private TableColumn<Item, String> colName;
    @FXML
    private TableColumn<Item, Long> colPrice;
    @FXML
    private TableColumn<Item, String> colDetail;
    @FXML
    private TableColumn<Item, String> colDescription;
    @FXML
    private TableColumn<Item, String> colStatus;
    @FXML
    private TextField tiencuoc;

    private ItemService itemService = new ItemService();
    private BidService bidService = new BidService();
    // tu dong chay khi mo scene duyet
    @FXML
    public void initialize() {
        colId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getId()));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullname()));
        colPrice.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getBeginPrice()));
        colDescription.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().toString()));
        colDetail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMota()));

        loadData();
    }
    public void loadData() {
        table.getItems().setAll(itemService.listPending());
    }

    public void trolaiA(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/dashbroad - Admin.fxml");
    }
    public void trolaiB(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/dashbroad-Bidder.fxml");
    }
    public void trolaiS(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/dashbroad - Seller.fxml");
    }

    public void submit(){
        int tiendaugia = Integer.parseInt(tiencuoc.getText());
        long accountid = UserAccount.getUserId(); // --> lay id nguoi đặt giá

        Item seclected = table.getSelectionModel().getSelectedItem();
        long IteamId = seclected.getId();
        bidService.placeBid(accountid , IteamId,tiendaugia);

    }

    private void switchScene(ActionEvent actionEvent, String s) {
    }

    public class SessionListController {

        @FXML
        private VBox sessionsContainer;

        // Hàm này tự chạy khi load UI
        @FXML
        public void initialize() {
            addOneItem(); // test thêm 1 item
        }


        // 👉 Hàm tạo 1 item
        private HBox createItem(String name, String price) {
            Label nameLabel = new Label(name);
            nameLabel.setStyle("-fx-font-weight: bold;");

            Label priceLabel = new Label(price);

            Button viewBtn = new Button("Xem");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox item = new HBox(10, nameLabel, spacer, priceLabel, viewBtn);
            item.setStyle("-fx-background-color: #eaf2f8; -fx-padding: 10; -fx-background-radius: 8;");

            return item;
        }

        // 👉 Hàm thêm 1 item vào VBox
        public void addOneItem() {
            HBox item = createItem("Session #101", "1.200.000đ");
            sessionsContainer.getChildren().add(item);
        }
    }
}
