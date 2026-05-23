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
import java.text.NumberFormat;
import java.util.Locale;

public class WinnerController{
    @FXML private Label lblTitle;
    @FXML private Label lblSessionId;
    @FXML private Label lblSessionState;
    @FXML private Label lblItemName;
    @FXML private Label lblItemId;
    @FXML private Label lblWinningPrice;
    @FXML private Label lblWinnerId;
    @FXML private Label lblNote;

    private final AuctionService auctionService = new AuctionService();
    private final ItemService itemService = new ItemService();
    private static long pendingSessionId = 0L;

    // Nhận sessionId từ màn session-detail trước khi load màn Winner.fxml.
    public static void setSessionId(long sessionId) {
        pendingSessionId = sessionId;
    }

    // JavaFX tự gọi sau khi load FXML: lấy phiên đấu giá, đóng phiên nếu còn RUNNING,
    // rồi đổ dữ liệu kết quả lên giao diện.
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

    // Đổ thông tin phiên đấu giá, vật phẩm, giá thắng và người thắng lên các label.
    private void populate(Auction auction){
        Item item = null;
        try {
            item = itemService.getById(auction.getItem_id());
        } catch (Exception ignored) {
        }

        setText(lblSessionId, String.valueOf(auction.getId()));
        setText(lblSessionState, auction.getState() == null ? "UNKNOWN" : auction.getState().name());
        setText(lblItemId, String.valueOf(auction.getItem_id()));

        if (lblItemName != null){
            lblItemName.setText(item == null ? "(?)" : item.getFullname());
        }
        if (lblWinningPrice != null){
            lblWinningPrice.setText(formatMoney(auction.getCurrent_price()));
        }
        if (auction.getCurrent_user_id() >0){
            if (lblTitle != null) {
                lblTitle.setText("Kết quả phiên đấu giá");
            }
            setText(lblWinnerId, formatWinner(auction));
            setText(lblNote, "Phiên đã kết thúc. Vật phẩm được bán cho người thắng ở mức giá cuối cùng.");
        }else{
            if(lblTitle != null){
                lblTitle.setText("Phiên đấu giá kết thúc, không có người tham gia");
            }
            setText(lblWinnerId, "Không có");
            setText(lblNote, "Không có bidder đặt giá. Vật phẩm được trả về trạng thái chờ xử lý tiếp.");
        }
    }

    // Xử lý nút quay lại: đưa user về dashboard đúng với role hiện tại.
    @FXML
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

    // Thay root scene hiện tại bằng màn FXML mới và giữ nguyên cửa sổ ứng dụng.
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

    // Hiện hộp thoại lỗi khi không tải được phiên hoặc không xử lý được kết quả.
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Lấy thông tin người thắng qua AuctionService; nếu không lấy được tên thì fallback sang ID.
    private String formatWinner(Auction auction) {
        UserAccount winner = null;
        try {
            winner = auctionService.declareWinner(auction.getId());
        } catch (Exception ignored) {
        }

        String winnerName = winner == null || winner.getFullname() == null || winner.getFullname().isBlank()
                ? "User #" + auction.getCurrent_user_id()
                : winner.getFullname();
        return winnerName + " (ID " + auction.getCurrent_user_id() + ")";
    }

    // Định dạng số tiền có dấu phẩy ngăn cách hàng nghìn để hiển thị trên UI.
    private String formatMoney(long amount) {
        return NumberFormat.getNumberInstance(Locale.US).format(amount);
    }

    // Set text an toàn cho Label, tránh lỗi khi FXML không bind một label nào đó.
    private void setText(Label label, String value) {
        if (label != null) {
            label.setText(value == null ? "" : value);
        }
    }
}
