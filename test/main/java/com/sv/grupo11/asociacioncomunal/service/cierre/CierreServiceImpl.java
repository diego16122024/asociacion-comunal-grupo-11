package com.sv.grupo11.asociacioncomunal.service.cierre;

import com.sv.grupo11.asociacioncomunal.dto.ResultadoCierre;
import com.sv.grupo11.asociacioncomunal.exception.CierreMensualException;
import com.sv.grupo11.asociacioncomunal.exception.OperacionEnCursoException;
import com.sv.grupo11.asociacioncomunal.exception.ValidacionException;
import com.sv.grupo11.asociacioncomunal.model.Socio;
import com.sv.grupo11.asociacioncomunal.repository.CierreRepository;
import com.sv.grupo11.asociacioncomunal.repository.CuotaRepository;
import com.sv.grupo11.asociacioncomunal.repository.SocioRepository;
import com.sv.grupo11.asociacioncomunal.service.CierreService;
import com.sv.grupo11.asociacioncomunal.util.Listas;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * Cierre mensual CONCURRENTE (ExecutorService + Callable + Future).
 *
 * <p>Que se ejecuta en paralelo: N tareas de generacion de cuotas (una por lote de socios) y 1 tarea de auditoria
 * de morosidad. Cada tarea toma su propia conexion del pool (DataSource) y la devuelve al terminar.
 *
 * <p>Control de problemas de concurrencia:
 * <ul>
 *   <li>ReentrantLock.tryLock: impide dos cierres simultaneos (el segundo recibe OperacionEnCursoException).</li>
 *   <li>UNIQUE(id_socio, periodo) en BD: red de seguridad contra cuotas duplicadas.</li>
 *   <li>Cada lote es una transaccion (commit/rollback) y el UPDATE de morosidad es una sola sentencia atomica.</li>
 *   <li>La auditoria excluye el periodo recien generado: el resultado es determinista, sin importar el orden de los hilos.</li>
 *   <li>Future.get con timeout + manejo de InterruptedException/ExecutionException.</li>
 *   <li>El cierre es idempotente: si fallo a medias se puede ejecutar de nuevo sin duplicar nada.</li>
 * </ul>
 */
public class CierreServiceImpl implements CierreService, AutoCloseable {
    private static final Logger LOG = Logger.getLogger(CierreServiceImpl.class.getName());

    private final SocioRepository socioRepository;
    private final CuotaRepository cuotaRepository;
    private final CierreRepository cierreRepository;
    private final ExecutorService executor;
    private final int tamanoLote;
    private final long timeoutSegundos;
    private final Clock clock;
    private final ReentrantLock cierreEnCurso = new ReentrantLock();

    public CierreServiceImpl(SocioRepository socioRepository, CuotaRepository cuotaRepository,
                             CierreRepository cierreRepository, ExecutorService executor,
                             int tamanoLote, long timeoutSegundos, Clock clock) {
        this.socioRepository = socioRepository;
        this.cuotaRepository = cuotaRepository;
        this.cierreRepository = cierreRepository;
        this.executor = executor;
        this.tamanoLote = tamanoLote;
        this.timeoutSegundos = timeoutSegundos;
        this.clock = clock;
    }

    @Override
    public ResultadoCierre ejecutarCierre(YearMonth periodo, LocalDate corte, String ejecutadoPor)
            throws CierreMensualException {
        if (periodo == null || corte == null) {
            throw new ValidacionException("El periodo y la fecha de corte son obligatorios.");
        }
        if (!cierreEnCurso.tryLock()) {
            throw new OperacionEnCursoException("Ya hay un cierre en ejecucion.");
        }
        try {
            long inicio = System.nanoTime();
            List<Socio> activos = socioRepository.listarTodos().stream().filter(Socio::estaActivo).toList();

            List<Future<Integer>> generacion = new ArrayList<>();
            Future<Integer> auditoria;
            try {
                for (List<Socio> lote : Listas.partir(activos, tamanoLote)) {
                    generacion.add(executor.submit(new GenerarCuotasTask(cuotaRepository, lote, periodo)));
                }
                auditoria = executor.submit(new AuditarMorosidadTask(cuotaRepository, corte, periodo));
            } catch (RejectedExecutionException e) {
                throw new CierreMensualException("El servicio de ejecucion no acepta tareas", e);
            }

            List<String> errores = new ArrayList<>();
            List<Future<Integer>> todas = new ArrayList<>(generacion);
            todas.add(auditoria);
            int generadas = recolectar(generacion, todas, errores, "generacion");
            int vencidas = recolectar(List.of(auditoria), todas, errores, "auditoria");

            if (!errores.isEmpty()) {
                throw new CierreMensualException("El cierre de " + periodo + " termino con errores", errores);
            }
            long duracionMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
            ResultadoCierre resultado = new ResultadoCierre(periodo, generadas, vencidas, duracionMs,
                    corte, LocalDate.now(clock), ejecutadoPor);
            cierreRepository.guardar(resultado);
            LOG.info("Cierre " + periodo + " terminado en " + duracionMs + " ms");
            return resultado;
        } finally {
            cierreEnCurso.unlock();       // siempre se libera, incluso si hubo excepcion
        }
    }

    private int recolectar(List<Future<Integer>> futuros, List<Future<Integer>> todas,
                           List<String> errores, String etiqueta) throws CierreMensualException {
        int total = 0;
        for (Future<Integer> futuro : futuros) {
            try {
                total += futuro.get(timeoutSegundos, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                Throwable causa = e.getCause() != null ? e.getCause() : e;
                errores.add(etiqueta + ": " + causa.getMessage());
            } catch (TimeoutException e) {
                futuro.cancel(true);
                errores.add(etiqueta + ": tiempo de espera agotado (" + timeoutSegundos + " s)");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();          // se restaura la bandera de interrupcion
                todas.forEach(f -> f.cancel(true));
                throw new CierreMensualException("Cierre interrumpido", e);
            }
        }
        return total;
    }

    @Override
    public Optional<ResultadoCierre> ultimoCierre() { return cierreRepository.ultimo(); }

    /** Apaga el pool de hilos de forma ordenada. */
    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
