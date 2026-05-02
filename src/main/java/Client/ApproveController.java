package Client;

import Common.DataBase.entities.Item;
import Server.service.ItemService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.io.IOException;

import static Client.util.NavigationUtil.switchScene;

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

    private ItemService itemService = new ItemService();

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


    public void duyetsp() {
        Item selected = table.getSelectionModel().getSelectedItem();

        if (selected == null) {
            System.out.println("Chưa chọn sản phẩm 😅");
            return;
        }

        // gọi backend duyệt sp
        itemService.approve(selected.getId());

        System.out.println("Đã duyệt ID: " + selected.getId());

        // reload lại bảng
        loadData();
    }

    public void trolai(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/dashbroad - Admin.fxml");
    }
}
