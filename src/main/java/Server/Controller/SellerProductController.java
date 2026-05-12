package Server.Controller;

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
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SellerProductController {

    @FXML private TextField itemName;
    @FXML private TextField itemPrice;
    @FXML private TextField itemDescription;

    @FXML private TableView<Item> table;
    @FXML private TableColumn<Item, Long> colId;
    @FXML private TableColumn<Item, String> colName;
    @FXML private TableColumn<Item, Long> colPrice;
    @FXML private TableColumn<Item, String> colStatus;
    @FXML private TableColumn<Item, String> colReason;
    @FXML private Label lblPageInfo;
    @FXML private TextField txtPageInput;

    private final ItemService itemService = new ItemService();
    private final List<Item> allItems = new ArrayList<>();
    private static final int PAGE_SIZE = 8;
    private int currentPage = 1;

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
        allItems.clear();
        allItems.addAll(itemService.listByOwner(UserAccount.getUserId()));
        renderPage(1);
    }

    @FXML public void goFirstPage() { renderPage(1); }
    @FXML public void goPrevPage() { renderPage(currentPage - 1); }
    @FXML public void goNextPage() { renderPage(currentPage + 1); }
    @FXML public void goLastPage() { renderPage(getTotalPages()); }

    @FXML
    public void goToPage() {
        if (txtPageInput == null || txtPageInput.getText() == null) return;
        try {
            int page = Integer.parseInt(txtPageInput.getText().trim());
            renderPage(page);
        } catch (NumberFormatException ignored) {
            renderPage(currentPage);
        }
    }

    private void renderPage(int page) {
        int totalPages = getTotalPages();
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        currentPage = page;

        int fromIndex = (currentPage - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, allItems.size());
        List<Item> pageRows = fromIndex >= toIndex ? List.of() : allItems.subList(fromIndex, toIndex);
        table.getItems().setAll(pageRows);

        if (lblPageInfo != null) {
            lblPageInfo.setText("Trang " + currentPage + " / " + totalPages);
        }
    }

    private int getTotalPages() {
        if (allItems.isEmpty()) return 1;
        return (allItems.size() + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    public void submitItem() {
        try {
            String name = itemName.getText() == null ? "" : itemName.getText().trim();
            String desc = itemDescription.getText() == null ? "" : itemDescription.getText().trim();
            long price = Long.parseLong(itemPrice.getText().trim());

            if (name.isEmpty()) {
                showWarning("Vui lòng nhập tên sản phẩm.");
                return;
            }

            Item item = new Item();
            item.setFullname(name);
            item.setOwner_user_id(UserAccount.getUserId());
            item.setDescription(desc);
            item.setBeginPrice(price);
            item.setMota("Chờ duyệt");

            itemService.upload(item);
            showInfo("Đã gửi yêu cầu đăng bán. Chờ admin duyệt.");

            itemName.clear();
            itemPrice.clear();
            itemDescription.clear();
            loadMyItems();
        } catch (NumberFormatException e) {
            showWarning("Giá không hợp lệ.");
        } catch (Exception e) {
            showWarning("Đăng bán thất bại: " + e.getMessage());
        }
    }

    public void backToSellerDashboard(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/UILogin.fxml");
    }

    private void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, UILogin.APP_WIDTH, UILogin.APP_HEIGHT));
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