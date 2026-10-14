package com.sv.grupo11.asociacioncomunal.util;

import static org.junit.jupiter.api.Assertions.*;

import com.sv.grupo11.asociacioncomunal.TestData;
import com.sv.grupo11.asociacioncomunal.exception.AccesoDenegadoException;
import com.sv.grupo11.asociacioncomunal.model.Permiso;
import com.sv.grupo11.asociacioncomunal.model.Rol;
import org.junit.jupiter.api.Test;

class SesionUsuarioTest {

    @Test
    void sinSesionSeDeniegaElAcceso() {
        assertThrows(AccesoDenegadoException.class, () -> new SesionUsuario().exigir(Permiso.VER_SOCIOS));
    }

    @Test
    void exigirDevuelveElUsuarioCuandoTienePermiso() {
        SesionUsuario s = new SesionUsuario();
        s.iniciar(TestData.usuario(Rol.TESORERO, null));
        assertEquals(Rol.TESORERO, s.exigir(Permiso.REGISTRAR_PAGOS).getRol());
    }

    @Test
    void rolSinPermisoEsRechazado() {
        SesionUsuario s = new SesionUsuario();
        s.iniciar(TestData.usuario(Rol.SECRETARIO, null));
        assertThrows(AccesoDenegadoException.class, () -> s.exigir(Permiso.REGISTRAR_PAGOS));
    }

    @Test
    void cerrarSesionLimpiaElUsuario() {
        SesionUsuario s = new SesionUsuario();
        s.iniciar(TestData.usuario(Rol.ADMINISTRADOR, null));
        s.cerrar();
        assertTrue(s.usuario().isEmpty());
    }
}
