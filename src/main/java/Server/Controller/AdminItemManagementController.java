package Server.Controller;

import Common.DataBase.entities.Item;
import Server.service.ItemService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
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
    private static final int PAGE_SIZE = 12;
    private int currentPage = 1;
    private int totalItems = 0;
    private boolean loading = false;

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
        loadPageAsync(1);
    }

    @FXML public void goFirstPage() { loadPageAsync(1); }
    @FXML public void goPrevPage() { loadPageAsync(currentPage - 1); }
    @FXML public void goNextPage() { loadPageAsync(currentPage + 1); }
    @FXML public void goLastPage() { loadPageAsync(getTotalPages()); }

    @FXML
    public void goToPage() {
        if (txtPageInput == null || txtPageInput.getText() == null) return;
        try {
            int page = Integer.parseInt(txtPageInput.getText().trim());
            loadPageAsync(page);
        } catch (NumberFormatException ignored) {
            loadPageAsync(currentPage);
        }
    }

    public void backToAdminDashboard(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/dashboard - Admin.fxml");
    }

    private void renderPage(int page, List<Item> pageRows) {
        int totalPages = getTotalPages();
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        currentPage = page;
        table.getItems().setAll(pageRows);

        if (lblPageInfo != null) {
            lblPageInfo.setText("Trang " + currentPage + " / " + totalPages);
        }
    }

    private int getTotalPages() {
        if (totalItems <= 0) return 1;
        return (totalItems + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    private void loadPageAsync(int requestedPage) {
        if (loading) return;
        loading = true;
        if (lblPageInfo != null) lblPageInfo.setText("Đang tải...");

        Task<PageData> task = new Task<>() {
            @Override
            protected PageData call() {
                int total = itemService.countAll();
                int totalPages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
                int safePage = Math.max(1, Math.min(requestedPage, totalPages));
                List<Item> rows = itemService.listAllPaged(safePage, PAGE_SIZE);
                return new PageData(total, safePage, rows);
            }
        };

        task.setOnSucceeded(event -> {
            PageData result = task.getValue();
            totalItems = result.totalItems;
            renderPage(result.page, result.rows);
            loading = false;
        });

        task.setOnFailed(event -> {
            loading = false;
            if (lblPageInfo != null) lblPageInfo.setText("Tải thất bại");
        });

        Thread worker = new Thread(task, "admin-item-page-load");
        worker.setDaemon(true);
        worker.start();
    }

    private static class PageData {
        private final int totalItems;
        private final int page;
        private final List<Item> rows;

        private PageData(int totalItems, int page, List<Item> rows) {
            this.totalItems = totalItems;
            this.page = page;
            this.rows = rows;
        }
    }

    private void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, UILogin.APP_WIDTH, UILogin.APP_HEIGHT));
        stage.show();
    }
}
