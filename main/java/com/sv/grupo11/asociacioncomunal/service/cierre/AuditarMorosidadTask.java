package com.sv.grupo11.asociacioncomunal.service.cierre;

import com.sv.grupo11.asociacioncomunal.repository.CuotaRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Tarea concurrente 2: marca como VENCIDAS las cuotas pendientes cuya fecha limite ya paso.
 * Excluye el periodo que se esta generando para que el resultado NO dependa de que hilo termine primero.
 */
public class AuditarMorosidadTask implements Callable<Integer> {
    private static final Logger LOG = Logger.getLogger(AuditarMorosidadTask.class.getName());

    private final CuotaRepository cuotaRepository;
    private final LocalDate corte;
    private final YearMonth periodoEnGeneracion;

    public AuditarMorosidadTask(CuotaRepository cuotaRepository, LocalDate corte, YearMonth periodoEnGeneracion) {
        this.cuotaRepository = cuotaRepository;
        this.corte = corte;
        this.periodoEnGeneracion = periodoEnGeneracion;
    }

    @Override
    public Integer call() {
        int vencidas = cuotaRepository.marcarVencidas(corte, periodoEnGeneracion);
        LOG.info("[" + Thread.currentThread().getName() + "] auditoria: " + vencidas + " cuota(s) marcada(s) como vencida(s)");
        return vencidas;
    }
}
