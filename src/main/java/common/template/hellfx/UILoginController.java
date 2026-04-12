package com.template.hellofx;



import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.io.IOException;

public class UILoginController {
    private Parent root;
    private Scene scene;
    private Stage stage;
    private ActionEvent actionEvent;
    @FXML
    private TextField name;
    @FXML
    private TextField password;
    @FXML
    private Label welcomeText;



    public void login(){
        String ten = name.getText();
        String pass= password.getText();
        welcomeText.setText("chao mung " + ten + " den voi he thong dau gia");
    }
    public void Register(ActionEvent actionEvent) throws IOException {
        System.out.println("123");
        root = FXMLLoader.load(getClass().getResource("/com/template/hellofx/Scene2.fxml"));
        stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
    public void dangki(ActionEvent actionEvent) throws IOException {
        System.out.println("456");
        root = FXMLLoader.load(getClass().getResource("/com/template/hellofx/Main.fxml"));
        stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}
