package Client;

import Common.DataBase.entities.Item;
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

public class ApproveController {

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
    private TextField rejectReason;

    private final ItemService itemService = new ItemService();

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

    public void duyetsp() {
        Item selected = table.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showWarning("Chưa chọn sản phẩm.");
            return;
        }

        try {
            itemService.approve(selected.getId());
            showInfo("Đã duyệt sản phẩm ID: " + selected.getId());
            loadData();
        } catch (Exception e) {
            showWarning("Duyệt thất bại: " + e.getMessage());
        }
    }

    public void tuchoi() {
        Item selected = table.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showWarning("Chưa chọn sản phẩm.");
            return;
        }

        String reason = rejectReason == null ? "" : rejectReason.getText();

        try {
            itemService.reject(selected.getId(), reason);
            showInfo("Đã từ chối/xóa khỏi danh sách duyệt. Seller sẽ nhận được lý do.");
            if (rejectReason != null) {
                rejectReason.clear();
            }
            loadData();
        } catch (Exception e) {
            showWarning("Từ chối thất bại: " + e.getMessage());
        }
    }

    public void trolai(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/dashboard - Admin.fxml");
    }

    private void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
