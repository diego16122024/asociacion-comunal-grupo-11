package com.sv.grupo11.asociacioncomunal.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.sv.grupo11.asociacioncomunal.TestData;
import com.sv.grupo11.asociacioncomunal.dto.CuentaSocio;
import com.sv.grupo11.asociacioncomunal.exception.RecursoNoEncontradoException;
import com.sv.grupo11.asociacioncomunal.exception.ReglaNegocioException;
import com.sv.grupo11.asociacioncomunal.model.Cuota;
import com.sv.grupo11.asociacioncomunal.model.EstadoCuota;
import com.sv.grupo11.asociacioncomunal.repository.CuotaRepository;
import com.sv.grupo11.asociacioncomunal.repository.SocioRepository;
import com.sv.grupo11.asociacioncomunal.service.impl.CuotaServiceImpl;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CuotaServiceImplTest {
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-12T12:00:00Z"), ZoneOffset.UTC);

    @Mock private CuotaRepository cuotaRepository;
    @Mock private SocioRepository socioRepository;
    private CuotaServiceImpl service;

    @BeforeEach
    void preparar() { service = new CuotaServiceImpl(cuotaRepository, socioRepository, RELOJ); }

    @Test
    void registrarPagoMarcaLaCuotaYDevuelveLaActualizada() {
        Cuota pendiente = TestData.cuota(5, 1, "5.00", "2026-09", EstadoCuota.PENDIENTE);
        Cuota pagada = TestData.cuota(5, 1, "5.00", "2026-09", EstadoCuota.PAGADA);
        when(cuotaRepository.buscarPorId(5L)).thenReturn(Optional.of(pendiente), Optional.of(pagada));
        when(cuotaRepository.marcarPagada(5L, LocalDate.of(2026, 10, 1))).thenReturn(true);

        Cuota resultado = service.registrarPago(5, LocalDate.of(2026, 10, 1));

        assertTrue(resultado.estaPagada());
        verify(cuotaRepository).marcarPagada(5L, LocalDate.of(2026, 10, 1));
    }

    @Test
    void registrarPagoSinFechaUsaLaDeHoy() {
        when(cuotaRepository.buscarPorId(5L)).thenReturn(Optional.of(TestData.cuota(5, 1, "5.00", "2026-09", EstadoCuota.PENDIENTE)));
        when(cuotaRepository.marcarPagada(anyLong(), any())).thenReturn(true);

        service.registrarPago(5, null);

        verify(cuotaRepository).marcarPagada(5L, LocalDate.of(2026, 10, 12));
    }

    @Test
    void cuotaYaPagadaEsErrorDeNegocio() {
        when(cuotaRepository.buscarPorId(7L)).thenReturn(Optional.of(TestData.cuota(7, 1, "5.00", "2026-08", EstadoCuota.PAGADA)));

        ReglaNegocioException e = assertThrows(ReglaNegocioException.class, () -> service.registrarPago(7, null));

        assertTrue(e.getMessage().contains("ya fue pagada"));
        verify(cuotaRepository, never()).marcarPagada(anyLong(), any());
    }

    @Test
    void pagoConcurrenteDetectadoPorElUpdateAtomico() {
        // la lectura dice PENDIENTE pero otro usuario pago justo antes: el UPDATE afecta 0 filas
        when(cuotaRepository.buscarPorId(8L)).thenReturn(Optional.of(TestData.cuota(8, 1, "5.00", "2026-09", EstadoCuota.PENDIENTE)));
        when(cuotaRepository.marcarPagada(anyLong(), any())).thenReturn(false);

        assertThrows(ReglaNegocioException.class, () -> service.registrarPago(8, null));
    }

    @Test
    void cuotaInexistente() {
        when(cuotaRepository.buscarPorId(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.registrarPago(99, null));
    }

    @Test
    void cuentaDelSocioSumaSoloLoPendiente() {
        when(socioRepository.buscarPorId(1L)).thenReturn(Optional.of(TestData.socio(1, "11111111-1")));
        when(cuotaRepository.buscarPorSocio(1L)).thenReturn(List.of(
                TestData.cuota(1, 1, "5.00", "2026-10", EstadoCuota.PENDIENTE),
                TestData.cuota(2, 1, "5.00", "2026-09", EstadoCuota.VENCIDA),
                TestData.cuota(3, 1, "5.00", "2026-08", EstadoCuota.PAGADA)));

        CuentaSocio cuenta = service.cuentaDe(1);

        assertEquals(2, cuenta.pendientes().size());
        assertEquals(3, cuenta.historial().size());
        assertEquals(0, new java.math.BigDecimal("10.00").compareTo(cuenta.totalPendiente()));
    }

    @Test
    void cuotasDeSocioInexistente() {
        when(socioRepository.buscarPorId(50L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.cuotasDeSocio(50));
        verify(cuotaRepository, never()).buscarPorSocio(anyLong());
    }
}
