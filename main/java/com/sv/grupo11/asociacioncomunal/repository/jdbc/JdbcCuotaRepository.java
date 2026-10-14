package com.sv.grupo11.asociacioncomunal.repository.jdbc;

import com.sv.grupo11.asociacioncomunal.model.Cuota;
import com.sv.grupo11.asociacioncomunal.model.EstadoCuota;
import com.sv.grupo11.asociacioncomunal.repository.CuotaRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;

public class JdbcCuotaRepository extends JdbcSupport implements CuotaRepository {

    private static final String COLUMNAS = "id, id_socio, monto, periodo, fecha_vencimiento, fecha_pago, estado";
    private static final String SQL_INSERT =
            "INSERT INTO cuota (id_socio, monto, periodo, fecha_vencimiento, fecha_pago, estado) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_EXISTE = "SELECT id FROM cuota WHERE id_socio = ? AND periodo = ?";

    public JdbcCuotaRepository(DataSource dataSource) { super(dataSource); }

    @Override
    public Cuota guardar(Cuota c) {
        c.setId(insertar(SQL_INSERT, ps -> asignarInsert(ps, c)));
        return c;
    }

    @Override
    public Optional<Cuota> buscarPorId(Long id) {
        return consultarUno("SELECT " + COLUMNAS + " FROM cuota WHERE id = ?", ps -> ps.setLong(1, id), this::mapear);
    }

    @Override
    public List<Cuota> listarTodos() {
        return consultar("SELECT " + COLUMNAS + " FROM cuota ORDER BY id", SIN_PARAMETROS, this::mapear);
    }

    @Override
    public List<Cuota> buscarPorSocio(long idSocio) {
        return consultar("SELECT " + COLUMNAS + " FROM cuota WHERE id_socio = ? ORDER BY periodo DESC",
                ps -> ps.setLong(1, idSocio), this::mapear);
    }

    @Override
    public List<Cuota> buscarPorEstado(EstadoCuota estado) {
        return consultar("SELECT " + COLUMNAS + " FROM cuota WHERE estado = ? ORDER BY id",
                ps -> ps.setString(1, estado.name()), this::mapear);
    }

    @Override
    public List<Cuota> ultimosPagos(int limite) {
        return consultar("SELECT " + COLUMNAS + " FROM cuota WHERE estado = 'PAGADA' ORDER BY fecha_pago DESC, id DESC LIMIT ?",
                ps -> ps.setInt(1, limite), this::mapear);
    }

    @Override
    public boolean existePorSocioYPeriodo(long idSocio, YearMonth periodo) {
        return !consultar(SQL_EXISTE, ps -> { ps.setLong(1, idSocio); ps.setString(2, periodo.toString()); },
                rs -> rs.getLong(1)).isEmpty();
    }

    @Override
    public Map<EstadoCuota, TotalPorEstado> totalesPorEstado() {
        Map<EstadoCuota, TotalPorEstado> mapa = new EnumMap<>(EstadoCuota.class);
        for (EstadoCuota e : EstadoCuota.values()) mapa.put(e, new TotalPorEstado(0, java.math.BigDecimal.ZERO));
        consultar("SELECT estado, COUNT(*) AS n, COALESCE(SUM(monto), 0) AS total FROM cuota GROUP BY estado",
                SIN_PARAMETROS,
                rs -> mapa.put(EstadoCuota.valueOf(rs.getString("estado")),
                        new TotalPorEstado(rs.getLong("n"), rs.getBigDecimal("total"))));
        return mapa;
    }

    @Override
    public boolean marcarPagada(long idCuota, LocalDate fechaPago) {
        int filas = actualizar("UPDATE cuota SET estado = 'PAGADA', fecha_pago = ? WHERE id = ? AND estado <> 'PAGADA'",
                ps -> { setFecha(ps, 1, fechaPago); ps.setLong(2, idCuota); });
        return filas == 1;
    }

    @Override
    public int marcarVencidas(LocalDate corte, YearMonth periodoExcluido) {
        return actualizar("UPDATE cuota SET estado = 'VENCIDA' WHERE estado = 'PENDIENTE' AND fecha_vencimiento < ? AND periodo <> ?",
                ps -> { setFecha(ps, 1, corte); ps.setString(2, periodoExcluido.toString()); });
    }

    @Override
    public int insertarLote(List<Cuota> cuotas) {
        return enTransaccion(con -> {
            int insertadas = 0;
            try (PreparedStatement existe = con.prepareStatement(SQL_EXISTE);
                 PreparedStatement insert = con.prepareStatement(SQL_INSERT)) {
                for (Cuota c : cuotas) {
                    existe.setLong(1, c.getIdSocio());
                    existe.setString(2, c.getPeriodo().toString());
                    try (ResultSet rs = existe.executeQuery()) {
                        if (rs.next()) continue;          // ya existe: se omite (idempotente)
                    }
                    asignarInsert(insert, c);
                    insertadas += insert.executeUpdate();
                }
            }
            return insertadas;
        });
    }

    private void asignarInsert(PreparedStatement ps, Cuota c) throws SQLException {
        ps.setLong(1, c.getIdSocio());
        ps.setBigDecimal(2, c.getMonto());
        ps.setString(3, c.getPeriodo().toString());
        setFecha(ps, 4, c.getFechaVencimiento());
        setFecha(ps, 5, c.getFechaPago());
        ps.setString(6, c.getEstado().name());
    }

    private Cuota mapear(ResultSet rs) throws SQLException {
        return new Cuota(rs.getLong("id"), rs.getLong("id_socio"), rs.getBigDecimal("monto"),
                YearMonth.parse(rs.getString("periodo")),
                rs.getObject("fecha_vencimiento", LocalDate.class),
                rs.getObject("fecha_pago", LocalDate.class),
                EstadoCuota.valueOf(rs.getString("estado")));
    }
}
