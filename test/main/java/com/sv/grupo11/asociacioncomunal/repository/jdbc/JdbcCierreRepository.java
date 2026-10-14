package com.sv.grupo11.asociacioncomunal.repository.jdbc;

import com.sv.grupo11.asociacioncomunal.dto.ResultadoCierre;
import com.sv.grupo11.asociacioncomunal.repository.CierreRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import javax.sql.DataSource;

public class JdbcCierreRepository extends JdbcSupport implements CierreRepository {

    public JdbcCierreRepository(DataSource dataSource) { super(dataSource); }

    @Override
    public void guardar(ResultadoCierre r) {
        insertar("INSERT INTO cierre_mensual (periodo, cuotas_generadas, cuotas_vencidas, duracion_ms, fecha_corte,"
                + " fecha_ejecucion, ejecutado_por) VALUES (?, ?, ?, ?, ?, ?, ?)", ps -> {
            ps.setString(1, r.periodo().toString());
            ps.setInt(2, r.cuotasGeneradas());
            ps.setInt(3, r.cuotasVencidas());
            ps.setLong(4, r.duracionMs());
            setFecha(ps, 5, r.fechaCorte());
            setFecha(ps, 6, r.fechaEjecucion());
            ps.setString(7, r.ejecutadoPor());
        });
    }

    @Override
    public Optional<ResultadoCierre> ultimo() {
        return consultarUno("SELECT periodo, cuotas_generadas, cuotas_vencidas, duracion_ms, fecha_corte, fecha_ejecucion,"
                + " ejecutado_por FROM cierre_mensual ORDER BY id DESC LIMIT 1", SIN_PARAMETROS, this::mapear);
    }

    private ResultadoCierre mapear(ResultSet rs) throws SQLException {
        return new ResultadoCierre(YearMonth.parse(rs.getString("periodo")), rs.getInt("cuotas_generadas"),
                rs.getInt("cuotas_vencidas"), rs.getLong("duracion_ms"),
                rs.getObject("fecha_corte", LocalDate.class), rs.getObject("fecha_ejecucion", LocalDate.class),
                rs.getString("ejecutado_por"));
    }
}
