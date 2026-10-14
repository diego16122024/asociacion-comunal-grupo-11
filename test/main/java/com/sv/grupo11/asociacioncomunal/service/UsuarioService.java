package com.sv.grupo11.asociacioncomunal.service;

import com.sv.grupo11.asociacioncomunal.dto.NuevoUsuarioRequest;
import com.sv.grupo11.asociacioncomunal.model.Usuario;
import java.util.List;

public interface UsuarioService {
    Usuario crear(NuevoUsuarioRequest solicitud);
    List<Usuario> listar();
}
