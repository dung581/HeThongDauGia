package Server.Controller;

import Common.DataBase.entities.Item;
import Common.Enum.ItemStatus;
import Common.Enum.UserRole;
import Common.Model.user.UserAccount;
import Server.service.AuctionService;
import Server.service.ItemService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class ItemBrowseController {
    @FXML private TableView<Item> table;
    @FXML private TableColumn<Item, Long> colId;
    @FXML private TableColumn<Item, String> colName;
    @FXML private TableColumn<Item, Long> colPrice;
    @FXML private TableColumn<Item, String> colDescription;
    @FXML private TableColumn<Item, String> colNote;
    @FXML private TableColumn<Item, String> colStatus;
    @FXML private ChoiceBox<String> statusFilter;
    @FXML private TextField rejectReason;
    @FXML private TextField endTime;
    @FXML private Label lblRole;
    @FXML private Label lblTotal;

    private final ItemService itemService = new ItemService();
    private final AuctionService auctionService = new AuctionService();

    private static final String ALL = "TAT_CA";

    @FXML
    public void initialize(){
        colId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getId()));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullname()));
        colPrice.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getBeginPrice()));
        colDescription.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription() == null ? "" : data.getValue().getDescription()));
        colNote.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMota() == null ? "" : data.getValue().getMota()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus() == null ? "" : data.getValue().getStatus().name()));

        // bo loc status
        if (statusFilter != null){
            ObservableList<String> options = FXCollections.observableArrayList(ALL, ItemStatus.PENDING.name(), ItemStatus.APPROVED.name(), ItemStatus.IN_AUCTION.name(), ItemStatus.SOLD.name(), ItemStatus.REJECTED.name());
            statusFilter.setItems(options);
            UserRole role = UserAccount.getCurrentRole();
            if (role == UserRole.ADMIN){
                statusFilter.setValue(ItemStatus.PENDING.name()); //ADMIN mac dinh xem Pending de duyet
            }else{
                statusFilter.setValue(ItemStatus.APPROVED.name()); // Role khac se xem APPROVED
            }
            statusFilter.setOnAction(e -> loadData());
        }
        if (lblRole != null){
            UserRole role = UserAccount.getCurrentRole();
            lblRole.setText("Vai trò: "+(role == null ? "?" : role.name()));
        }
        loadData();
    }
    // tai du lieu bo loc
    public void loadData(){
        try {
            String filter = (statusFilter == null || statusFilter.getValue() == null) ? ItemStatus.PENDING.name() : statusFilter.getValue();
            List<Item> data;
            if (ALL.equals(filter)){
                data = mergeAll();
            }
            else {
                ItemStatus status = ItemStatus.valueOf(filter);
                data = listByStatusAuto(status);
            }

            //Seller chi xem duoc item cua minh
            UserRole role = UserAccount.getCurrentRole();
            if (role == UserRole.SELLER){
                long myId = UserAccount.getUserId();
                data.removeIf(it -> it.getOwner_user_id() != myId);
            }

            table.getItems().setAll(data);

            if (lblRole != null) {
                lblTotal.setText("Tổng: " + data.size() + " sản phẩm");
            }
        }catch (Exception e){
            showWarning("Không tải được danh sách: "+e.getMessage());
        }
    }
    private List<Item> mergeAll(){
        List<Item> all = itemService.listPending();
        all.addAll(itemService.listApproved());
        return all;
    }

    private List<Item> listByStatusAuto(ItemStatus status){
        switch (status){
            case PENDING:
                return itemService.listPending();
            case APPROVED:
                return itemService.listApproved();
            default:
                List<Item> base = mergeAll();
                base.removeIf(i-> i.getStatus() != status);
                return base;
        }
    }

    // Admin click vao Approve -> tao luon phien dau gia
    public void onApprove() {
        if (!requireAdmin()) return;

        Item selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Chua chon san pham.");
            return;
        }

        if (selected.getStatus() != ItemStatus.PENDING) {
            showWarning("Chi duyet duoc item dang o trang thai PENDING.");
            return;
        }

        // Yeu cau nhap so gio dau gia
        int hours;
        try{
            hours = Integer.parseInt(endTime == null ? "" :endTime.getText().trim());
            if (hours <= 0){
                showWarning("Số giờ đấu giá phải > 0.");
                return;
            }
        }catch (NumberFormatException e){
            showWarning("Vui lòng nhập số giờ hợp lệ cho phiên đấu giá");
            return;
        }

        try{
            itemService.approve(selected.getId());

            LocalDateTime endAt = LocalDateTime.now().plus(Duration.ofHours(hours));
            auctionService.createSession(selected.getId(), endAt);

            showInfo("Đã duyệt sản phẩm ID" +selected.getId() + " và mở phiên đấu giá.");
            loadData();
        } catch (Exception e){
            showWarning("Duyệt thất bại: " + e.getMessage());
        }
    }

    //Admin click vao Reject
    public void onReject() {
        if(!requireAdmin()) return;

        Item selected = table.getSelectionModel().getSelectedItem();
        if (selected == null){
            showWarning("Chưa chọn sản phẩm");
            return;
        }
        if(selected.getStatus() != ItemStatus.PENDING){
            showWarning("Chỉ từ chối được item đang ở ngoài trạng thái Pending.");
            return;
        }
        String reason = rejectReason == null ? "" :rejectReason.getText();

        try{
            itemService.reject(selected.getId(), reason);
            showInfo("Đã từ chối sản phẩm ID "+selected.getId() + ".");
            if (rejectReason != null){
                rejectReason.clear();
            }
            loadData();
        }catch (Exception e){
            showWarning("Từ chối thất bại: "+e.getMessage());
        }
    }

    // voi cac san pham APPROVED, Admin mở phiên đấu giá
    public void onCreateSession(){
        if (!requireAdmin()) return;

        Item selected = table.getSelectionModel().getSelectedItem();
        if (selected == null){
            showWarning("Chưa chọn sản phẩm");
            return;
        }
        if (selected.getStatus() != ItemStatus.APPROVED){
            showWarning("Chỉ mở phiên đấu giá với các item dang APPROVED");
            return;
        }

        int hours;
        try {
            hours = Integer.parseInt(endTime == null ? "" : endTime.getText().trim());
            if (hours <= 0) {
                showWarning("Số giờ phải > 0.");
                return;
            }
        } catch (NumberFormatException e) {
            showWarning("Vui lòng nhập số giờ hợp lệ.");
            return;
        }

        try {
            LocalDateTime endAt = LocalDateTime.now().plus(Duration.ofHours(hours));
            auctionService.createSession(selected.getId(), endAt);
            showInfo("Đã mở phiên đấu giá cho sản phẩm ID " + selected.getId() + ".");
            loadData();
        } catch (Exception e) {
            showWarning("Mở phiên thất bại: " + e.getMessage());
        }
    }

    // Trở về trang trước với role hiện tại
    public void onBack(ActionEvent actionEvent) throws IOException {
        UserRole role = UserAccount.getCurrentRole();
        String target;
        if (role == UserRole.ADMIN) {
            target = "/com/template/hellfx/dashboard - Admin.fxml";
        } else if (role == UserRole.SELLER) {
            target = "/com/template/hellfx/dashboard - Seller.fxml";
        } else {
            target = "/com/template/hellfx/dashboard-Bidder.fxml";
        }
        switchScene(actionEvent, target);
    }
    private boolean requireAdmin() {
        UserRole role = UserAccount.getCurrentRole();
        if (role != UserRole.ADMIN) {
            showWarning("Chuc nang nay chi danh cho Admin.");
            return false;
        }
        return true;
    }

    // Các hàm helper
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
