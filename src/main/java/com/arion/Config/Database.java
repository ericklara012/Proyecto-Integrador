package com.arion.Config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class Database {

    private static Connection connection;

    // Método para obtener la conexión
    public static Connection getConnection() throws Exception {
        if (connection == null || connection.isClosed()) {
            // Leer archivo config.properties
            Properties props = new Properties();
            try (InputStream input = Database.class.getResourceAsStream("/config.properties")) {
                if (input == null) {
                    throw new Exception("No se encontró el archivo config.properties");
                }
                props.load(input);
            }

            String url = props.getProperty("db.url");
            String user = props.getProperty("db.user");
            String password = props.getProperty("db.password");

            // Conectar a la base de datos
            connection = DriverManager.getConnection(url, user, password);
        }
        return connection;
    }
}
