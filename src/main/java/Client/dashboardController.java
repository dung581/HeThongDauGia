package Client;

import javafx.event.ActionEvent;

import java.io.IOException;

public class dashboardController {

    public void goToLogin(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/UILogin.fxml");
    }
    public void Sandaugia(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/danhSachDauGia.fxml");
    }

    public void dangban (ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/uploadItem.fxml");
    }
    public void quanlytk (ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/danhSachDauGia.fxml");
    }

    private void switchScene(ActionEvent actionEvent, String s) {
    }



}
