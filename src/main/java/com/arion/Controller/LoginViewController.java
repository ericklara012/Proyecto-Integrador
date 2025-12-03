package com.arion.Controller;

import com.arion.Model.User;
import com.arion.Config.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;

import java.io.IOException;

public class LoginViewController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField visiblePasswordField;

    @FXML
    private SVGPath eyeIcon;

    // Contenido SVG para el ojo abierto (mostrar contraseña)
    private final String EYE_OPEN = "M12,4.5C7,4.5,2.73,7.61,1,12c1.73,4.39,6,7.5,11,7.5s9.27-3.11,11-7.5C21.27,7.61,17,4.5,12,4.5z "
            + "M12,17c-2.76,0-5-2.24-5-5s2.24-5,5-5s5,2.24,5,5S14.76,17,12,17z "
            + "M12,9c-1.66,0-3,1.34-3,3s1.34,3,3,3s3-1.34,3-3S13.66,9,12,9z";

    // Contenido SVG para el ojo cerrado (ocultar contraseña)
    private final String EYE_CLOSED = "M12,7c2.76,0,5,2.24,5,5c0,0.65-0.13,1.26-0.36,1.83l2.92,2.92c1.51-1.26,2.7-2.89,3.43-4.75 "
            + "c-1.73-4.39-6-7.5-11-7.5c-1.4,0-2.74,0.25-3.98,0.7l2.16,2.16C10.74,7.13,11.35,7,12,7z "
            + "M2,4.27l2.28,2.28l0.46,0.46C3.08,8.3,1.78,10.02,1,12c1.73,4.39,6,7.5,11,7.5c1.55,0,3.03-0.3,4.38-0.84l0.42,0.42 "
            + "L19.73,22L21,20.73L3.27,3L2,4.27z M7.53,9.8l1.55,1.55c-0.05,0.21-0.08,0.43-0.08,0.65c0,1.66,1.34,3,3,3 "
            + "c0.22,0,0.44-0.03,0.65-0.08l1.55,1.55c-0.67,0.33-1.41,0.53-2.2,0.53c-2.76,0-5-2.24-5-5C7,11.21,7.2,10.47,7.53,9.8z "
            + "M11.84,9.02l3.15,3.15l0.02-0.16c0-1.66-1.34-3-3-3L11.84,9.02z";

    @FXML
    private void initialize() {
        // Sincroniza los campos de contraseña
        if (passwordField != null && visiblePasswordField != null) {
            passwordField.textProperty().addListener((observable, oldValue, newValue) ->
                visiblePasswordField.setText(newValue));

            visiblePasswordField.textProperty().addListener((observable, oldValue, newValue) ->
                passwordField.setText(newValue));
        }
    }

    @FXML
    private void togglePasswordVisibility(MouseEvent event) {
        if (passwordField.isVisible()) {
            visiblePasswordField.setText(passwordField.getText());
            passwordField.setVisible(false);
            visiblePasswordField.setVisible(true);
            eyeIcon.setContent(EYE_CLOSED);
            visiblePasswordField.requestFocus();
            visiblePasswordField.positionCaret(visiblePasswordField.getText().length());
        } else {
            passwordField.setText(visiblePasswordField.getText());
            passwordField.setVisible(true);
            visiblePasswordField.setVisible(false);
            eyeIcon.setContent(EYE_OPEN);
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
        }
    }

    @FXML
    private void Session(ActionEvent event) {
        String username = emailField.getText();
        String password = passwordField.getText();

        // Validar que los campos no estén vacíos
        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Por favor ingrese usuario y contraseña");
            return;
        }

        // Autenticar usuario
        User user = User.authenticate(username, password);

        if (user != null) {
            // Guardar usuario en sesión
            SessionManager.getInstance().setCurrentUser(user);

            // Navegar al dashboard
            navigateToDashboard(event);
        } else {
            showAlert("Error de autenticación", "Usuario o contraseña incorrectos");
        }
    }

    private void navigateToDashboard(ActionEvent event) {
        try {
            // Carga el nuevo FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/DashboardView.fxml"));
            Parent nuevaEscena = loader.load();

            // Obtiene la ventana actual
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // Obtiene el tamaño de la pantalla
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

            // Crea la nueva escena con tamaño igual al de la pantalla
            Scene scene = new Scene(nuevaEscena, screenBounds.getWidth(), screenBounds.getHeight());

            // Configura la posición y tamaño antes de mostrar
            stage.setX(screenBounds.getMinX());
            stage.setY(screenBounds.getMinY());
            stage.setWidth(screenBounds.getWidth());
            stage.setHeight(screenBounds.getHeight());

            // Establece la escena
            stage.setScene(scene);

            // Configura maximizado sin animación
            stage.setMaximized(true);

            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void RegisterView(ActionEvent event) {
        try {
            // Carga el nuevo FXML con un nuevo loader cada vez
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/RegisterView.fxml"));
            Parent nuevaEscena = loader.load();

            // Obtiene la ventana actual
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // Obtiene el tamaño de la pantalla
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

            // Crea la nueva escena con tamaño igual al de la pantalla
            Scene scene = new Scene(nuevaEscena, screenBounds.getWidth(), screenBounds.getHeight());

            // Configura la posición y tamaño antes de mostrar
            stage.setX(screenBounds.getMinX());
            stage.setY(screenBounds.getMinY());
            stage.setWidth(screenBounds.getWidth());
            stage.setHeight(screenBounds.getHeight());

            // Establece la escena
            stage.setScene(scene);

            // Configura maximizado sin animación
            stage.setMaximized(true);

            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
