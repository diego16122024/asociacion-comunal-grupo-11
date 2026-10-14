package com.sv.grupo11.asociacioncomunal.repository.jdbc;

import com.sv.grupo11.asociacioncomunal.model.EstadoSocio;
import com.sv.grupo11.asociacioncomunal.model.MiembroDirectiva;
import com.sv.grupo11.asociacioncomunal.model.Socio;
import com.sv.grupo11.asociacioncomunal.repository.SocioRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

public class JdbcSocioRepository extends JdbcSupport implements SocioRepository {

    private static final String COLUMNAS =
            "id, tipo, nombre, dui, telefono, direccion, fecha_ingreso, estado, cargo, periodo_directiva";

    public JdbcSocioRepository(DataSource dataSource) { super(dataSource); }

    @Override
    public Socio guardar(Socio s) {
        long id = insertar(
                "INSERT INTO socio (tipo, nombre, dui, telefono, direccion, fecha_ingreso, estado, cargo, periodo_directiva)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                ps -> {
                    ps.setString(1, s.getTipo());
                    ps.setString(2, s.getNombre());
                    ps.setString(3, s.getDui());
                    ps.setString(4, s.getTelefono());
                    ps.setString(5, s.getDireccion());
                    setFecha(ps, 6, s.getFechaIngreso());
                    ps.setString(7, s.getEstado().name());
                    ps.setString(8, s.getCargo());
                    ps.setString(9, s.getPeriodoDirectiva());
                });
        s.setId(id);
        return s;
    }

    @Override
    public void actualizar(Socio s) {
        actualizar("UPDATE socio SET telefono = ?, direccion = ?, estado = ? WHERE id = ?", ps -> {
            ps.setString(1, s.getTelefono());
            ps.setString(2, s.getDireccion());
            ps.setString(3, s.getEstado().name());
            ps.setLong(4, s.getId());
        });
    }

    @Override
    public Optional<Socio> buscarPorId(Long id) {
        return consultarUno("SELECT " + COLUMNAS + " FROM socio WHERE id = ?", ps -> ps.setLong(1, id), this::mapear);
    }

    @Override
    public List<Socio> listarTodos() {
        return consultar("SELECT " + COLUMNAS + " FROM socio ORDER BY id", SIN_PARAMETROS, this::mapear);
    }

    @Override
    public boolean existePorDui(String dui) {
        return !consultar("SELECT id FROM socio WHERE dui = ?", ps -> ps.setString(1, dui), rs -> rs.getLong(1)).isEmpty();
    }

    /** Polimorfismo en la capa de datos: la fila decide si se reconstruye un Socio o un MiembroDirectiva. */
    private Socio mapear(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String nombre = rs.getString("nombre");
        String dui = rs.getString("dui");
        String telefono = rs.getString("telefono");
        String direccion = rs.getString("direccion");
        LocalDate ingreso = rs.getObject("fecha_ingreso", LocalDate.class);
        EstadoSocio estado = EstadoSocio.valueOf(rs.getString("estado"));
        if ("DIRECTIVA".equals(rs.getString("tipo"))) {
            return new MiembroDirectiva(id, nombre, dui, telefono, direccion, ingreso, estado,
                    rs.getString("cargo"), rs.getString("periodo_directiva"));
        }
        return new Socio(id, nombre, dui, telefono, direccion, ingreso, estado);
    }
}
