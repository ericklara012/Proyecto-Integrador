package com.arion.Controller;

import com.arion.Model.Budget;
import com.arion.Model.Transaction;
import com.arion.Config.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.net.URL;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

public class BudgetFormController implements Initializable {

    @FXML private Label titleLabel;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<YearMonth> monthYearComboBox;
    @FXML private TextField limitAmountField;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private Budget budgetToEdit;
    private Runnable onSaveCallback;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configurar el botón de cancelar
        cancelButton.setOnAction(event -> {
            ((Stage) cancelButton.getScene().getWindow()).close();
        });

        // Configurar el botón de guardar
        saveButton.setOnAction(event -> saveButtonAction());

        // Configurar el selector de mes/año
        setupMonthYearSelector();

        // Cargar categorías para el ComboBox
        loadCategories();

        // Validación para que solo se acepten números en el campo de límite
        limitAmountField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d{0,2})?")) {
                limitAmountField.setText(oldValue);
            }
        });
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    public void setBudgetToEdit(Budget budget) {
        this.budgetToEdit = budget;
        titleLabel.setText("Editar Presupuesto");

        // Rellenar el formulario con los datos del presupuesto
        categoryComboBox.setValue(budget.getCategory());
        monthYearComboBox.setValue(budget.getPeriodYearMonth());
        limitAmountField.setText(String.valueOf(budget.getLimitAmount()));
    }

    public void setupForNewBudget(YearMonth defaultMonth) {
        titleLabel.setText("Nuevo Presupuesto");
        monthYearComboBox.setValue(defaultMonth);
    }

    private void setupMonthYearSelector() {
        // Llenar con 12 meses, desde el actual hacia adelante
        List<YearMonth> months = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = 0; i < 12; i++) {
            months.add(current.plusMonths(i));
        }

        monthYearComboBox.setItems(FXCollections.observableArrayList(months));

        monthYearComboBox.setConverter(new StringConverter<YearMonth>() {
            @Override
            public String toString(YearMonth yearMonth) {
                if (yearMonth != null) {
                    return yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
                }
                return "";
            }

            @Override
            public YearMonth fromString(String string) {
                return null; // No necesario para este caso
            }
        });
    }

    private void loadCategories() {
        Set<String> categories = new HashSet<>();

        // Obtener categorías de transacciones existentes
        try {
            List<Transaction> transactions = Transaction.getAll(SessionManager.getInstance().getCurrentUserId());
            for (Transaction transaction : transactions) {
                if (transaction.getCategory() != null && !transaction.getCategory().isEmpty()) {
                    categories.add(transaction.getCategory());
                }
            }

            // Agregar categorías predefinidas si es necesario
            categories.add("Alimentación");
            categories.add("Transporte");
            categories.add("Vivienda");
            categories.add("Entretenimiento");
            categories.add("Salud");
            categories.add("Educación");
            categories.add("Otros");

            categoryComboBox.setItems(FXCollections.observableArrayList(categories));
        } catch (Exception e) {
            // Error al cargar categorías
        }
    }

    private void saveButtonAction() {
        // Validar los datos
        if (!validateForm()) {
            return;
        }

        try {
            String category = categoryComboBox.getValue();
            YearMonth yearMonth = monthYearComboBox.getValue();
            double limitAmount = Double.parseDouble(limitAmountField.getText());

            // Actualizar o crear presupuesto según corresponda
            if (budgetToEdit == null) {
                // Nuevo presupuesto
                Budget newBudget = new Budget(category, limitAmount, yearMonth);

                if (newBudget.save(SessionManager.getInstance().getCurrentUserId())) {
                    showAlert(Alert.AlertType.INFORMATION, "Éxito", "Presupuesto guardado",
                             "El presupuesto se ha creado correctamente.");

                    if (onSaveCallback != null) {
                        onSaveCallback.run();
                    }

                    ((Stage) saveButton.getScene().getWindow()).close();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "No se pudo guardar",
                             "Ocurrió un error al guardar el presupuesto.");
                }
            } else {
                // Actualizar presupuesto existente
                budgetToEdit.setCategory(category);
                budgetToEdit.setPeriodYearMonth(yearMonth);
                budgetToEdit.setLimitAmount(limitAmount);

                if (budgetToEdit.update()) {
                    showAlert(Alert.AlertType.INFORMATION, "Éxito", "Presupuesto actualizado",
                             "El presupuesto se ha actualizado correctamente.");

                    if (onSaveCallback != null) {
                        onSaveCallback.run();
                    }

                    ((Stage) saveButton.getScene().getWindow()).close();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "No se pudo actualizar",
                             "Ocurrió un error al actualizar el presupuesto.");
                }
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Error inesperado", e.getMessage());
        }
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (categoryComboBox.getValue() == null || categoryComboBox.getValue().trim().isEmpty()) {
            errors.append("- Debes seleccionar o ingresar una categoría.\n");
        }

        if (monthYearComboBox.getValue() == null) {
            errors.append("- Debes seleccionar un mes.\n");
        }

        if (limitAmountField.getText().trim().isEmpty()) {
            errors.append("- El campo de límite no puede estar vacío.\n");
        } else {
            try {
                double amount = Double.parseDouble(limitAmountField.getText());
                if (amount <= 0) {
                    errors.append("- El límite debe ser mayor que cero.\n");
                }
            } catch (NumberFormatException e) {
                errors.append("- El límite debe ser un número válido.\n");
            }
        }

        if (errors.length() > 0) {
            showAlert(Alert.AlertType.ERROR, "Errores en el formulario",
                     "Por favor corrige los siguientes errores:", errors.toString());
            return false;
        }

        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
