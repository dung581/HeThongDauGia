package Client;

import Common.DataBase.entities.Item;
import Common.Enum.UserRole;
import Common.Model.user.UserAccount;
import Server.service.BidService;
import Server.service.ItemService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

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

    private final ItemService itemService = new ItemService();
    private final BidService bidService = new BidService();

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
        table.getItems().setAll(itemService.listApproved());
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

        Item selected = table.getSelectionModel().getSelectedItem();
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

