package com.arion.Controller;

import com.arion.Model.Transaction;
import com.arion.Config.SessionManager;
import com.arion.Utils.AlertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

// Importaciones para OpenPDF (reemplazar iText)
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;

import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class ReportsViewController implements Initializable {

    @FXML private TableView<Transaction> transactionsTable;
    @FXML private TableColumn<Transaction, String> descriptionCol;
    @FXML private TableColumn<Transaction, String> categoryCol;
    @FXML private TableColumn<Transaction, LocalDate> dateCol;
    @FXML private TableColumn<Transaction, Double> amountCol;
    @FXML private TableColumn<Transaction, Void> actionsCol;
    @FXML private TextField filterField;
    @FXML private Label totalIncomeLabel;
    @FXML private Label totalExpensesLabel;
    @FXML private Label netBalanceLabel;

    private ObservableList<Transaction> transactionList = FXCollections.observableArrayList();
    private FilteredList<Transaction> filteredTransactions;
    private DecimalFormat currencyFormat = new DecimalFormat("$#,##0.00");
    private Runnable dashboardRefreshCallback;

    public void setDashboardRefreshCallback(Runnable callback) {
        this.dashboardRefreshCallback = callback;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadUserTransactions();
        setupTableColumns();
        setupFiltering();
        updateSummaryLabels();
    }

    private void loadUserTransactions() {
        int currentUserId = SessionManager.getInstance().getCurrentUserId();
        if (currentUserId > 0) {
            List<Transaction> userTransactions = Transaction.getTransactionsByUser(currentUserId);
            transactionList.setAll(userTransactions);
        }
    }

    private void setupTableColumns() {
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));

        dateCol.setCellFactory(column -> new TableCell<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(formatter.format(date));
                }
            }
        });

        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                    setStyle("");
                } else {
                    Transaction transaction = getTableView().getItems().get(getIndex());
                    if (transaction.getType() == Transaction.TransactionType.INCOME) {
                        setText(String.format("+$%,.2f", amount));
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    } else {
                        setText(String.format("-$%,.2f", amount));
                        setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                    }
                }
            }
        });

        actionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = createIconButton("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z", "button-icon-edit");
            private final Button deleteBtn = createIconButton("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z", "button-icon-delete");
            private final HBox pane = new HBox(10, editBtn, deleteBtn);

            {
                pane.setAlignment(Pos.CENTER);
                deleteBtn.setOnAction(event -> {
                    Transaction transaction = getTableView().getItems().get(getIndex());
                    if (AlertUtils.showConfirmationAlert("Confirmar eliminación",
                        "¿Estás seguro de que quieres eliminar esta transacción?")) {
                        if (transaction.delete()) {
                            transactionList.remove(transaction);
                            updateSummaryLabels();
                            if (dashboardRefreshCallback != null) {
                                dashboardRefreshCallback.run();
                            }
                            AlertUtils.showSuccessAlert("Éxito", "Transacción eliminada correctamente");
                        } else {
                            AlertUtils.showErrorAlert("Error", "No se pudo eliminar la transacción");
                        }
                    }
                });

                editBtn.setOnAction(event -> {
                    Transaction transaction = getTableView().getItems().get(getIndex());
                    openEditTransactionForm(transaction);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void setupFiltering() {
        filteredTransactions = new FilteredList<>(transactionList, p -> true);

        filterField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredTransactions.setPredicate(transaction -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();

                if (transaction.getDescription() != null && transaction.getDescription().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                if (transaction.getCategory() != null && transaction.getCategory().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                if (transaction.getNote() != null && transaction.getNote().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }

                return false;
            });
        });

        SortedList<Transaction> sortedTransactions = new SortedList<>(filteredTransactions);
        sortedTransactions.comparatorProperty().bind(transactionsTable.comparatorProperty());
        transactionsTable.setItems(sortedTransactions);
    }

    private void updateSummaryLabels() {
        int currentUserId = SessionManager.getInstance().getCurrentUserId();

        if (totalIncomeLabel == null || totalExpensesLabel == null || netBalanceLabel == null) {
            return;
        }

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

        if (netBalance >= 0) {
            netBalanceLabel.setStyle("-fx-text-fill: #4CAF50;");
        } else {
            netBalanceLabel.setStyle("-fx-text-fill: #F44336;");
        }
    }

    private Button createIconButton(String iconPath, String styleClass) {
        Button button = new Button();
        SVGPath icon = new SVGPath();
        icon.setContent(iconPath);
        icon.getStyleClass().add("icon");

        // Asignar un tamaño específico al icono
        icon.setScaleX(1.2);
        icon.setScaleY(1.2);

        // Establecer color para el icono
        if (styleClass.contains("edit")) {
            icon.setStyle("-fx-fill: #007BFF;"); // Azul para editar
        } else if (styleClass.contains("delete")) {
            icon.setStyle("-fx-fill: #DC3545;"); // Rojo para eliminar
        }

        button.setGraphic(icon);
        button.getStyleClass().addAll("button-icon", styleClass);

        // Dar un tamaño mínimo al botón para asegurar visibilidad
        button.setMinSize(30, 30);
        button.setPrefSize(30, 30);

        // Agregar tooltip
        if (styleClass.contains("edit")) {
            Tooltip tooltip = new Tooltip("Editar transacción");
            button.setTooltip(tooltip);
        } else if (styleClass.contains("delete")) {
            Tooltip tooltip = new Tooltip("Eliminar transacción");
            button.setTooltip(tooltip);
        }

        return button;
    }

    private void openEditTransactionForm(Transaction transaction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/TransactionFormView.fxml"));
            Parent root = loader.load();

            TransactionFormController controller = loader.getController();
            controller.populateForm(transaction);
            controller.setOnTransactionSaved(() -> {
                loadUserTransactions();
                updateSummaryLabels();
                if (dashboardRefreshCallback != null) {
                    dashboardRefreshCallback.run();
                }
            });

            Stage stage = new Stage();
            stage.setTitle("Editar Transacción");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

        } catch (IOException e) {
            // Error al cargar formulario de edición
        }
    }

    @FXML
    private void addNewTransaction() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/TransactionFormView.fxml"));
            Parent root = loader.load();

            TransactionFormController controller = loader.getController();
            controller.configureFor(TransactionFormController.FormType.EXPENSE);
            controller.setOnTransactionSaved(() -> {
                loadUserTransactions();
                updateSummaryLabels();
                if (dashboardRefreshCallback != null) {
                    dashboardRefreshCallback.run();
                }
            });

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
    private void refreshData() {
        loadUserTransactions();
        updateSummaryLabels();
    }

    @FXML
    private void downloadTransactions() {
        try {
            // ERS Requisito 3.2: Exportar a PDF o Excel
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Guardar Reporte de Transacciones");
            fileChooser.setInitialFileName("reporte_transacciones");

            // Agregar filtros para PDF y Excel
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Archivos Excel", "*.xlsx")
            );

            Stage stage = (Stage) transactionsTable.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);

            if (file != null) {
                String fileName = file.getName().toLowerCase();

                // Determinar el formato según la extensión del archivo
                if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
                    generateExcel(file);
                    AlertUtils.showSuccessAlert("Éxito", "Reporte Excel generado exitosamente en:\n" + file.getAbsolutePath());
                } else {
                    // Por defecto PDF
                    if (!fileName.endsWith(".pdf")) {
                        file = new File(file.getAbsolutePath() + ".pdf");
                    }
                    generatePDF(file);
                    AlertUtils.showSuccessAlert("Éxito", "Reporte PDF generado exitosamente en:\n" + file.getAbsolutePath());
                }
            }

        } catch (Exception e) {
            AlertUtils.showErrorAlert("Error", "Error al generar el reporte: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void generatePDF(File file) throws Exception {
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        // Título del documento
        com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD);
        Paragraph title = new Paragraph("REPORTE DE TRANSACCIONES", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20f);
        document.add(title);

        // Información del usuario y fecha
        com.lowagie.text.Font normalFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.NORMAL);
        com.lowagie.text.Font boldFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD);

        String username = SessionManager.getInstance().getCurrentUsername();
        Paragraph userInfo = new Paragraph("Usuario: " + username, normalFont);
        userInfo.setSpacingAfter(10f);
        document.add(userInfo);

        Paragraph dateInfo = new Paragraph("Fecha de generación: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), normalFont);
        dateInfo.setSpacingAfter(20f);
        document.add(dateInfo);

        // Resumen financiero
        int currentUserId = SessionManager.getInstance().getCurrentUserId();
        double totalIncome = Transaction.getTotalIncome(currentUserId);
        double totalExpenses = Transaction.getTotalExpenses(currentUserId);
        double netBalance = totalIncome - totalExpenses;

        Paragraph summaryTitle = new Paragraph("RESUMEN FINANCIERO", boldFont);
        summaryTitle.setSpacingAfter(10f);
        document.add(summaryTitle);

        Paragraph incomeP = new Paragraph("Total Ingresos: " + currencyFormat.format(totalIncome), normalFont);
        document.add(incomeP);

        Paragraph expensesP = new Paragraph("Total Gastos: " + currencyFormat.format(totalExpenses), normalFont);
        document.add(expensesP);

        Paragraph balanceP = new Paragraph("Balance Neto: " + currencyFormat.format(netBalance), boldFont);
        balanceP.setSpacingAfter(20f);
        document.add(balanceP);

        // Tabla de transacciones
        Paragraph tableTitle = new Paragraph("DETALLE DE TRANSACCIONES", boldFont);
        tableTitle.setSpacingAfter(10f);
        document.add(tableTitle);

        // Crear tabla con 5 columnas
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        // Configurar anchos de columnas
        float[] columnWidths = {20f, 25f, 15f, 20f, 20f};
        table.setWidths(columnWidths);

        // Headers de la tabla
        addTableHeader(table, "Fecha");
        addTableHeader(table, "Categoría");
        addTableHeader(table, "Tipo");
        addTableHeader(table, "Monto");
        addTableHeader(table, "Descripción");

        // Agregar datos de transacciones
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        com.lowagie.text.Font cellFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.NORMAL);

        for (Transaction transaction : transactionList) {
            // Fecha
            String dateStr = transaction.getDate() != null ? transaction.getDate().format(dateFormatter) : "";
            PdfPCell dateCell = new PdfPCell(new Phrase(dateStr, cellFont));
            table.addCell(dateCell);

            // Categoría
            String category = transaction.getCategory() != null ? transaction.getCategory() : "";
            PdfPCell categoryCell = new PdfPCell(new Phrase(category, cellFont));
            table.addCell(categoryCell);

            // Tipo
            String type = transaction.getType() == Transaction.TransactionType.INCOME ? "Ingreso" : "Gasto";
            PdfPCell typeCell = new PdfPCell(new Phrase(type, cellFont));
            table.addCell(typeCell);

            // Monto
            String amountStr;
            if (transaction.getType() == Transaction.TransactionType.INCOME) {
                amountStr = "+" + currencyFormat.format(transaction.getAmount());
            } else {
                amountStr = "-" + currencyFormat.format(transaction.getAmount());
            }
            PdfPCell amountCell = new PdfPCell(new Phrase(amountStr, cellFont));
            table.addCell(amountCell);

            // Descripción o Nota
            String description = transaction.getNote() != null ? transaction.getNote() :
                                (transaction.getDescription() != null ? transaction.getDescription() : "");
            if (description.length() > 50) {
                description = description.substring(0, 47) + "...";
            }
            PdfPCell descCell = new PdfPCell(new Phrase(description, cellFont));
            table.addCell(descCell);
        }

        document.add(table);

        // Pie de página
        Paragraph footer = new Paragraph("\n\nReporte generado por Arion - Gestor de Finanzas Personales",
                                        new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.ITALIC));
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
    }

    private void addTableHeader(PdfPTable table, String headerText) {
        com.lowagie.text.Font headerFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD);
        PdfPCell header = new PdfPCell(new Phrase(headerText, headerFont));
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        header.setBackgroundColor(new Color(240, 240, 240));
        header.setPadding(5);
        table.addCell(header);
    }

    /**
     * Genera un archivo Excel con las transacciones (ERS Requisito 3.2)
     */
    private void generateExcel(File file) throws Exception {
        int currentUserId = SessionManager.getInstance().getCurrentUserId();
        String username = SessionManager.getInstance().getCurrentUsername();

        // Usar la clase Reporte para generar el Excel
        com.arion.Model.Reporte reporte = com.arion.Model.Reporte.crearReporte(
            currentUserId,
            username,
            LocalDate.now().minusMonths(12), // Últimos 12 meses por defecto
            LocalDate.now()
        );

        reporte.generarExcelDOCUMENTO(file.getAbsolutePath());
    }

    private void showAlert(String title, String message) {
        showAlert(title, message, Alert.AlertType.ERROR);
    }

    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
