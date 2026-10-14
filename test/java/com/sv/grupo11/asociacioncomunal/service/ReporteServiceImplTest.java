package com.sv.grupo11.asociacioncomunal.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sv.grupo11.asociacioncomunal.TestData;
import com.sv.grupo11.asociacioncomunal.dto.MorosoDTO;
import com.sv.grupo11.asociacioncomunal.dto.ResumenPanel;
import com.sv.grupo11.asociacioncomunal.model.EstadoCuota;
import com.sv.grupo11.asociacioncomunal.repository.CierreRepository;
import com.sv.grupo11.asociacioncomunal.repository.CuotaRepository;
import com.sv.grupo11.asociacioncomunal.repository.CuotaRepository.TotalPorEstado;
import com.sv.grupo11.asociacioncomunal.repository.SocioRepository;
import com.sv.grupo11.asociacioncomunal.service.impl.ReporteServiceImpl;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
class ReporteServiceImplTest {
    @Mock private SocioRepository socioRepository;
    @Mock private CuotaRepository cuotaRepository;
    @Mock private CierreRepository cierreRepository;
    private ReporteServiceImpl service;

    @BeforeEach
    void preparar() { service = new ReporteServiceImpl(socioRepository, cuotaRepository, cierreRepository); }

    @Test
    void morososSeAgrupanPorSocioYSeOrdenanPorMontoDescendente() {
        when(socioRepository.listarTodos()).thenReturn(List.of(
                TestData.socio(1, "11111111-1"), TestData.directiva(2, "22222222-2"), TestData.socio(3, "33333333-3")));
        when(cuotaRepository.buscarPorEstado(EstadoCuota.VENCIDA)).thenReturn(List.of(
                TestData.cuota(1, 1, "5.00", "2026-07", EstadoCuota.VENCIDA),
                TestData.cuota(2, 2, "7.50", "2026-07", EstadoCuota.VENCIDA),
                TestData.cuota(3, 2, "7.50", "2026-08", EstadoCuota.VENCIDA)));

        List<MorosoDTO> morosos = service.morosos();

        assertEquals(2, morosos.size());                       // el socio 3 no debe aparecer
        assertEquals(2L, morosos.get(0).socio().getId());      // 15.00 > 5.00
        assertEquals(2, morosos.get(0).cuotasVencidas());
        assertEquals(0, new BigDecimal("15.00").compareTo(morosos.get(0).montoAdeudado()));
        assertEquals(1L, morosos.get(1).socio().getId());
    }

    @Test
    void sinCuotasVencidasNoHayMorosos() {
        when(socioRepository.listarTodos()).thenReturn(List.of(TestData.socio(1, "11111111-1")));
        when(cuotaRepository.buscarPorEstado(EstadoCuota.VENCIDA)).thenReturn(List.of());
        assertTrue(service.morosos().isEmpty());
    }

    @Test
    void resumenCombinaSociosTotalesYUltimoCierre() {
        when(socioRepository.listarTodos()).thenReturn(List.of(
                TestData.socio(1, "11111111-1"), TestData.socioInactivo(2, "22222222-2"), TestData.directiva(3, "33333333-3")));
        Map<EstadoCuota, TotalPorEstado> t = new EnumMap<>(EstadoCuota.class);
        t.put(EstadoCuota.PAGADA, new TotalPorEstado(4, new BigDecimal("27.50")));
        t.put(EstadoCuota.PENDIENTE, new TotalPorEstado(2, new BigDecimal("10.00")));
        t.put(EstadoCuota.VENCIDA, new TotalPorEstado(1, new BigDecimal("5.00")));
        when(cuotaRepository.totalesPorEstado()).thenReturn(t);
        when(cuotaRepository.ultimosPagos(6)).thenReturn(List.of());
        when(cierreRepository.ultimo()).thenReturn(Optional.empty());

        ResumenPanel r = service.resumen();

        assertEquals(2, r.sociosActivos());
        assertEquals(3, r.sociosRegistrados());
        assertEquals(1, r.miembrosDirectiva());
        assertEquals(0, new BigDecimal("27.50").compareTo(r.recaudado()));
        assertEquals(1, r.cuotasVencidas());
        assertNull(r.ultimoCierre());
    }
}
