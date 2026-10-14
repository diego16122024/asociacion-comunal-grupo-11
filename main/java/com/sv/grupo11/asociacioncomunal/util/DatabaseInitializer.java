package com.sv.grupo11.asociacioncomunal.util;

import com.sv.grupo11.asociacioncomunal.exception.ConexionBdException;
import com.sv.grupo11.asociacioncomunal.exception.RepositoryException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.sql.DataSource;

/** Ejecuta database/schema.sql y, si la BD esta vacia, database/data.sql. */
public final class DatabaseInitializer {
    private static final Logger LOG = Logger.getLogger(DatabaseInitializer.class.getName());

    private DatabaseInitializer() { }

    public static void inicializar(DataSource ds, boolean cargarDatos) throws RepositoryException {
        ejecutarScript(ds, "database/schema.sql");
        if (cargarDatos && estaVacia(ds)) {
            ejecutarScript(ds, "database/data.sql");
            LOG.info("Datos de prueba cargados.");
        }
    }

    private static boolean estaVacia(DataSource ds) {
        try (Connection con = ds.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM usuario")) {
            return rs.next() && rs.getInt(1) == 0;
        } catch (SQLException e) {
            throw new ConexionBdException("No se pudo consultar el estado de la base de datos", e);
        }
    }

    private static void ejecutarScript(DataSource ds, String recurso) {
        List<String> sentencias = leerSentencias(recurso);
        try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
            for (String sql : sentencias) {
                st.executeUpdate(sql);
            }
        } catch (SQLException e) {
            throw new ConexionBdException("Error ejecutando " + recurso, e);
        }
    }

    static List<String> leerSentencias(String recurso) {
        try (InputStream in = DatabaseInitializer.class.getClassLoader().getResourceAsStream(recurso)) {
            if (in == null) throw new RepositoryException("No se encontro el script " + recurso, null);
            List<String> resultado = new ArrayList<>();
            StringBuilder actual = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    String t = linea.trim();
                    if (t.isEmpty() || t.startsWith("--")) continue;
                    actual.append(linea).append('\n');
                    if (t.endsWith(";")) {
                        String sql = actual.toString().trim();
                        resultado.add(sql.substring(0, sql.length() - 1));
                        actual.setLength(0);
                    }
                }
            }
            return resultado;
        } catch (IOException e) {
            throw new RepositoryException("No se pudo leer el script " + recurso, e);
        }
    }
}
