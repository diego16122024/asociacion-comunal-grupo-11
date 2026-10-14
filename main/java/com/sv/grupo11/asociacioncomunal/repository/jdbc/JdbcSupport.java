package com.sv.grupo11.asociacioncomunal.repository.jdbc;

import com.sv.grupo11.asociacioncomunal.exception.ConexionBdException;
import com.sv.grupo11.asociacioncomunal.exception.DuplicateKeyException;
import com.sv.grupo11.asociacioncomunal.exception.RepositoryException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Plantilla JDBC reutilizable. Concentra en un solo lugar: obtener la conexion del DataSource,
 * try-with-resources (Connection, PreparedStatement, ResultSet), traduccion de SQLException y
 * transacciones. Asi cada DAO solo escribe su SQL y su mapeo (elimina codigo duplicado).
 */
abstract class JdbcSupport {

    @FunctionalInterface interface RowMapper<T> { T mapear(ResultSet rs) throws SQLException; }
    @FunctionalInterface interface StatementSetter { void asignar(PreparedStatement ps) throws SQLException; }
    @FunctionalInterface interface TrabajoTransaccional<R> { R ejecutar(Connection con) throws SQLException; }

    protected static final StatementSetter SIN_PARAMETROS = ps -> { };

    private final DataSource dataSource;

    protected JdbcSupport(DataSource dataSource) { this.dataSource = dataSource; }

    protected Connection abrir() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new ConexionBdException("No se pudo obtener una conexion del pool: " + e.getMessage(), e);
        }
    }

    /** SELECT: executeQuery() + mapeo fila a fila. */
    protected <T> List<T> consultar(String sql, StatementSetter setter, RowMapper<T> mapper) {
        try (Connection con = abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setter.asignar(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> filas = new ArrayList<>();
                while (rs.next()) filas.add(mapper.mapear(rs));
                return filas;
            }
        } catch (SQLException e) {
            throw traducir("consulta", e);
        }
    }

    protected <T> Optional<T> consultarUno(String sql, StatementSetter setter, RowMapper<T> mapper) {
        return consultar(sql, setter, mapper).stream().findFirst();
    }

    /** INSERT/UPDATE/DELETE: executeUpdate(). Devuelve las filas afectadas. */
    protected int actualizar(String sql, StatementSetter setter) {
        try (Connection con = abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setter.asignar(ps);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw traducir("actualizacion", e);
        }
    }

    /** INSERT que devuelve la llave autogenerada. */
    protected long insertar(String sql, StatementSetter setter) {
        try (Connection con = abrir();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setter.asignar(ps);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new RepositoryException("La BD no devolvio la llave generada", null);
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw traducir("insercion", e);
        }
    }

    /** Ejecuta varias sentencias en una transaccion: commit si todo sale bien, rollback si falla. */
    protected <R> R enTransaccion(TrabajoTransaccional<R> trabajo) {
        try (Connection con = abrir()) {
            boolean autoCommitOriginal = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                R resultado = trabajo.ejecutar(con);
                con.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    e.addSuppressed(ex);
                }
                if (e instanceof SQLException sql) throw traducir("transaccion", sql);
                throw (RuntimeException) e;
            } finally {
                con.setAutoCommit(autoCommitOriginal);   // la conexion vuelve limpia al pool
            }
        } catch (SQLException e) {
            throw traducir("transaccion", e);
        }
    }

    protected RepositoryException traducir(String contexto, SQLException e) {
        String estado = e.getSQLState();
        if (estado != null && estado.startsWith("23")) {
            return new DuplicateKeyException("Violacion de restriccion de unicidad/integridad en " + contexto, e);
        }
        if (estado != null && estado.startsWith("08")) {
            return new ConexionBdException("Error de conexion con la base de datos en " + contexto, e);
        }
        return new RepositoryException("Error JDBC en " + contexto + ": " + e.getMessage(), e);
    }

    // ---- helpers para valores nulos ----
    protected static void setFecha(PreparedStatement ps, int idx, LocalDate fecha) throws SQLException {
        if (fecha == null) ps.setNull(idx, Types.DATE); else ps.setObject(idx, fecha);
    }

    protected static void setLongNulo(PreparedStatement ps, int idx, Long valor) throws SQLException {
        if (valor == null) ps.setNull(idx, Types.BIGINT); else ps.setLong(idx, valor);
    }

    protected static Long getLongNulo(ResultSet rs, String columna) throws SQLException {
        long v = rs.getLong(columna);
        return rs.wasNull() ? null : v;
    }
}
