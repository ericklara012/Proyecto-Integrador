package com.arion.Model;

import com.arion.Config.Database;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.FileOutputStream;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


public class Reporte {

    // Atributos según diagrama de clases
    private int id;
    private String rangoFechas;
    private String formato;
    private String nombreUsuario;
    private double ingresoTotal;
    private double totalGastos;
    private double totalBalance;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private List<Transaction> movimientos;

    // Constructores
    public Reporte() {
        this.formato = "PDF";
        this.movimientos = new ArrayList<>();
    }

    public Reporte(String rangoFechas, String formato) {
        this.rangoFechas = rangoFechas;
        this.formato = formato;
        this.movimientos = new ArrayList<>();
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRangoFechas() {
        return rangoFechas;
    }

    public void setRangoFechas(String rangoFechas) {
        this.rangoFechas = rangoFechas;
    }

    public String getFormato() {
        return formato;
    }

    public void setFormato(String formato) {
        this.formato = formato;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public double getIngresoTotal() {
        return ingresoTotal;
    }

    public void setIngresoTotal(double ingresoTotal) {
        this.ingresoTotal = ingresoTotal;
    }

    public double getTotalGastos() {
        return totalGastos;
    }

    public void setTotalGastos(double totalGastos) {
        this.totalGastos = totalGastos;
    }

    public double getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(double totalBalance) {
        this.totalBalance = totalBalance;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public List<Transaction> getMovimientos() {
        return movimientos;
    }

    public void setMovimientos(List<Transaction> movimientos) {
        this.movimientos = movimientos;
    }

    // Métodos según diagrama de clases

    /**
     * Exporta el reporte en el formato especificado
     * @param rangoFechas String - rango de fechas para el reporte
     * @param formato String - formato de exportación (PDF, CSV, EXCEL)
     * @param documento String - archivo de destino
     */
    public void exportarReporte(String rangoFechas, String formato, String documento) {
        this.rangoFechas = rangoFechas;
        this.formato = formato;

        // Parsear rango de fechas
        String[] fechas = rangoFechas.split(" to ");
        if (fechas.length == 2) {
            this.fechaInicio = LocalDate.parse(fechas[0]);
            this.fechaFin = LocalDate.parse(fechas[1]);
        }

        // Inicializar datos del reporte
        initiarDatos(movimientos);

        // Generar el reporte según el formato (ERS Requisito 3.2)
        if ("PDF".equalsIgnoreCase(formato)) {
            generarPDFDOCUMENTO(documento);
        } else if ("EXCEL".equalsIgnoreCase(formato) || "XLS".equalsIgnoreCase(formato) || "XLSX".equalsIgnoreCase(formato)) {
            generarExcelDOCUMENTO(documento);
        }
    }

    /**
     * Inicializa los datos del reporte con una lista de movimientos
     * @param movimientos List<Transaction> - lista de movimientos a incluir
     */
    public void initiarDatos(List<Transaction> movimientos) {
        this.movimientos = movimientos;

        // Calcular totales
        this.ingresoTotal = 0;
        this.totalGastos = 0;

        for (Transaction mov : movimientos) {
            if (mov.getType() == Transaction.TransactionType.INCOME) {
                this.ingresoTotal += mov.getAmount();
            } else {
                this.totalGastos += mov.getAmount();
            }
        }

        this.totalBalance = this.ingresoTotal - this.totalGastos;
    }

    /**
     * Genera un documento PDF con el reporte
     * @param documento String - ruta del archivo de destino
     */
    public void generarPDFDOCUMENTO(String documento) {
        try {
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, new FileOutputStream(documento));
            doc.open();

            DecimalFormat currencyFormat = new DecimalFormat("$#,##0.00");

            // Título del documento
            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD);
            Paragraph title = new Paragraph("REPORTE DE TRANSACCIONES", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            doc.add(title);

            // Información del usuario y fecha
            com.lowagie.text.Font normalFont = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.NORMAL);
            com.lowagie.text.Font boldFont = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD);

            if (this.nombreUsuario != null && !this.nombreUsuario.isEmpty()) {
                Paragraph userInfo = new Paragraph("Usuario: " + this.nombreUsuario, normalFont);
                userInfo.setSpacingAfter(10f);
                doc.add(userInfo);
            }

            Paragraph dateInfo = new Paragraph(
                "Fecha de generación: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                normalFont);
            dateInfo.setSpacingAfter(10f);
            doc.add(dateInfo);

            if (this.rangoFechas != null && !this.rangoFechas.isEmpty()) {
                Paragraph rangeInfo = new Paragraph("Rango de fechas: " + this.rangoFechas, normalFont);
                rangeInfo.setSpacingAfter(20f);
                doc.add(rangeInfo);
            }

            // Resumen financiero
            Paragraph summaryTitle = new Paragraph("RESUMEN FINANCIERO", boldFont);
            summaryTitle.setSpacingAfter(10f);
            doc.add(summaryTitle);

            Paragraph incomeP = new Paragraph(
                "Total Ingresos: " + currencyFormat.format(this.ingresoTotal), normalFont);
            doc.add(incomeP);

            Paragraph expensesP = new Paragraph(
                "Total Gastos: " + currencyFormat.format(this.totalGastos), normalFont);
            doc.add(expensesP);

            Paragraph balanceP = new Paragraph(
                "Balance Neto: " + currencyFormat.format(this.totalBalance), boldFont);
            balanceP.setSpacingAfter(20f);
            doc.add(balanceP);

            // Tabla de transacciones
            Paragraph tableTitle = new Paragraph("DETALLE DE TRANSACCIONES", boldFont);
            tableTitle.setSpacingAfter(10f);
            doc.add(tableTitle);

            // Crear tabla con 5 columnas
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // Configurar anchos de columnas
            try {
                float[] columnWidths = {20f, 25f, 15f, 20f, 20f};
                table.setWidths(columnWidths);
            } catch (DocumentException de) {
                System.err.println("Error al configurar anchos de columna: " + de.getMessage());
            }

            // Headers de la tabla
            addTableHeader(table, "Fecha");
            addTableHeader(table, "Categoría");
            addTableHeader(table, "Tipo");
            addTableHeader(table, "Monto");
            addTableHeader(table, "Descripción");

            // Agregar datos de transacciones
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            com.lowagie.text.Font cellFont = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.NORMAL);

            for (Transaction transaction : this.movimientos) {
                // Fecha
                String dateStr = transaction.getDate() != null
                    ? transaction.getDate().format(dateFormatter) : "";
                PdfPCell dateCell = new PdfPCell(new Phrase(dateStr, cellFont));
                table.addCell(dateCell);

                // Categoría
                String category = transaction.getCategory() != null ? transaction.getCategory() : "";
                PdfPCell categoryCell = new PdfPCell(new Phrase(category, cellFont));
                table.addCell(categoryCell);

                // Tipo
                String type = transaction.getType() == Transaction.TransactionType.INCOME
                    ? "Ingreso" : "Gasto";
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

            doc.add(table);

            // Pie de página
            Paragraph footer = new Paragraph(
                "\n\nReporte generado por Arion - Gestor de Finanzas Personales",
                new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.ITALIC));
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();

        } catch (Exception e) {
            System.err.println("Error al generar PDF: " + e.getMessage());
        }
    }

    /**
     * Método auxiliar para agregar encabezados a la tabla
     */
    private void addTableHeader(PdfPTable table, String headerText) {
        com.lowagie.text.Font headerFont = new com.lowagie.text.Font(
            com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD);
        PdfPCell header = new PdfPCell(new Phrase(headerText, headerFont));
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        header.setBackgroundColor(new Color(240, 240, 240));
        header.setPadding(5);
        table.addCell(header);
    }

    /**
     * Obtiene todas las transacciones de un usuario en un rango de fechas
     * @param userId int - ID del usuario
     * @param fechaInicio LocalDate - fecha de inicio
     * @param fechaFin LocalDate - fecha de fin
     * @return List<Transaction> - lista de transacciones
     */
    public static List<Transaction> obtenerTransaccionesEnRango(int userId, LocalDate fechaInicio, LocalDate fechaFin) {
        List<Transaction> transacciones = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE user_id = ? AND date BETWEEN ? AND ? ORDER BY date DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setDate(2, java.sql.Date.valueOf(fechaInicio));
            stmt.setDate(3, java.sql.Date.valueOf(fechaFin));

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Transaction transaction = new Transaction(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("description"),
                    rs.getString("category"),
                    rs.getDate("date").toLocalDate(),
                    rs.getDouble("amount"),
                    Transaction.TransactionType.valueOf(rs.getString("type")),
                    rs.getString("note")
                );
                transacciones.add(transaction);
            }
        } catch (Exception e) {
            System.err.println("Error al obtener transacciones en rango: " + e.getMessage());
        }

        return transacciones;
    }

    /**
     * Crea un reporte completo para un usuario
     * @param userId int - ID del usuario
     * @param nombreUsuario String - nombre del usuario
     * @param fechaInicio LocalDate - fecha de inicio
     * @param fechaFin LocalDate - fecha de fin
     * @return Reporte - objeto de reporte con todos los datos
     */
    public static Reporte crearReporte(int userId, String nombreUsuario, LocalDate fechaInicio, LocalDate fechaFin) {
        Reporte reporte = new Reporte();
        reporte.setNombreUsuario(nombreUsuario);
        reporte.setFechaInicio(fechaInicio);
        reporte.setFechaFin(fechaFin);
        reporte.setRangoFechas(fechaInicio.toString() + " to " + fechaFin.toString());

        // Obtener transacciones del rango
        List<Transaction> transacciones = obtenerTransaccionesEnRango(userId, fechaInicio, fechaFin);
        reporte.initiarDatos(transacciones);

        return reporte;
    }

    /**
     * Genera un documento Excel con el reporte (ERS Requisito 3.2)
     * @param documento String - ruta del archivo de destino
     */
    public void generarExcelDOCUMENTO(String documento) {
        try {
            // Importar clases de Apache POI
            org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Reporte Financiero");

            // Estilos
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            org.apache.poi.ss.usermodel.CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("$#,##0.00"));

            int rowNum = 0;

            // Título
            org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("REPORTE DE TRANSACCIONES");
            org.apache.poi.ss.usermodel.CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            // Información del usuario
            rowNum++;
            if (this.nombreUsuario != null && !this.nombreUsuario.isEmpty()) {
                org.apache.poi.ss.usermodel.Row userRow = sheet.createRow(rowNum++);
                userRow.createCell(0).setCellValue("Usuario:");
                userRow.createCell(1).setCellValue(this.nombreUsuario);
            }

            // Fecha de generación
            org.apache.poi.ss.usermodel.Row dateRow = sheet.createRow(rowNum++);
            dateRow.createCell(0).setCellValue("Fecha de generación:");
            dateRow.createCell(1).setCellValue(LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            if (this.rangoFechas != null && !this.rangoFechas.isEmpty()) {
                org.apache.poi.ss.usermodel.Row rangeRow = sheet.createRow(rowNum++);
                rangeRow.createCell(0).setCellValue("Rango de fechas:");
                rangeRow.createCell(1).setCellValue(this.rangoFechas);
            }

            // Resumen financiero
            rowNum++;
            org.apache.poi.ss.usermodel.Row summaryTitleRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell summaryTitleCell = summaryTitleRow.createCell(0);
            summaryTitleCell.setCellValue("RESUMEN FINANCIERO");
            summaryTitleCell.setCellStyle(headerStyle);

            org.apache.poi.ss.usermodel.Row incomeRow = sheet.createRow(rowNum++);
            incomeRow.createCell(0).setCellValue("Total Ingresos:");
            org.apache.poi.ss.usermodel.Cell incomeCell = incomeRow.createCell(1);
            incomeCell.setCellValue(this.ingresoTotal);
            incomeCell.setCellStyle(currencyStyle);

            org.apache.poi.ss.usermodel.Row expensesRow = sheet.createRow(rowNum++);
            expensesRow.createCell(0).setCellValue("Total Gastos:");
            org.apache.poi.ss.usermodel.Cell expensesCell = expensesRow.createCell(1);
            expensesCell.setCellValue(this.totalGastos);
            expensesCell.setCellStyle(currencyStyle);

            org.apache.poi.ss.usermodel.Row balanceRow = sheet.createRow(rowNum++);
            balanceRow.createCell(0).setCellValue("Balance Neto:");
            org.apache.poi.ss.usermodel.Cell balanceCell = balanceRow.createCell(1);
            balanceCell.setCellValue(this.totalBalance);
            balanceCell.setCellStyle(currencyStyle);

            // Detalle de transacciones
            rowNum++;
            org.apache.poi.ss.usermodel.Row detailTitleRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell detailTitleCell = detailTitleRow.createCell(0);
            detailTitleCell.setCellValue("DETALLE DE TRANSACCIONES");
            detailTitleCell.setCellStyle(headerStyle);

            // Encabezados de la tabla
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"Fecha", "Categoría", "Tipo", "Monto", "Descripción"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Datos de transacciones
            java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
            for (Transaction transaction : this.movimientos) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);

                // Fecha
                row.createCell(0).setCellValue(
                    transaction.getDate() != null ? transaction.getDate().format(dateFormatter) : ""
                );

                // Categoría
                row.createCell(1).setCellValue(
                    transaction.getCategory() != null ? transaction.getCategory() : ""
                );

                // Tipo
                row.createCell(2).setCellValue(
                    transaction.getType() == Transaction.TransactionType.INCOME ? "Ingreso" : "Gasto"
                );

                // Monto
                org.apache.poi.ss.usermodel.Cell amountCell = row.createCell(3);
                amountCell.setCellValue(transaction.getAmount());
                amountCell.setCellStyle(currencyStyle);

                // Descripción
                String description = transaction.getNote() != null ? transaction.getNote() :
                    (transaction.getDescription() != null ? transaction.getDescription() : "");
                row.createCell(4).setCellValue(description);
            }

            // Ajustar ancho de columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Escribir archivo
            try (java.io.FileOutputStream fileOut = new java.io.FileOutputStream(documento)) {
                workbook.write(fileOut);
            }

            workbook.close();

        } catch (Exception e) {
            System.err.println("Error al generar Excel: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

