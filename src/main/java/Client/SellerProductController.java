package Client;

import Common.DataBase.entities.Item;
import Common.Model.user.UserAccount;
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

public class SellerProductController {

    @FXML
    private TextField itemName;
    @FXML
    private TextField itemPrice;
    @FXML
    private TextField itemDescription;

    @FXML
    private TableView<Item> table;
    @FXML
    private TableColumn<Item, Long> colId;
    @FXML
    private TableColumn<Item, String> colName;
    @FXML
    private TableColumn<Item, Long> colPrice;
    @FXML
    private TableColumn<Item, String> colStatus;
    @FXML
    private TableColumn<Item, String> colReason;

    private final ItemService itemService = new ItemService();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getId()));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullname()));
        colPrice.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getBeginPrice()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
        colReason.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMota() == null ? "" : data.getValue().getMota()));
        loadMyItems();
    }

    public void loadMyItems() {
        table.getItems().setAll(itemService.listByOwner(UserAccount.getUserId()));
    }

    public void submitItem() {
        try {
            String name = itemName.getText() == null ? "" : itemName.getText().trim();
            String desc = itemDescription.getText() == null ? "" : itemDescription.getText().trim();
            long price = Long.parseLong(itemPrice.getText().trim());

            if (name.isEmpty()) {
                showWarning("Vui long nhap ten san pham.");
                return;
            }

            Item item = new Item();
            item.setFullname(name);
            item.setOwner_user_id(UserAccount.getUserId());
            item.setDescription(desc);
            item.setBeginPrice(price);
            item.setMota("Cho duyet");

            itemService.upload(item);
            showInfo("Da gui yeu cau dang ban. Cho admin duyet.");

            itemName.clear();
            itemPrice.clear();
            itemDescription.clear();
            loadMyItems();
        } catch (NumberFormatException e) {
            showWarning("Gia khong hop le.");
        } catch (Exception e) {
            showWarning("Dang ban that bai: " + e.getMessage());
        }
    }

    public void backToSellerDashboard(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/dashboard - Seller.fxml");
    }

    private void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
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
}
