package com.arion.Controller;

import com.arion.Model.Budget;
import com.arion.Model.Transaction;
import com.arion.Config.SessionManager;
import com.arion.Utils.AlertUtils;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.geometry.Pos;

import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class BudgetViewController implements Initializable {

    @FXML private TableView<Budget> budgetsTable;
    @FXML private TableColumn<Budget, String> categoryCol;
    @FXML private TableColumn<Budget, Double> limitAmountCol;
    @FXML private TableColumn<Budget, Double> spentAmountCol;
    @FXML private TableColumn<Budget, Double> remainingCol;
    @FXML private TableColumn<Budget, ProgressBar> progressCol;
    @FXML private TableColumn<Budget, Budget> actionsCol;
    @FXML private Button addNewBudgetButton;
    @FXML private ListView<String> alertsListView;
    @FXML private Label totalBudgetedLabel;
    @FXML private Label totalSpentLabel;
    @FXML private Label activeBudgetsLabel;

    private ObservableList<Budget> budgetsList = FXCollections.observableArrayList();
    private DecimalFormat currencyFormat = new DecimalFormat("$#,##0.00");
    private YearMonth currentYearMonth = YearMonth.now();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configurar tabla de presupuestos
        setupTable();

        // Configurar botones
        setupButtons();

        // Cargar datos
        loadBudgets();

        // Mostrar alertas de presupuestos excedidos
        loadBudgetAlerts();

        // Actualizar resumen
        updateSummaryLabels();
    }

    private void setupTable() {
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));

        limitAmountCol.setCellValueFactory(cellData -> cellData.getValue().limitAmountProperty().asObject());
        limitAmountCol.setCellFactory(col -> new TableCell<Budget, Double>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(amount));
                }
            }
        });

        spentAmountCol.setCellValueFactory(cellData -> {
            Budget budget = cellData.getValue();
            double spent = Budget.getSpentAmountForCategoryInMonth(
                SessionManager.getInstance().getCurrentUserId(),
                budget.getCategory(),
                budget.getPeriodYearMonth()
            );
            return new ReadOnlyObjectWrapper<>(spent);
        });
        spentAmountCol.setCellFactory(col -> new TableCell<Budget, Double>() {
            @Override
            protected void updateItem(Double spent, boolean empty) {
                super.updateItem(spent, empty);
                if (empty || spent == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(spent));
                }
            }
        });

        remainingCol.setCellValueFactory(cellData -> {
            Budget budget = cellData.getValue();
            double spent = Budget.getSpentAmountForCategoryInMonth(
                SessionManager.getInstance().getCurrentUserId(),
                budget.getCategory(),
                budget.getPeriodYearMonth()
            );
            double remaining = budget.getLimitAmount() - spent;
            return new ReadOnlyObjectWrapper<>(remaining);
        });
        remainingCol.setCellFactory(col -> new TableCell<Budget, Double>() {
            @Override
            protected void updateItem(Double remaining, boolean empty) {
                super.updateItem(remaining, empty);
                if (empty || remaining == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(remaining));
                    if (remaining < 0) {
                        setStyle("-fx-text-fill: red;");
                    } else {
                        setStyle("-fx-text-fill: green;");
                    }
                }
            }
        });

        progressCol.setCellValueFactory(cellData -> {
            Budget budget = cellData.getValue();
            double spent = Budget.getSpentAmountForCategoryInMonth(
                SessionManager.getInstance().getCurrentUserId(),
                budget.getCategory(),
                budget.getPeriodYearMonth()
            );
            double percentage = spent / budget.getLimitAmount();

            ProgressBar progressBar = new ProgressBar(percentage);
            progressBar.setPrefWidth(140);

            if (percentage >= 1.0) {
                progressBar.setStyle("-fx-accent: red;");
            } else if (percentage >= 0.8) {
                progressBar.setStyle("-fx-accent: orange;");
            } else {
                progressBar.setStyle("-fx-accent: green;");
            }

            return new ReadOnlyObjectWrapper<>(progressBar);
        });
        progressCol.setCellFactory(col -> new TableCell<Budget, ProgressBar>() {
            @Override
            protected void updateItem(ProgressBar progressBar, boolean empty) {
                super.updateItem(progressBar, empty);
                if (empty || progressBar == null) {
                    setGraphic(null);
                } else {
                    setGraphic(progressBar);
                }
            }
        });

        actionsCol.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        actionsCol.setCellFactory(col -> new TableCell<Budget, Budget>() {
            private final Button editBtn = createIconButton("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z", "button-icon-edit");
            private final Button deleteBtn = createIconButton("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z", "button-icon-delete");
            private final HBox pane = new HBox(10, editBtn, deleteBtn);

            {
                pane.setAlignment(Pos.CENTER);

                editBtn.setOnAction(event -> {
                    Budget budget = getTableView().getItems().get(getIndex());
                    showBudgetForm(budget);
                });

                deleteBtn.setOnAction(event -> {
                    Budget budget = getTableView().getItems().get(getIndex());
                    confirmAndDeleteBudget(budget);
                });
            }

            @Override
            protected void updateItem(Budget budget, boolean empty) {
                super.updateItem(budget, empty);
                if (empty || budget == null) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });

        budgetsTable.setItems(budgetsList);
    }

    private void setupButtons() {
        addNewBudgetButton.setOnAction(event -> showBudgetForm(null));
    }

    private void loadBudgets() {
        budgetsList.clear();
        int userId = SessionManager.getInstance().getCurrentUserId();

        // Obtenemos todos los presupuestos activos
        for (Budget budget : Budget.getAllActive(userId)) {
            budgetsList.add(budget);
        }
    }

    private void loadBudgetAlerts() {
        alertsListView.getItems().clear();
        List<Budget> exceededBudgets = Budget.getExceededBudgets(SessionManager.getInstance().getCurrentUserId());

        if (exceededBudgets.isEmpty()) {
            alertsListView.getItems().add("No hay presupuestos excedidos.");
        } else {
            for (Budget budget : exceededBudgets) {
                double spent = Budget.getSpentAmountForCategoryInMonth(
                    SessionManager.getInstance().getCurrentUserId(),
                    budget.getCategory(),
                    budget.getPeriodYearMonth()
                );
                double exceeded = spent - budget.getLimitAmount();
                String alert = String.format(
                    "¡ALERTA! Has excedido tu presupuesto en %s por %s (%.1f%% del límite)",
                    budget.getCategory(),
                    currencyFormat.format(exceeded),
                    (spent / budget.getLimitAmount() * 100)
                );
                alertsListView.getItems().add(alert);
            }
        }
    }

    private void showBudgetForm(Budget budget) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/BudgetFormView.fxml"));
            Parent root = loader.load();

            BudgetFormController controller = loader.getController();
            controller.setOnSaveCallback(() -> {
                loadBudgets();
                loadBudgetAlerts();
                updateSummaryLabels();
            });

            if (budget != null) {
                controller.setBudgetToEdit(budget);
            } else {
                controller.setupForNewBudget(currentYearMonth);
            }

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(budget == null ? "Nuevo Presupuesto" : "Editar Presupuesto");
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            AlertUtils.showErrorAlert("Error", "Error al abrir el formulario de presupuesto: " + e.getMessage());
        }
    }

    private void confirmAndDeleteBudget(Budget budget) {
        if (AlertUtils.showConfirmationAlert("Confirmar eliminación",
            "¿Estás seguro que deseas eliminar este presupuesto?\nEsta acción no se puede deshacer.")) {
            if (budget.delete()) {
                loadBudgets();
                loadBudgetAlerts();
                updateSummaryLabels();
                AlertUtils.showSuccessAlert("Éxito", "Presupuesto eliminado correctamente");
            } else {
                AlertUtils.showErrorAlert("Error", "No se pudo eliminar el presupuesto. Ocurrió un error al intentar eliminar el presupuesto.");
            }
        }
    }

    private Button createIconButton(String iconPath, String styleClass) {
        Button button = new Button();
        SVGPath icon = new SVGPath();
        icon.setContent(iconPath);
        icon.getStyleClass().add("icon");
        button.setGraphic(icon);
        button.getStyleClass().addAll("button-icon", styleClass);

        // Agregar tooltip
        if (styleClass.contains("edit")) {
            Tooltip tooltip = new Tooltip("Editar presupuesto");
            button.setTooltip(tooltip);
        } else if (styleClass.contains("delete")) {
            Tooltip tooltip = new Tooltip("Eliminar presupuesto");
            button.setTooltip(tooltip);
        }

        return button;
    }

    private void updateSummaryLabels() {
        double totalBudgeted = 0;
        double totalSpent = 0;
        int activeBudgets = budgetsList.size();

        for (Budget budget : budgetsList) {
            totalBudgeted += budget.getLimitAmount();
            totalSpent += Budget.getSpentAmountForCategoryInMonth(
                SessionManager.getInstance().getCurrentUserId(),
                budget.getCategory(),
                budget.getPeriodYearMonth()
            );
        }

        totalBudgetedLabel.setText(currencyFormat.format(totalBudgeted));
        totalSpentLabel.setText(currencyFormat.format(totalSpent));
        activeBudgetsLabel.setText(String.valueOf(activeBudgets));
    }
}
