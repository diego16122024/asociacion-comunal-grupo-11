package com.sv.grupo11.asociacioncomunal.repository;

import com.sv.grupo11.asociacioncomunal.model.Cuota;
import com.sv.grupo11.asociacioncomunal.model.EstadoCuota;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public interface CuotaRepository extends CrudRepository<Cuota, Long> {

    record TotalPorEstado(long cantidad, BigDecimal monto) { }

    List<Cuota> buscarPorSocio(long idSocio);
    List<Cuota> buscarPorEstado(EstadoCuota estado);
    List<Cuota> ultimosPagos(int limite);
    boolean existePorSocioYPeriodo(long idSocio, YearMonth periodo);
    Map<EstadoCuota, TotalPorEstado> totalesPorEstado();

    /**
     * Pago ATOMICO: UPDATE ... WHERE estado <> 'PAGADA'. Devuelve false si otra transaccion
     * ya la habia pagado (evita doble pago concurrente).
     */
    boolean marcarPagada(long idCuota, LocalDate fechaPago);

    /** Marca como VENCIDA las PENDIENTES con vencimiento anterior al corte (excepto el periodo indicado). */
    int marcarVencidas(LocalDate corte, YearMonth periodoExcluido);

    /** Inserta un lote en UNA transaccion; omite las cuotas que ya existian. Devuelve cuantas inserto. */
    int insertarLote(List<Cuota> cuotas);
}
