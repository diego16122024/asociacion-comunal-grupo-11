package com.sv.grupo11.asociacioncomunal.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ListasTest {

    @Test
    void partirRepartePorLotes() {
        List<List<Integer>> lotes = Listas.partir(List.of(1, 2, 3, 4, 5), 2);
        assertEquals(3, lotes.size());
        assertEquals(List.of(1, 2), lotes.get(0));
        assertEquals(List.of(5), lotes.get(2));
    }

    @Test
    void listaVaciaNoGeneraLotes() {
        assertTrue(Listas.partir(List.<String>of(), 3).isEmpty());
    }

    @Test
    void tamanoInvalidoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> Listas.partir(List.of(1), 0));
    }
}
