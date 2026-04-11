package com.template.hellfx;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class UILoginController {
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
    public void Register(){
        System.out.println("tao tai khoan moi");
    }

}
