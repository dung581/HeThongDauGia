package Server.Controller;


import Common.DataBase.entities.Auction;
import Common.DataBase.entities.Item;
import Common.Enum.AuctionState;
import Common.Enum.UserRole;
import Common.Model.user.UserAccount;
import Server.service.AuctionService;
import Server.service.ItemService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class WinnerController{
    @FXML private Label lblTitle;
    @FXML private Label lblItemName;
    @FXML private Label lblWinningPrice;
    @FXML private Label lblWinnerId;
    @FXML private Label lblNote;

    private final AuctionService auctionService = new AuctionService();
    private final ItemService itemService = new ItemService();
    private static long pendingSessionId = 0L;

    public static void setSessionId(long sessionId) {
        pendingSessionId = sessionId;
    }

    @FXML
    public void initialize(){
        long sessionId = pendingSessionId;
        if (sessionId <= 0) {
            if (lblTitle != null) {
                lblTitle.setText("Chưa có phiên đấu giá nào kết thúc");
                return;
            }
        }
        try{
            Auction auction = auctionService.getById(sessionId);
            if(auction == null){
                showError("Không tìm thấy phiên đấu giá");
                return;
            }

            if (auction.getState() == AuctionState.RUNNING){
                auctionService.closeSession(sessionId);
                auction = auctionService.getById(sessionId);
            }
            populate(auction);
        }catch (Exception e){
            showError("Không xử lý được phiên đấu giá: "+e.getMessage());
        }
    }

    private void populate(Auction auction){
        Item item = null;
        try {
            item = itemService.getById(auction.getItem_id());
        } catch (Exception ignored) {
        }

        if (lblItemName != null){
            lblItemName.setText(item == null ? "(?)" : item.getFullname());
        }
        if (lblWinningPrice != null){
            lblWinningPrice.setText(String.valueOf(auction.getCurrent_price()));
        }
        if (auction.getCurrent_user_id() >0){
            // co winner
            if (lblTitle != null) {
                lblTitle.setText("Chúc mừng người thắng cuộc");
            }
            if (lblWinnerId != null){
                lblWinnerId.setText(String.valueOf(auction.getCurrent_user_id()));
            }
            if (lblNote != null){
                lblNote.setText("Tiền thắng đã được trừ từ số dư người thắng, Item được đánh dấu SOLD");
            }
        }else{
            //Không có bidder
            if(lblTitle != null){
                lblTitle.setText("Phiên đấu giá kết thúc, không có người tham gia");
            }
            if(lblWinnerId != null){
                lblWinnerId.setText("(Không có)");
            }
            if(lblNote != null){
                lblNote.setText("Không có Bidder, Item trở lại trạng thái APPROVED, phiên đấu giá CANCLED");
            }
        }
    }
    // khi nguười dùng bấm nút quay lại
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

    private void switchScene(ActionEvent actionEvent, String fxmlPath) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        Scene currentScene = stage.getScene();
        if (currentScene == null) {
            stage.setScene(new Scene(root));
        } else {
            currentScene.setRoot(root);
        }
        stage.show();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
