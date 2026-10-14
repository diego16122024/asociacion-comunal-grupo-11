package com.sv.grupo11.asociacioncomunal.service;

import com.sv.grupo11.asociacioncomunal.dto.SocioRequest;
import com.sv.grupo11.asociacioncomunal.model.Socio;
import java.util.List;

public interface SocioService {
    Socio registrar(SocioRequest solicitud);
    Socio actualizarContacto(long idSocio, String telefono, String direccion);
    Socio cambiarEstado(long idSocio);
    Socio obtener(long idSocio);
    List<Socio> listar();
    List<Socio> buscar(String texto);
}
