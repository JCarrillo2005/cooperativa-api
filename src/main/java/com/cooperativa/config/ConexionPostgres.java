package com.cooperativa.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/** Conexiones independientes a la base secundaria, configuradas fuera del código. */
public final class ConexionPostgres {
    private ConexionPostgres() { }

    public static Connection getConexion() throws SQLException {
        return abrir(cargarConfiguracion());
    }

    /** Permite compartir una misma configuración durante todo el respaldo. */
    public static Connection abrir(Properties configuracion) throws SQLException {
        String url = requerida(configuracion, "pg.url");
        if (!url.startsWith("jdbc:postgresql:")) {
            throw new SQLException("pg.url debe ser una URL JDBC de PostgreSQL.", "08001");
        }
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("Falta lib/postgresql-42.7.13.jar en el classpath.", "08001", ex);
        }
        return DriverManager.getConnection(url, requerida(configuracion, "pg.user"),
                requerida(configuracion, "pg.password"));
    }

    public static Properties cargarConfiguracion() throws SQLException {
        Properties configuracion = new Properties();
        try (FileInputStream archivo = new FileInputStream("config.properties")) {
            configuracion.load(archivo);
        } catch (IOException ex) {
            throw new SQLException("No se pudo leer config.properties en la raíz del proyecto.", "08001");
        }
        return configuracion;
    }

    public static String requerida(Properties configuracion, String clave) throws SQLException {
        String valor = configuracion.getProperty(clave);
        if (valor == null || valor.trim().isEmpty()) {
            throw new SQLException("Falta la propiedad " + clave + " en config.properties.", "08001");
        }
        return valor;
    }

    public static boolean probarConexion() {
        try (Connection conexion = getConexion();
             PreparedStatement consulta = conexion.prepareStatement("SELECT 1");
             ResultSet resultado = consulta.executeQuery()) {
            return resultado.next() && resultado.getInt(1) == 1;
        } catch (SQLException ex) {
            return false;
        }
    }

    /** Prueba real del driver y del servidor sin mostrar URL, usuario ni contraseña. */
    public static void main(String[] argumentos) {
        if (argumentos.length != 0) {
            System.err.println("Uso: com.cooperativa.config.ConexionPostgres");
            System.exit(2);
        }
        try (Connection conexion = getConexion();
             PreparedStatement consulta = conexion.prepareStatement("SELECT 1");
             ResultSet resultado = consulta.executeQuery()) {
            if (!resultado.next() || resultado.getInt(1) != 1) {
                throw new SQLException("La prueba no devolvió el resultado esperado.", "08001");
            }
            System.out.println("Conexión PostgreSQL verificada mediante JDBC.");
        } catch (SQLException ex) {
            System.err.println("No se pudo verificar PostgreSQL. SQLState=" + ex.getSQLState()
                    + "; código=" + ex.getErrorCode()
                    + ". Revisar configuración, servicio y classpath según scripts/README.md.");
            System.exit(1);
        }
    }
}
