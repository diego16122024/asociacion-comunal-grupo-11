package com.sv.grupo11.asociacioncomunal.controller;

import com.sv.grupo11.asociacioncomunal.exception.CredencialesInvalidasException;
import com.sv.grupo11.asociacioncomunal.model.Usuario;
import com.sv.grupo11.asociacioncomunal.service.AuthService;
import com.sv.grupo11.asociacioncomunal.util.SesionUsuario;
import java.util.Optional;

public class AuthController {
    private final AuthService authService;
    private final SesionUsuario sesion;

    public AuthController(AuthService authService, SesionUsuario sesion) {
        this.authService = authService;
        this.sesion = sesion;
    }

    public Usuario iniciarSesion(String usuario, String clave) throws CredencialesInvalidasException {
        Usuario u = authService.autenticar(usuario, clave);
        sesion.iniciar(u);
        return u;
    }

    public void cerrarSesion() { sesion.cerrar(); }
    public Optional<Usuario> usuarioActual() { return sesion.usuario(); }
}
