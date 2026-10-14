package com.sv.grupo11.asociacioncomunal.controller;

import com.sv.grupo11.asociacioncomunal.dto.SocioRequest;
import com.sv.grupo11.asociacioncomunal.model.Permiso;
import com.sv.grupo11.asociacioncomunal.model.Socio;
import com.sv.grupo11.asociacioncomunal.service.SocioService;
import com.sv.grupo11.asociacioncomunal.util.SesionUsuario;
import java.util.List;

public class SocioController {
    private final SocioService socioService;
    private final SesionUsuario sesion;

    public SocioController(SocioService socioService, SesionUsuario sesion) {
        this.socioService = socioService;
        this.sesion = sesion;
    }

    public List<Socio> buscar(String texto) {
        sesion.exigir(Permiso.VER_SOCIOS);
        return socioService.buscar(texto);
    }

    public Socio registrar(SocioRequest solicitud) {
        sesion.exigir(Permiso.EDITAR_SOCIOS);
        return socioService.registrar(solicitud);
    }

    public Socio actualizarContacto(long idSocio, String telefono, String direccion) {
        sesion.exigir(Permiso.EDITAR_SOCIOS);
        return socioService.actualizarContacto(idSocio, telefono, direccion);
    }

    public Socio cambiarEstado(long idSocio) {
        sesion.exigir(Permiso.CAMBIAR_ESTADO_SOCIO);
        return socioService.cambiarEstado(idSocio);
    }
}
