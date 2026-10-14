package com.sv.grupo11.asociacioncomunal.service.cierre;

import com.sv.grupo11.asociacioncomunal.model.Cuota;
import com.sv.grupo11.asociacioncomunal.model.Socio;
import com.sv.grupo11.asociacioncomunal.repository.CuotaRepository;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/** Tarea concurrente 1: genera (en una transaccion propia) las cuotas de un LOTE de socios activos. */
public class GenerarCuotasTask implements Callable<Integer> {
    private static final Logger LOG = Logger.getLogger(GenerarCuotasTask.class.getName());

    private final CuotaRepository cuotaRepository;
    private final List<Socio> lote;
    private final YearMonth periodo;

    public GenerarCuotasTask(CuotaRepository cuotaRepository, List<Socio> lote, YearMonth periodo) {
        this.cuotaRepository = cuotaRepository;
        this.lote = lote;
        this.periodo = periodo;
    }

    @Override
    public Integer call() {
        List<Cuota> cuotas = lote.stream().map(s -> Cuota.generarPara(s, periodo)).toList();
        int insertadas = cuotaRepository.insertarLote(cuotas);   // usa SU PROPIA conexion del pool
        LOG.info("[" + Thread.currentThread().getName() + "] lote de " + lote.size() + " socio(s): "
                + insertadas + " cuota(s) generada(s) para " + periodo);
        return insertadas;
    }
}
