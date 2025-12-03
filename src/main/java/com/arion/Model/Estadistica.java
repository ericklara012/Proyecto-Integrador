package com.arion.Model;

import com.arion.Config.Database;
import java.sql.*;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;


public class Estadistica {


    private int id;
    private YearMonth mes;
    private double totalIngresos;
    private double totalEgresos;
    private int userId;

    // Constructores
    public Estadistica() {
        this.mes = YearMonth.now();
    }

    public Estadistica(YearMonth mes) {
        this.mes = mes;
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public YearMonth getMes() {
        return mes;
    }

    public void setMes(YearMonth mes) {
        this.mes = mes;
    }

    public double getTotalIngresos() {
        return totalIngresos;
    }

    public void setTotalIngresos(double totalIngresos) {
        this.totalIngresos = totalIngresos;
    }

    public double getTotalEgresos() {
        return totalEgresos;
    }

    public void setTotalEgresos(double totalEgresos) {
        this.totalEgresos = totalEgresos;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    // Métodos según diagrama de clases

    /**
     * Registra un nuevo usuario en el sistema
     * @param correo String - correo del usuario
     * @param contraseña String - contraseña del usuario
     * @return boolean - true si se registró exitosamente
     */
    public boolean registrarUsuario(String correo, String contraseña) {
        // Delegar al modelo Usuario
        User user = new User();
        user.setEmail(correo);
        user.setPassword(contraseña);
        user.setUsername(correo.split("@")[0]); // Usar parte del correo como username
        return user.register();
    }

    /**
     * Inicia sesión de un usuario
     * @param correo String - correo del usuario
     * @param contraseña String - contraseña del usuario
     * @return boolean - true si la autenticación fue exitosa
     */
    public boolean iniciarSesion(String correo, String contraseña) {
        User user = User.authenticate(correo, contraseña);
        return user != null;
    }

    /**
     * Valida el correo del usuario
     * @param correo String - correo a validar
     * @return boolean - true si el correo es válido
     */
    public boolean validarCorreoUsuario(String correo) {
        if (correo == null || correo.isEmpty()) {
            return false;
        }
        // Validación básica de formato de email
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return correo.matches(emailRegex);
    }

    /**
     * Valida la contraseña del usuario
     * @param contraseña String - contraseña a validar
     * @return boolean - true si la contraseña es válida
     */
    public boolean validarContraseña(String contraseña) {
        if (contraseña == null || contraseña.isEmpty()) {
            return false;
        }
        // Validación: mínimo 6 caracteres
        return contraseña.length() >= 6;
    }

    /**
     * Calcula las estadísticas financieras para un usuario en un mes específico
     * @param userId int - ID del usuario
     * @param mes YearMonth - mes para calcular las estadísticas
     * @return Estadistica - objeto con las estadísticas calculadas
     */
    public static Estadistica calcularEstadisticas(int userId, YearMonth mes) {
        Estadistica estadistica = new Estadistica(mes);
        estadistica.setUserId(userId);

        String sql = "SELECT " +
                     "  SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END) as total_ingresos, " +
                     "  SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) as total_egresos " +
                     "FROM transactions " +
                     "WHERE user_id = ? AND YEAR(date) = ? AND MONTH(date) = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, mes.getYear());
            stmt.setInt(3, mes.getMonthValue());

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                estadistica.setTotalIngresos(rs.getDouble("total_ingresos"));
                estadistica.setTotalEgresos(rs.getDouble("total_egresos"));
            }
        } catch (Exception e) {
            System.err.println("Error al calcular estadísticas: " + e.getMessage());
        }

        return estadistica;
    }

    /**
     * Obtiene el balance neto (ingresos - egresos)
     * @return double - balance neto
     */
    public double getBalanceNeto() {
        return totalIngresos - totalEgresos;
    }

    /**
     * Obtiene estadísticas por categoría para un usuario en un mes específico
     * @param userId int - ID del usuario
     * @param mes YearMonth - mes para calcular las estadísticas
     * @return Map<String, Double> - mapa de categoría a monto total
     */
    public static Map<String, Double> obtenerEstadisticasPorCategoria(int userId, YearMonth mes) {
        Map<String, Double> estadisticas = new HashMap<>();

        String sql = "SELECT category, SUM(amount) as total " +
                     "FROM transactions " +
                     "WHERE user_id = ? AND YEAR(date) = ? AND MONTH(date) = ? AND type = 'EXPENSE' " +
                     "GROUP BY category";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, mes.getYear());
            stmt.setInt(3, mes.getMonthValue());

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String categoria = rs.getString("category");
                double total = rs.getDouble("total");
                estadisticas.put(categoria, total);
            }
        } catch (Exception e) {
            System.err.println("Error al obtener estadísticas por categoría: " + e.getMessage());
        }

        return estadisticas;
    }

    /**
     * Guarda o actualiza las estadísticas en la base de datos
     * @return boolean - true si se guardó exitosamente
     */
    public boolean guardar() {
        String sql = "INSERT INTO statistics (user_id, month, year, total_income, total_expenses) " +
                     "VALUES (?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE total_income = ?, total_expenses = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, this.userId);
            stmt.setInt(2, this.mes.getMonthValue());
            stmt.setInt(3, this.mes.getYear());
            stmt.setDouble(4, this.totalIngresos);
            stmt.setDouble(5, this.totalEgresos);
            stmt.setDouble(6, this.totalIngresos);
            stmt.setDouble(7, this.totalEgresos);

            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Error al guardar estadísticas: " + e.getMessage());
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("Estadísticas %s - Ingresos: $%.2f, Egresos: $%.2f, Balance: $%.2f",
                mes.toString(), totalIngresos, totalEgresos, getBalanceNeto());
    }
}

