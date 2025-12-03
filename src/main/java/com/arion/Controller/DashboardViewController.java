package com.arion.Controller;

import com.arion.Model.Transaction;
import com.arion.Config.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.stage.FileChooser;
import java.io.File;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import javafx.collections.transformation.FilteredList;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.ResourceBundle;

public class DashboardViewController implements Initializable {

    @FXML private PieChart expensesPieChart;
    @FXML private ListView<Transaction> transactionsListView;
    @FXML private Label totalIncomeLabel;
    @FXML private Label totalExpensesLabel;
    @FXML private Label netBalanceLabel;
    @FXML private Label usernameLabel;
    @FXML private Button budgetsButton;
    @FXML private ComboBox<String> categoryFilterComboBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button downloadTransactionsButton;
    @FXML private Button clearFiltersButton;

    private ObservableList<Transaction> transactions;
    private FilteredList<Transaction> filteredTransactions;
    private DecimalFormat currencyFormat = new DecimalFormat("$#,##0.00");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadUserData();
        setupPieChart();
        setupTransactionList();
        setupFilters();
        updateSummaryLabels();

        // Configurar el botón de presupuestos
        budgetsButton.setOnAction(event -> openBudgetManager());
    }

    private void loadUserData() {
        // Mostrar el nombre del usuario actual
        if (usernameLabel != null && SessionManager.getInstance().isLoggedIn()) {
            usernameLabel.setText("Bienvenido, " + SessionManager.getInstance().getCurrentUsername());
        }

        // Cargar transacciones del usuario actual
        int currentUserId = SessionManager.getInstance().getCurrentUserId();
        if (currentUserId > 0) {
            List<Transaction> userTransactions = Transaction.getRecentTransactionsByUser(currentUserId, 10);
            transactions = FXCollections.observableArrayList(userTransactions);
        } else {
            transactions = FXCollections.observableArrayList();
        }
    }

    private void setupPieChart() {
        int currentUserId = SessionManager.getInstance().getCurrentUserId();
        if (currentUserId <= 0) {
            expensesPieChart.setData(FXCollections.observableArrayList());
            return;
        }

        // Obtener todas las transacciones del usuario para el gráfico
        List<Transaction> allTransactions = Transaction.getTransactionsByUser(currentUserId);

        // Filtrar solo gastos y agrupar por categoría
        Map<String, Double> expensesByCategory = allTransactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                    Transaction::getCategory,
                    Collectors.summingDouble(Transaction::getAmount)
                ));

        // Calcular el total para calcular porcentajes
        double totalExpenses = expensesByCategory.values().stream().mapToDouble(Double::doubleValue).sum();

        // Crear datos para el gráfico de pastel
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        if (expensesByCategory.isEmpty()) {
            pieChartData.add(new PieChart.Data("Sin gastos", 1));
        } else {
            expensesByCategory.forEach((category, amount) -> {
                double percentage = totalExpenses > 0 ? (amount / totalExpenses) * 100 : 0;
                String label = String.format("%s\n%s (%.1f%%)", category, currencyFormat.format(amount), percentage);
                PieChart.Data data = new PieChart.Data(label, amount);
                pieChartData.add(data);
            });
        }
        
        expensesPieChart.setData(pieChartData);
        expensesPieChart.setTitle(null);
        expensesPieChart.setMinSize(PieChart.USE_PREF_SIZE, PieChart.USE_PREF_SIZE);
        expensesPieChart.setPrefSize(500, 400);
        expensesPieChart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        expensesPieChart.setLabelsVisible(true);
        
        // Agregar tooltips después de que el gráfico se renderice
        // Usar Platform.runLater para asegurar que los nodos estén creados
        javafx.application.Platform.runLater(() -> {
            expensesPieChart.getData().forEach(data -> {
                if (data.getNode() != null && !data.getName().equals("Sin gastos")) {
                    String[] parts = data.getName().split("\n");
                    if (parts.length >= 2) {
                        String category = parts[0];
                        String amountAndPercent = parts[1];
                        Tooltip tooltip = new Tooltip(String.format("Categoría: %s\n%s", category, amountAndPercent));
                        Tooltip.install(data.getNode(), tooltip);
                    }
                }
            });
        });
    }

    private void setupTransactionList() {
        // Configurar doble clic para editar transacciones
        transactionsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && transactionsListView.getSelectionModel().getSelectedItem() != null) {
                Transaction selectedTransaction = transactionsListView.getSelectionModel().getSelectedItem();
                openEditTransactionForm(selectedTransaction);
            }
        });

        // Celda personalizada para mostrar cada transacción
        transactionsListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Transaction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(10);
                    hbox.setAlignment(Pos.CENTER_LEFT);

                    VBox descriptionBox = new VBox();
                    Label categoryLabel = new Label(item.getCategory());
                    categoryLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

                    Label dateLabel = new Label(item.getDateString());
                    dateLabel.setFont(Font.font("Arial", 12));
                    dateLabel.setStyle("-fx-text-fill: #666666;");

                    descriptionBox.getChildren().addAll(categoryLabel, dateLabel);

                    Label amountLabel = new Label(currencyFormat.format(item.getAmount()));
                    amountLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

                    // Color basado en el tipo de transacción
                    if (item.getType() == Transaction.TransactionType.INCOME) {
                        amountLabel.setStyle("-fx-text-fill: #4CAF50;"); // Verde para ingresos
                    } else {
                        amountLabel.setStyle("-fx-text-fill: #F44336;"); // Rojo para gastos
                    }

                    hbox.getChildren().addAll(descriptionBox, amountLabel);
                    HBox.setHgrow(descriptionBox, javafx.scene.layout.Priority.ALWAYS);

                    setGraphic(hbox);
                }
            }
        });
    }
    
    private void setupFilters() {
        // Crear lista filtrada
        filteredTransactions = new FilteredList<>(transactions, p -> true);
        transactionsListView.setItems(filteredTransactions);
        
        // Cargar categorías para el filtro
        Set<String> categories = new HashSet<>();
        for (Transaction t : transactions) {
            if (t.getCategory() != null && !t.getCategory().isEmpty()) {
                categories.add(t.getCategory());
            }
        }
        categoryFilterComboBox.getItems().addAll("Todas las categorías");
        categoryFilterComboBox.getItems().addAll(categories.stream().sorted().collect(Collectors.toList()));
        categoryFilterComboBox.setValue("Todas las categorías");
        
        // Configurar filtros
        categoryFilterComboBox.setOnAction(e -> applyFilters());
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }
    
    private void applyFilters() {
        filteredTransactions.setPredicate(transaction -> {
            // Filtro por categoría
            String selectedCategory = categoryFilterComboBox.getValue();
            if (selectedCategory != null && !selectedCategory.equals("Todas las categorías")) {
                if (!selectedCategory.equals(transaction.getCategory())) {
                    return false;
                }
            }
            
            // Filtro por fecha
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            
            if (startDate != null && transaction.getDate().isBefore(startDate)) {
                return false;
            }
            
            if (endDate != null && transaction.getDate().isAfter(endDate)) {
                return false;
            }
            
            return true;
        });
    }
    
    @FXML
    private void clearFilters() {
        categoryFilterComboBox.setValue("Todas las categorías");
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        applyFilters();
    }
    
    @FXML
    private void downloadTransactions() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Guardar Reporte de Transacciones");
            fileChooser.setInitialFileName("transacciones_filtradas");
            
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Archivos Excel", "*.xlsx")
            );
            
            Stage stage = (Stage) transactionsListView.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);
            
            if (file != null) {
                String fileName = file.getName().toLowerCase();
                List<Transaction> transactionsToExport = new ArrayList<>(filteredTransactions);
                
                if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
                    generateExcel(file, transactionsToExport);
                    com.arion.Utils.AlertUtils.showSuccessAlert("Éxito", "Reporte Excel generado exitosamente");
                } else {
                    if (!fileName.endsWith(".pdf")) {
                        file = new File(file.getAbsolutePath() + ".pdf");
                    }
                    generatePDF(file, transactionsToExport);
                    com.arion.Utils.AlertUtils.showSuccessAlert("Éxito", "Reporte PDF generado exitosamente");
                }
            }
        } catch (Exception e) {
            com.arion.Utils.AlertUtils.showErrorAlert("Error", "Error al generar el reporte: " + e.getMessage());
        }
    }
    
    private void generatePDF(File file, List<Transaction> transactionsToExport) throws Exception {
        int currentUserId = SessionManager.getInstance().getCurrentUserId();
        String username = SessionManager.getInstance().getCurrentUsername();
        
        LocalDate startDate = startDatePicker.getValue() != null ? startDatePicker.getValue() : LocalDate.now().minusMonths(12);
        LocalDate endDate = endDatePicker.getValue() != null ? endDatePicker.getValue() : LocalDate.now();
        
        com.arion.Model.Reporte reporte = com.arion.Model.Reporte.crearReporte(currentUserId, username, startDate, endDate);
        reporte.setMovimientos(transactionsToExport);
        reporte.initiarDatos(transactionsToExport);
        reporte.generarPDFDOCUMENTO(file.getAbsolutePath());
    }
    
    private void generateExcel(File file, List<Transaction> transactionsToExport) throws Exception {
        int currentUserId = SessionManager.getInstance().getCurrentUserId();
        String username = SessionManager.getInstance().getCurrentUsername();
        
        LocalDate startDate = startDatePicker.getValue() != null ? startDatePicker.getValue() : LocalDate.now().minusMonths(12);
        LocalDate endDate = endDatePicker.getValue() != null ? endDatePicker.getValue() : LocalDate.now();
        
        com.arion.Model.Reporte reporte = com.arion.Model.Reporte.crearReporte(currentUserId, username, startDate, endDate);
        reporte.setMovimientos(transactionsToExport);
        reporte.initiarDatos(transactionsToExport);
        reporte.generarExcelDOCUMENTO(file.getAbsolutePath());
    }

    private void updateSummaryLabels() {
        int currentUserId = SessionManager.getInstance().getCurrentUserId();
        if (currentUserId <= 0) {
            totalIncomeLabel.setText("$0.00");
            totalExpensesLabel.setText("$0.00");
            netBalanceLabel.setText("$0.00");
            return;
        }

        double totalIncome = Transaction.getTotalIncome(currentUserId);
        double totalExpenses = Transaction.getTotalExpenses(currentUserId);
        double netBalance = totalIncome - totalExpenses;

        totalIncomeLabel.setText(currencyFormat.format(totalIncome));
        totalExpensesLabel.setText(currencyFormat.format(totalExpenses));
        netBalanceLabel.setText(currencyFormat.format(netBalance));

        // Cambiar color del balance neto según si es positivo o negativo
        if (netBalance >= 0) {
            netBalanceLabel.setStyle("-fx-text-fill: #4CAF50;"); // Verde para positivo
        } else {
            netBalanceLabel.setStyle("-fx-text-fill: #F44336;"); // Rojo para negativo
        }
        
        // Agregar tooltips informativos
        Tooltip incomeTooltip = new Tooltip("Total de ingresos del mes actual");
        totalIncomeLabel.setTooltip(incomeTooltip);
        
        Tooltip expensesTooltip = new Tooltip("Total de gastos del mes actual");
        totalExpensesLabel.setTooltip(expensesTooltip);
        
        Tooltip balanceTooltip = new Tooltip("Saldo total del mes actual (Ingresos - Gastos)");
        netBalanceLabel.setTooltip(balanceTooltip);
    }

    // Método para refrescar los datos (útil cuando se agrega una nueva transacción)
    public void refreshData() {
        loadUserData();
        setupPieChart();
        transactionsListView.setItems(transactions);
        updateSummaryLabels();
    }

    @FXML
    private void openTransactionForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/TransactionFormView.fxml"));
            Parent root = loader.load();

            // Obtener el controlador del formulario
            TransactionFormController controller = loader.getController();

            // Configurar callback para refrescar datos cuando se guarde una transacción
            controller.setOnTransactionSaved(this::refreshData);

            Stage stage = new Stage();
            stage.setTitle("Nueva Transacción");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

        } catch (IOException e) {
            // Error al cargar formulario de transacción
        }
    }

    @FXML
    private void openReports() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/ReportsView.fxml"));
            Parent root = loader.load();

            // Obtener el controlador de reportes y pasarle una referencia de este dashboard
            ReportsViewController reportsController = loader.getController();
            reportsController.setDashboardRefreshCallback(this::refreshData);

            Stage stage = new Stage();
            stage.setTitle("Reportes");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 800, 600));
            stage.showAndWait();

        } catch (IOException e) {
            // Error al cargar reportes
        }
    }

    @FXML
    private void logout() {
        SessionManager.getInstance().logout();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/LoginView.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) totalIncomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            // Error al cerrar sesión
        }
    }

    @FXML
    private void addIncome() {
        openTransactionFormWithType(TransactionFormController.FormType.INCOME);
    }

    @FXML
    private void addExpense() {
        openTransactionFormWithType(TransactionFormController.FormType.EXPENSE);
    }

    @FXML
    private void onViewReportsClick() {
        openReports();
    }

    private void openTransactionFormWithType(TransactionFormController.FormType formType) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/TransactionFormView.fxml"));
            Parent root = loader.load();

            // Obtener el controlador del formulario
            TransactionFormController controller = loader.getController();

            // Configurar el tipo de formulario (Ingreso o Gasto)
            controller.configureFor(formType);

            // Configurar callback para refrescar datos cuando se guarde una transacción
            controller.setOnTransactionSaved(this::refreshData);

            Stage stage = new Stage();
            stage.setTitle(formType == TransactionFormController.FormType.INCOME ? "Agregar Ingreso" : "Agregar Gasto");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

        } catch (IOException e) {
            // Error al cargar formulario de transacción
        }
    }

    private void openEditTransactionForm(Transaction transaction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/TransactionFormView.fxml"));
            Parent root = loader.load();

            // Obtener el controlador del formulario
            TransactionFormController controller = loader.getController();

            // Configurar el formulario para edición
            controller.configureForEdit(transaction);

            // Configurar callback para refrescar datos cuando se guarde la transacción editada
            controller.setOnTransactionSaved(this::refreshData);

            Stage stage = new Stage();
            stage.setTitle("Editar Transacción");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

        } catch (IOException e) {
            // Error al cargar formulario de edición de transacción
        }
    }

    // Método para abrir el gestor de presupuestos
    private void openBudgetManager() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/BudgetView.fxml"));
            Parent root = loader.load();

            // Obtener el controlador para configurarlo si es necesario
            BudgetViewController controller = loader.getController();

            // Abrir en una nueva ventana
            Stage stage = new Stage();
            stage.setTitle("Gestión de Presupuestos");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root)); // Usar el tamaño definido en el FXML
            stage.showAndWait();

        } catch (IOException e) {
            // Error al cargar gestión de presupuestos
        }
    }
}
