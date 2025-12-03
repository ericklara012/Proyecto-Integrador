package com.arion.Test;

import com.arion.Utils.AlertUtils;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AlertTestApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Button successBtn = new Button("Probar Alerta de Éxito");
        successBtn.setOnAction(e -> AlertUtils.showSuccessAlert("Éxito", "Esta es una alerta de éxito"));

        Button errorBtn = new Button("Probar Alerta de Error");
        errorBtn.setOnAction(e -> AlertUtils.showErrorAlert("Error", "Esta es una alerta de error"));

        Button warningBtn = new Button("Probar Alerta de Advertencia");
        warningBtn.setOnAction(e -> AlertUtils.showWarningAlert("Advertencia", "Esta es una alerta de advertencia"));

        Button confirmBtn = new Button("Probar Alerta de Confirmación");
        confirmBtn.setOnAction(e -> {
            boolean result = AlertUtils.showConfirmationAlert("Confirmación", "¿Deseas continuar?");
            // Resultado: result es true si se confirmó, false si se canceló
        });

        root.getChildren().addAll(successBtn, errorBtn, warningBtn, confirmBtn);

        Scene scene = new Scene(root, 300, 250);
        primaryStage.setTitle("Test de Alertas");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}