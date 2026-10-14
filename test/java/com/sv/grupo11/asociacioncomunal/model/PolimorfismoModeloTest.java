package com.sv.grupo11.asociacioncomunal.model;

import static org.junit.jupiter.api.Assertions.*;

import com.sv.grupo11.asociacioncomunal.TestData;
import java.math.BigDecimal;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class PolimorfismoModeloTest {

    @Test
    void socioComunPagaLaCuotaBase() {
        Cobrable c = TestData.socio(1, "00000001-1");
        assertEquals(0, new BigDecimal("5.00").compareTo(c.calcularCuotaMensual()));
    }

    @Test
    void miembroDeDirectivaPagaCuotaBaseMasAporte() {
        Cobrable c = TestData.directiva(2, "00000002-2");
        assertEquals(0, new BigDecimal("7.50").compareTo(c.calcularCuotaMensual()));
    }

    @Test
    void descripcionCortaEsPolimorfica() {
        Persona socio = TestData.socio(1, "00000001-1");
        Persona directivo = TestData.directiva(2, "00000002-2");
        assertTrue(socio.descripcionCorta().startsWith("Socio #1"));
        assertTrue(directivo.descripcionCorta().startsWith("Presidente #2"));
    }

    @Test
    void cuotaGeneradaUsaElMontoSegunElTipoDeSocio() {
        Cuota c = Cuota.generarPara(TestData.directiva(2, "00000002-2"), YearMonth.of(2026, 10));
        assertEquals(0, new BigDecimal("7.50").compareTo(c.getMonto()));
        assertEquals(java.time.LocalDate.of(2026, 10, 31), c.getFechaVencimiento());
        assertEquals(EstadoCuota.PENDIENTE, c.getEstado());
    }

    @Test
    void permisosPorRol() {
        assertTrue(Rol.ADMINISTRADOR.tiene(Permiso.GESTIONAR_USUARIOS));
        assertTrue(Rol.TESORERO.tiene(Permiso.EJECUTAR_CIERRE));
        assertFalse(Rol.TESORERO.tiene(Permiso.EDITAR_SOCIOS));
        assertTrue(Rol.SECRETARIO.tiene(Permiso.EDITAR_SOCIOS));
        assertFalse(Rol.SECRETARIO.tiene(Permiso.VER_FINANZAS));
        assertFalse(Rol.SOCIO.tiene(Permiso.VER_SOCIOS));
    }
}
