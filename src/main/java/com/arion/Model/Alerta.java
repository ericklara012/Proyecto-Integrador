package com.arion.Model;

import com.arion.Config.Database;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase Alerta - gestiona alertas y notificaciones del sistema
 * Mapea a la entidad "Alerta" del diagrama de clases
 */
public class Alerta {

    // Atributos según diagrama de clases
    private int id;
    private String mensaje;
    private LocalDate fecha;
    private int userId;
    private int presupuestoId;
    private boolean leida;

    // Constructores
    public Alerta() {
        this.fecha = LocalDate.now();
        this.leida = false;
    }

    public Alerta(String mensaje, LocalDate fecha) {
        this.mensaje = mensaje;
        this.fecha = fecha;
        this.leida = false;
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getPresupuestoId() {
        return presupuestoId;
    }

    public void setPresupuestoId(int presupuestoId) {
        this.presupuestoId = presupuestoId;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

    // Métodos según diagrama de clases

    /**
     * Genera una alerta cuando un presupuesto está cerca o ha sido excedido
     * @param idPresupuesto int - ID del presupuesto
     * @param idUsuario int - ID del usuario
     */
    public void generarAlerta(int idPresupuesto, int idUsuario) {
        String sql = "INSERT INTO alerts (user_id, budget_id, message, date, read) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, idUsuario);
            stmt.setInt(2, idPresupuesto);
            stmt.setString(3, this.mensaje);
            stmt.setDate(4, java.sql.Date.valueOf(this.fecha));
            stmt.setBoolean(5, this.leida);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        this.id = generatedKeys.getInt(1);
                        this.userId = idUsuario;
                        this.presupuestoId = idPresupuesto;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error al generar alerta: " + e.getMessage());
        }
    }

    /**
     * Envía una notificación al usuario
     * @param correo String - correo del usuario
     * @param idUsuario int - ID del usuario
     */
    public void enviarNotificacion(String correo, int idUsuario) {
        // En una implementación real, aquí se enviaría un correo electrónico
        // Por ahora, solo registramos la notificación en el sistema
        System.out.println("Notificación enviada a: " + correo);
        System.out.println("Mensaje: " + this.mensaje);

        // Guardar la notificación en la base de datos
        this.userId = idUsuario;
        generarAlerta(0, idUsuario);
    }

    /**
     * Obtiene todas las alertas de un usuario
     * @param idUsuario int - ID del usuario
     * @return List<Alerta> - lista de alertas del usuario
     */
    public static List<Alerta> obtenerAlertasPorUsuario(int idUsuario) {
        List<Alerta> alertas = new ArrayList<>();
        String sql = "SELECT * FROM alerts WHERE user_id = ? ORDER BY date DESC, id DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idUsuario);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Alerta alerta = new Alerta();
                alerta.setId(rs.getInt("id"));
                alerta.setUserId(rs.getInt("user_id"));
                alerta.setPresupuestoId(rs.getInt("budget_id"));
                alerta.setMensaje(rs.getString("message"));
                alerta.setFecha(rs.getDate("date").toLocalDate());
                alerta.setLeida(rs.getBoolean("read"));
                alertas.add(alerta);
            }
        } catch (Exception e) {
            // Si la tabla no existe, retornar lista vacía
            System.err.println("Error al obtener alertas por usuario: " + e.getMessage());
        }
        return alertas;
    }

    /**
     * Obtiene alertas no leídas de un usuario
     * @param idUsuario int - ID del usuario
     * @return List<Alerta> - lista de alertas no leídas
     */
    public static List<Alerta> obtenerAlertasNoLeidas(int idUsuario) {
        List<Alerta> alertas = new ArrayList<>();
        String sql = "SELECT * FROM alerts WHERE user_id = ? AND read = false ORDER BY date DESC, id DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idUsuario);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Alerta alerta = new Alerta();
                alerta.setId(rs.getInt("id"));
                alerta.setUserId(rs.getInt("user_id"));
                alerta.setPresupuestoId(rs.getInt("budget_id"));
                alerta.setMensaje(rs.getString("message"));
                alerta.setFecha(rs.getDate("date").toLocalDate());
                alerta.setLeida(rs.getBoolean("read"));
                alertas.add(alerta);
            }
        } catch (Exception e) {
            System.err.println("Error al obtener alertas no leídas: " + e.getMessage());
        }
        return alertas;
    }

    /**
     * Marca una alerta como leída
     * @return boolean - true si se marcó exitosamente
     */
    public boolean marcarComoLeida() {
        String sql = "UPDATE alerts SET read = true WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, this.id);
            boolean result = stmt.executeUpdate() > 0;
            if (result) {
                this.leida = true;
            }
            return result;
        } catch (Exception e) {
            System.err.println("Error al marcar alerta como leída: " + e.getMessage());
        }
        return false;
    }
}

