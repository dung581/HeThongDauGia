package Server.Controller;

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
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AdminItemManagementController {

    @FXML private TableView<Item> table;
    @FXML private TableColumn<Item, Long> colId;
    @FXML private TableColumn<Item, String> colName;
    @FXML private TableColumn<Item, Long> colOwnerId;
    @FXML private TableColumn<Item, Long> colPrice;
    @FXML private TableColumn<Item, String> colStatus;
    @FXML private TableColumn<Item, String> colDescription;
    @FXML private TableColumn<Item, String> colReason;
    @FXML private Label lblPageInfo;
    @FXML private TextField txtPageInput;

    private final ItemService itemService = new ItemService();
    private final List<Item> allItems = new ArrayList<>();
    private static final int PAGE_SIZE = 12;
    private int currentPage = 1;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getId()));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullname()));
        colOwnerId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getOwner_user_id()));
        colPrice.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getBeginPrice()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
        colDescription.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDescription() == null ? "" : data.getValue().getDescription()
        ));
        colReason.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getMota() == null ? "" : data.getValue().getMota()
        ));

        loadAllItems();
    }

    public void loadAllItems() {
        allItems.clear();
        allItems.addAll(itemService.listAll());
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

    public void backToAdminDashboard(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/dashboard - Admin.fxml");
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

    private void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, UILogin.APP_WIDTH, UILogin.APP_HEIGHT));
        stage.show();
    }
}

