package com.sv.grupo11.asociacioncomunal.util;

import static org.junit.jupiter.api.Assertions.*;

import com.sv.grupo11.asociacioncomunal.exception.ValidacionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorPersonaTest {

    @Test @DisplayName("DUI valido se devuelve sin espacios")
    void duiValido() {
        assertEquals("04512378-9", ValidadorPersona.validarDui("  04512378-9 "));
    }

    @Test @DisplayName("DUI con formato incorrecto lanza ValidacionException")
    void duiInvalido() {
        for (String malo : new String[]{"045123789", "0451237-89", "ABCDEFGH-1", "04512378-91"}) {
            assertThrows(ValidacionException.class, () -> ValidadorPersona.validarDui(malo), malo);
        }
    }

    @Test @DisplayName("DUI nulo o vacio es obligatorio")
    void duiVacio() {
        assertThrows(ValidacionException.class, () -> ValidadorPersona.validarDui(null));
        assertThrows(ValidacionException.class, () -> ValidadorPersona.validarDui("   "));
    }

    @Test @DisplayName("Telefono valido e invalido")
    void telefono() {
        assertEquals("7412-3698", ValidadorPersona.validarTelefono("7412-3698"));
        ValidacionException e = assertThrows(ValidacionException.class, () -> ValidadorPersona.validarTelefono("74123698"));
        assertTrue(e.getMessage().contains("####-####"));
    }

    @Test @DisplayName("requerirTexto recorta y rechaza vacios")
    void requerirTexto() {
        assertEquals("Ana", ValidadorPersona.requerirTexto("  Ana ", "nombre"));
        assertThrows(ValidacionException.class, () -> ValidadorPersona.requerirTexto("", "nombre"));
    }
}
