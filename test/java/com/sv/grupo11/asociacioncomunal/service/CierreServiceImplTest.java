package com.sv.grupo11.asociacioncomunal.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sv.grupo11.asociacioncomunal.TestData;
import com.sv.grupo11.asociacioncomunal.dto.ResultadoCierre;
import com.sv.grupo11.asociacioncomunal.exception.CierreMensualException;
import com.sv.grupo11.asociacioncomunal.exception.OperacionEnCursoException;
import com.sv.grupo11.asociacioncomunal.exception.RepositoryException;
import com.sv.grupo11.asociacioncomunal.exception.ValidacionException;
import com.sv.grupo11.asociacioncomunal.model.Cuota;
import com.sv.grupo11.asociacioncomunal.repository.CierreRepository;
import com.sv.grupo11.asociacioncomunal.repository.CuotaRepository;
import com.sv.grupo11.asociacioncomunal.repository.SocioRepository;
import com.sv.grupo11.asociacioncomunal.service.cierre.CierreServiceImpl;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Pruebas del cierre mensual: ExecutorService real, repositorios simulados con Mockito. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CierreServiceImplTest {
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-12T12:00:00Z"), ZoneOffset.UTC);
    private static final YearMonth PERIODO = YearMonth.of(2026, 10);
    private static final LocalDate CORTE = LocalDate.of(2026, 10, 12);

    @Mock private SocioRepository socioRepository;
    @Mock private CuotaRepository cuotaRepository;
    @Mock private CierreRepository cierreRepository;
    private ExecutorService pool;
    private CierreServiceImpl service;

    @BeforeEach
    void preparar() {
        pool = Executors.newFixedThreadPool(4);
        service = new CierreServiceImpl(socioRepository, cuotaRepository, cierreRepository, pool, 2, 10, RELOJ);
        // 5 socios: 4 activos y 1 inactivo (no debe recibir cuota) -> 2 lotes de 2 socios
        when(socioRepository.listarTodos()).thenReturn(List.of(
                TestData.socio(1, "00000001-1"), TestData.directiva(2, "00000002-2"), TestData.socio(3, "00000003-3"),
                TestData.socioInactivo(4, "00000004-4"), TestData.socio(5, "00000005-5")));
        when(cuotaRepository.insertarLote(any())).thenAnswer(inv -> ((List<Cuota>) inv.getArgument(0)).size());
        when(cuotaRepository.marcarVencidas(any(), any())).thenReturn(3);
    }

    @AfterEach
    void limpiar() { pool.shutdownNow(); }

    @Test
    @SuppressWarnings("unchecked")
    void cierreExitosoSumaLosResultadosDeLasTareasConcurrentes() throws CierreMensualException {
        ResultadoCierre r = service.ejecutarCierre(PERIODO, CORTE, "tesorero");

        assertEquals(4, r.cuotasGeneradas());      // solo socios activos
        assertEquals(3, r.cuotasVencidas());
        assertEquals("tesorero", r.ejecutadoPor());
        assertEquals(PERIODO, r.periodo());
        assertEquals(LocalDate.of(2026, 10, 12), r.fechaEjecucion());
        verify(cuotaRepository, times(2)).insertarLote(any());          // 2 lotes concurrentes
        verify(cuotaRepository, times(1)).marcarVencidas(CORTE, PERIODO);
        verify(cierreRepository).guardar(r);
    }

    @Test
    void siUnaTareaFallaSeLanzaCierreMensualExceptionYNoSeRegistra() {
        doThrow(new RepositoryException("BD caida", null)).when(cuotaRepository).insertarLote(any());

        CierreMensualException e = assertThrows(CierreMensualException.class,
                () -> service.ejecutarCierre(PERIODO, CORTE, "admin"));

        assertFalse(e.getErrores().isEmpty());
        assertTrue(e.getMessage().contains("BD caida"));
        verify(cierreRepository, never()).guardar(any());
        verify(cuotaRepository).marcarVencidas(CORTE, PERIODO);       // la otra tarea si se ejecuto
    }

    @Test
    void periodoNuloEsInvalido() {
        assertThrows(ValidacionException.class, () -> service.ejecutarCierre(null, CORTE, "admin"));
    }

    @Test
    void noSePermiteUnSegundoCierreMientrasOtroEstaEnCurso() throws Exception {
        CountDownLatch iniciado = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        when(cuotaRepository.marcarVencidas(any(), any())).thenAnswer(inv -> {
            iniciado.countDown();
            liberar.await(5, TimeUnit.SECONDS);       // bloquea el primer cierre a mitad de camino
            return 0;
        });
        ExecutorService otroHilo = Executors.newSingleThreadExecutor();
        try {
            Future<ResultadoCierre> primero = otroHilo.submit(() -> service.ejecutarCierre(PERIODO, CORTE, "admin"));
            assertTrue(iniciado.await(5, TimeUnit.SECONDS));

            assertThrows(OperacionEnCursoException.class, () -> service.ejecutarCierre(PERIODO, CORTE, "tesorero"));

            liberar.countDown();
            assertEquals(4, primero.get(5, TimeUnit.SECONDS).cuotasGeneradas());
        } finally {
            liberar.countDown();
            otroHilo.shutdownNow();
        }
    }

    @Test
    void despuesDeTerminarSePuedeEjecutarOtroCierre() throws CierreMensualException {
        service.ejecutarCierre(PERIODO, CORTE, "admin");
        assertNotNull(service.ejecutarCierre(PERIODO, CORTE, "admin"));   // el lock se libero (finally)
    }

    @Test
    void conPoolApagadoElCierreFallaConExcepcionControlada() {
        pool.shutdownNow();
        assertThrows(CierreMensualException.class, () -> service.ejecutarCierre(PERIODO, CORTE, "admin"));
        // y el lock quedo liberado
        assertThrows(CierreMensualException.class, () -> service.ejecutarCierre(PERIODO, CORTE, "admin"));
    }
}
