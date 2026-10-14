package com.sv.grupo11.asociacioncomunal.service;

import com.sv.grupo11.asociacioncomunal.exception.CredencialesInvalidasException;
import com.sv.grupo11.asociacioncomunal.model.Usuario;

public interface AuthService {
    Usuario autenticar(String nombreUsuario, String clave) throws CredencialesInvalidasException;
}
