package com.sv.grupo11.asociacioncomunal.dto;

import com.sv.grupo11.asociacioncomunal.model.Rol;

public record NuevoUsuarioRequest(String nombreUsuario, String clave, Rol rol, Long idSocio) { }
