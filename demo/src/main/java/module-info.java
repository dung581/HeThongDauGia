module org.ducanh.demo {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.ducanh.demo to javafx.fxml;
    exports org.ducanh.demo;
}