package com.arion;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/Fxml/LoginView.fxml"));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Mi App JavaFX");
        stage.setMaximized(true); // Establece la ventana en pantalla completa
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
