package com.arion.Model;

import com.arion.Config.Database;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase Movimiento (Movement) - representa transacciones financieras
 * Mapea a la entidad "Movimiento" del diagrama de clases
 */
public class Movimiento {

    // Atributos según diagrama de clases
    private int id;
    private String tipo;  // "INGRESO" o "EGRESO"
    private double monto;
    private LocalDate fecha;
    private String categoria;
    private String descripcion;

    // Constructores
    public Movimiento() {}

    public Movimiento(String tipo, double monto, LocalDate fecha, String categoria, String descripcion) {
        this.tipo = tipo;
        this.monto = monto;
        this.fecha = fecha;
        this.categoria = categoria;
        this.descripcion = descripcion;
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    // Métodos según diagrama de clases

    /**
     * Registra un nuevo movimiento en la base de datos
     * @param correo String - correo del usuario
     * @param monto double - monto del movimiento
     * @param fecha Date - fecha del movimiento
     * @param categoria String - categoría del movimiento
     * @param descripcion String - descripción del movimiento
     * @return boolean - true si se registró exitosamente
     */
    public boolean registrarMovimiento(String correo, double monto, LocalDate fecha, String categoria, String descripcion) {
        String sql = "INSERT INTO transactions (user_id, description, category, date, amount, type, note) " +
                     "SELECT u.id, ?, ?, ?, ?, ?, ? FROM users u WHERE u.email = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, descripcion);
            stmt.setString(2, categoria);
            stmt.setDate(3, java.sql.Date.valueOf(fecha));
            stmt.setDouble(4, monto);
            stmt.setString(5, this.tipo);
            stmt.setString(6, descripcion);
            stmt.setString(7, correo);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        this.id = generatedKeys.getInt(1);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error al registrar movimiento: " + e.getMessage());
        }
        return false;
    }

    /**
     * Edita un movimiento existente
     * @param idMovimiento int - ID del movimiento a editar
     * @return boolean - true si se editó exitosamente
     */
    public boolean editarMovimiento(int idMovimiento) {
        String sql = "UPDATE transactions SET description = ?, category = ?, date = ?, amount = ?, type = ? " +
                     "WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, this.descripcion);
            stmt.setString(2, this.categoria);
            stmt.setDate(3, java.sql.Date.valueOf(this.fecha));
            stmt.setDouble(4, this.monto);
            stmt.setString(5, this.tipo);
            stmt.setInt(6, idMovimiento);

            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Error al editar movimiento: " + e.getMessage());
        }
        return false;
    }

    /**
     * Elimina un movimiento de la base de datos
     * @param idMovimiento int - ID del movimiento a eliminar
     * @return boolean - true si se eliminó exitosamente
     */
    public boolean eliminarMovimiento(int idMovimiento) {
        String sql = "DELETE FROM transactions WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idMovimiento);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Error al eliminar movimiento: " + e.getMessage());
        }
        return false;
    }

    /**
     * Filtra movimientos por fecha
     * @param fecha Date - fecha para filtrar
     * @return List<Movimiento> - lista de movimientos en esa fecha
     */
    public static List<Movimiento> filtrarPorFecha(LocalDate fecha) {
        List<Movimiento> movimientos = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE date = ? ORDER BY date DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, java.sql.Date.valueOf(fecha));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Movimiento mov = new Movimiento();
                mov.setId(rs.getInt("id"));
                mov.setTipo(rs.getString("type"));
                mov.setMonto(rs.getDouble("amount"));
                mov.setFecha(rs.getDate("date").toLocalDate());
                mov.setCategoria(rs.getString("category"));
                mov.setDescripcion(rs.getString("description"));
                movimientos.add(mov);
            }
        } catch (Exception e) {
            System.err.println("Error al filtrar movimientos por fecha: " + e.getMessage());
        }
        return movimientos;
    }
}

