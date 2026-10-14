package com.sv.grupo11.asociacioncomunal.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sv.grupo11.asociacioncomunal.TestData;
import com.sv.grupo11.asociacioncomunal.dto.SocioRequest;
import com.sv.grupo11.asociacioncomunal.exception.*;
import com.sv.grupo11.asociacioncomunal.model.EstadoSocio;
import com.sv.grupo11.asociacioncomunal.model.MiembroDirectiva;
import com.sv.grupo11.asociacioncomunal.model.Socio;
import com.sv.grupo11.asociacioncomunal.repository.SocioRepository;
import com.sv.grupo11.asociacioncomunal.service.impl.SocioServiceImpl;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SocioServiceImplTest {
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-12T12:00:00Z"), ZoneOffset.UTC);

    @Mock private SocioRepository repository;
    private SocioServiceImpl service;

    @BeforeEach
    void preparar() {
        service = new SocioServiceImpl(repository, RELOJ);
    }

    /** Simula la BD: asigna el id autogenerado al guardar. */
    private void guardarAsignaId() {
        when(repository.guardar(any())).thenAnswer(inv -> {
            Socio s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });
    }

    private SocioRequest solicitud(String dui, String cargo, String periodo) {
        return new SocioRequest("Maria Perez", dui, "7412-3698", "Colonia Las Flores", null, cargo, periodo);
    }

    @Test @DisplayName("Registrar socio valido lo guarda activo, con fecha de hoy y cuota base")
    void registrarSocioExitoso() {
        guardarAsignaId();
        when(repository.existePorDui("04512378-9")).thenReturn(false);

        Socio s = service.registrar(solicitud("04512378-9", null, null));

        assertEquals(10L, s.getId());
        assertEquals(EstadoSocio.ACTIVO, s.getEstado());
        assertEquals(LocalDate.of(2026, 10, 12), s.getFechaIngreso());
        assertFalse(s.esDirectiva());
        assertEquals(0, new java.math.BigDecimal("5.00").compareTo(s.calcularCuotaMensual()));
        verify(repository).guardar(any());
    }

    @Test @DisplayName("Registrar miembro de directiva crea MiembroDirectiva con aporte extra")
    void registrarDirectiva() {
        guardarAsignaId();
        Socio s = service.registrar(solicitud("04512378-9", "Presidente", "2026-2028"));

        assertTrue(s instanceof MiembroDirectiva);
        assertEquals("Presidente", s.getCargo());
        assertEquals(0, new java.math.BigDecimal("7.50").compareTo(s.calcularCuotaMensual()));
    }

    @Test @DisplayName("Directiva sin periodo es un error de validacion y no se guarda")
    void directivaSinPeriodo() {
        assertThrows(ValidacionException.class, () -> service.registrar(solicitud("04512378-9", "Vocal", " ")));
        verify(repository, never()).guardar(any());
    }

    @Test @DisplayName("DUI con formato invalido se rechaza antes de tocar la BD")
    void duiInvalido() {
        assertThrows(ValidacionException.class, () -> service.registrar(solicitud("123", null, null)));
        verify(repository, never()).existePorDui(any());
        verify(repository, never()).guardar(any());
    }

    @Test @DisplayName("Telefono invalido se rechaza")
    void telefonoInvalido() {
        SocioRequest r = new SocioRequest("Ana", "04512378-9", "12345", "x", null, null, null);
        assertThrows(ValidacionException.class, () -> service.registrar(r));
    }

    @Test @DisplayName("DUI repetido es un error de negocio")
    void duiDuplicado() {
        when(repository.existePorDui("04512378-9")).thenReturn(true);

        ReglaNegocioException e = assertThrows(ReglaNegocioException.class,
                () -> service.registrar(solicitud("04512378-9", null, null)));

        assertTrue(e.getMessage().contains("04512378-9"));
        verify(repository, never()).guardar(any());
    }

    @Test @DisplayName("Si la BD detecta el DUI repetido (carrera), se traduce a error de negocio")
    void duiDuplicadoPorCarrera() {
        when(repository.existePorDui(any())).thenReturn(false);
        when(repository.guardar(any())).thenThrow(new DuplicateKeyException("uq_socio_dui", null));

        assertThrows(ReglaNegocioException.class, () -> service.registrar(solicitud("04512378-9", null, null)));
    }

    @Test @DisplayName("Actualizar contacto cambia telefono y direccion y persiste")
    void actualizarContacto() {
        Socio existente = TestData.socio(3, "03214567-8");
        when(repository.buscarPorId(3L)).thenReturn(Optional.of(existente));

        Socio s = service.actualizarContacto(3, "7555-0000", "Pasaje 9");

        assertEquals("7555-0000", s.getTelefono());
        assertEquals("Pasaje 9", s.getDireccion());
        verify(repository).actualizar(existente);
    }

    @Test @DisplayName("Actualizar contacto con telefono invalido no persiste")
    void actualizarContactoInvalido() {
        when(repository.buscarPorId(3L)).thenReturn(Optional.of(TestData.socio(3, "03214567-8")));
        assertThrows(ValidacionException.class, () -> service.actualizarContacto(3, "abc", ""));
        verify(repository, never()).actualizar(any());
    }

    @Test @DisplayName("Socio inexistente lanza RecursoNoEncontradoException")
    void socioNoExiste() {
        when(repository.buscarPorId(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.obtener(99));
        assertThrows(RecursoNoEncontradoException.class, () -> service.cambiarEstado(99));
    }

    @Test @DisplayName("Cambiar estado alterna ACTIVO <-> INACTIVO")
    void cambiarEstado() {
        Socio s = TestData.socio(4, "02345678-1");
        when(repository.buscarPorId(4L)).thenReturn(Optional.of(s));

        assertEquals(EstadoSocio.INACTIVO, service.cambiarEstado(4).getEstado());
        assertEquals(EstadoSocio.ACTIVO, service.cambiarEstado(4).getEstado());
        verify(repository, times(2)).actualizar(s);
    }

    @Test @DisplayName("Buscar filtra por nombre o DUI sin distinguir mayusculas")
    void buscar() {
        when(repository.listarTodos()).thenReturn(List.of(TestData.socio(1, "11111111-1"), TestData.socio(2, "22222222-2")));

        assertEquals(1, service.buscar("SOCIO 2").size());
        assertEquals(1, service.buscar("11111111").size());
        assertEquals(2, service.buscar("  ").size());
        assertTrue(service.buscar("zzz").isEmpty());
    }
}
