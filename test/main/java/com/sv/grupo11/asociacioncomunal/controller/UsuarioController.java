package com.sv.grupo11.asociacioncomunal.controller;

import com.sv.grupo11.asociacioncomunal.dto.NuevoUsuarioRequest;
import com.sv.grupo11.asociacioncomunal.model.Permiso;
import com.sv.grupo11.asociacioncomunal.model.Usuario;
import com.sv.grupo11.asociacioncomunal.service.UsuarioService;
import com.sv.grupo11.asociacioncomunal.util.SesionUsuario;
import java.util.List;

public class UsuarioController {
    private final UsuarioService usuarioService;
    private final SesionUsuario sesion;

    public UsuarioController(UsuarioService usuarioService, SesionUsuario sesion) {
        this.usuarioService = usuarioService;
        this.sesion = sesion;
    }

    public Usuario crear(NuevoUsuarioRequest solicitud) {
        sesion.exigir(Permiso.GESTIONAR_USUARIOS);
        return usuarioService.crear(solicitud);
    }

    public List<Usuario> listar() {
        sesion.exigir(Permiso.GESTIONAR_USUARIOS);
        return usuarioService.listar();
    }
}
