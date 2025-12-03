package com.arion.Model;

import com.arion.Config.Database;
import java.sql.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

/**
 * Clase Usuario (User) - gestiona usuarios del sistema
 * Mapea a la entidad "Usuario" del diagrama de clases
 */
public class User {
    private int id;
    private String username;
    private String password;
    private String email;
    private LocalDateTime createdAt;

    // Atributos adicionales según diagrama (alias)
    // correo -> email
    // contraseña -> password

    // Constructor vacío
    public User() {}

    // Constructor con parámetros básicos
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    // Constructor completo
    public User(int id, String username, String password, String email, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.createdAt = createdAt;
    }

    // Getters y setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Método para hashear contraseñas
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al hashear la contraseña", e);
        }
    }

    // Método para autenticar usuario
    public static User authenticate(String usernameOrEmail, String password) {
        String query = "SELECT id, username, password, email, created_at FROM users WHERE username = ? OR email = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, usernameOrEmail);
            stmt.setString(2, usernameOrEmail);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");

                User user = new User();
                String hashedInputPassword = user.hashPassword(password);

                if (storedPassword.equals(hashedInputPassword)) {
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(storedPassword);
                    user.setEmail(rs.getString("email"));
                    if (rs.getTimestamp("created_at") != null) {
                        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    }
                    return user;
                }
            }
        } catch (Exception e) {
            // Error en autenticación
        }

        return null;
    }

    // Método para registrar nuevo usuario
    public boolean register() {
        if (usernameExists(this.username)) {
            return false; // Usuario ya existe
        }

        // Verificar que el email no sea nulo
        if (this.email == null || this.email.isEmpty()) {
            return false;
        }

        String query = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, this.username);
            stmt.setString(2, hashPassword(this.password));
            stmt.setString(3, this.email);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    this.id = generatedKeys.getInt(1);
                }
                return true;
            }
        } catch (Exception e) {
            // Error al registrar usuario
        }
        return false;
    }

    // Método para verificar si el username ya existe
    private boolean usernameExists(String username) {
        String query = "SELECT COUNT(*) FROM users WHERE username = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            // Error al verificar username
        }
        return false;
    }

    // Métodos adicionales según diagrama de clases

    /**
     * Registra un nuevo usuario usando correo y contraseña
     * @param correo String - correo del usuario
     * @param contraseña String - contraseña del usuario
     * @return boolean - true si se registró exitosamente
     */
    public boolean registrarUsuario(String correo, String contraseña) {
        this.email = correo;
        this.password = contraseña;
        this.username = correo.split("@")[0]; // Usar parte del correo como username
        return register();
    }

    /**
     * Inicia sesión usando correo y contraseña
     * @param correo String - correo del usuario
     * @param contraseña String - contraseña del usuario
     * @return boolean - true si el inicio de sesión fue exitoso
     */
    public static boolean iniciarSesion(String correo, String contraseña) {
        User user = authenticate(correo, contraseña);
        return user != null;
    }

    /**
     * Valida el formato del correo del usuario
     * @param correo String - correo a validar
     * @return boolean - true si el correo es válido
     */
    public static boolean validarCorreoUsuario(String correo) {
        if (correo == null || correo.isEmpty()) {
            return false;
        }
        // Validación básica de formato de email
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return correo.matches(emailRegex);
    }

    /**
     * Valida que la contraseña cumpla con los requisitos del ERS
     * Requisitos: Mínimo 8 caracteres, debe incluir letras y números
     * @param contraseña String - contraseña a validar
     * @return boolean - true si la contraseña es válida
     */
    public static boolean validarContraseña(String contraseña) {
        if (contraseña == null || contraseña.isEmpty()) {
            return false;
        }

        // ERS Requisito 1.1: Mínimo 8 caracteres
        if (contraseña.length() < 8) {
            return false;
        }

        // ERS Requisito 1.1: Debe incluir letras y números
        boolean tieneLetra = contraseña.matches(".*[a-zA-Z].*");
        boolean tieneNumero = contraseña.matches(".*[0-9].*");

        return tieneLetra && tieneNumero;
    }

    /**
     * Obtiene el correo del usuario (alias de getEmail)
     * @return String - correo del usuario
     */
    public String getCorreo() {
        return this.email;
    }

    /**
     * Establece el correo del usuario (alias de setEmail)
     * @param correo String - correo del usuario
     */
    public void setCorreo(String correo) {
        this.email = correo;
    }

    /**
     * Obtiene la contraseña del usuario (alias de getPassword)
     * @return String - contraseña del usuario
     */
    public String getContraseña() {
        return this.password;
    }

    /**
     * Establece la contraseña del usuario (alias de setPassword)
     * @param contraseña String - contraseña del usuario
     */
    public void setContraseña(String contraseña) {
        this.password = contraseña;
    }
}
