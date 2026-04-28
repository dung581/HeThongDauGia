package Client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class danhSachDauGiaController {
    public void trolai(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/UILogin.fxml");
    }

    private void switchScene(ActionEvent actionEvent, String s) {
    }

    public class SessionListController {

        @FXML
        private VBox sessionsContainer;

        // Hàm này tự chạy khi load UI
        @FXML
        public void initialize() {
            addOneItem(); // test thêm 1 item
        }


        // 👉 Hàm tạo 1 item
        private HBox createItem(String name, String price) {
            Label nameLabel = new Label(name);
            nameLabel.setStyle("-fx-font-weight: bold;");

            Label priceLabel = new Label(price);

            Button viewBtn = new Button("Xem");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox item = new HBox(10, nameLabel, spacer, priceLabel, viewBtn);
            item.setStyle("-fx-background-color: #eaf2f8; -fx-padding: 10; -fx-background-radius: 8;");

            return item;
        }

        // 👉 Hàm thêm 1 item vào VBox
        public void addOneItem() {
            HBox item = createItem("Session #101", "1.200.000đ");
            sessionsContainer.getChildren().add(item);
        }
    }
}
