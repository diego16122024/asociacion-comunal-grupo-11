package com.sv.grupo11.asociacioncomunal.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    @Test
    void hashVerificaConLaClaveCorrecta() {
        String hash = PasswordHasher.hash("secreta1");
        assertTrue(PasswordHasher.verificar("secreta1", hash));
    }

    @Test
    void claveIncorrectaNoVerifica() {
        assertFalse(PasswordHasher.verificar("otra", PasswordHasher.hash("secreta1")));
    }

    @Test
    void mismaClaveGeneraHashesDistintosPorLaSal() {
        assertNotEquals(PasswordHasher.hash("abc123"), PasswordHasher.hash("abc123"));
    }

    @Test
    void formatosInvalidosNoVerificanYNoLanzan() {
        assertFalse(PasswordHasher.verificar("x", "no-es-un-hash"));
        assertFalse(PasswordHasher.verificar("x", "pbkdf2$abc$!!$??"));
        assertFalse(PasswordHasher.verificar(null, PasswordHasher.hash("x")));
        assertFalse(PasswordHasher.verificar("x", null));
    }

    @Test
    void hashDeDatosDePruebaCoincideConAdmin123() {
        // mismo valor que database/data.sql para el usuario "admin"
        String delSql = "pbkdf2$65000$211NM0CDz/4V8zvAo0q78A==$JZ2fAv/R8skOOuJ2B3jfiNbIVJ1/qQO6ppZW9IraTQ8=";
        assertTrue(PasswordHasher.verificar("admin123", delSql));
    }
}
