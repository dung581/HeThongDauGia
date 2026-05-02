package Client;

import javafx.event.ActionEvent;

import java.io.IOException;

public class UpLoatItemController {



    //nut cua admin
    public void ArtAdmin(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/ART-A.fxml");
    }
    public void ElectronicsAdmin(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/Electronics-A.fxml");
    }
    public void VehicleAdmin(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/Vehicle-A.fxml");
    }
    public void trolaiAdmin(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/dashbroad - Admin.fxml");
    }

    //nut cua seller
    public void ArtSeller(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/ART-S.fxml");
    }
    public void ElectronicsSeller(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/Electronics-S.fxml");
    }
    public void VehicleSeller(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/Vehicle-S.fxml");
    }
    public void trolaiSeller(ActionEvent actionEvent) throws IOException {
        switchScene(actionEvent, "/com/template/hellfx/dashbroad - Seller.fxml");
    }


    public void Submit(){

    }

    private void switchScene(ActionEvent actionEvent, String s) {
    }
}
