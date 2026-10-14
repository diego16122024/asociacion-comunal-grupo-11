package com.sv.grupo11.asociacioncomunal.service.impl;

import com.sv.grupo11.asociacioncomunal.exception.CredencialesInvalidasException;
import com.sv.grupo11.asociacioncomunal.model.Usuario;
import com.sv.grupo11.asociacioncomunal.repository.UsuarioRepository;
import com.sv.grupo11.asociacioncomunal.service.AuthService;
import com.sv.grupo11.asociacioncomunal.util.PasswordHasher;

public class AuthServiceImpl implements AuthService {
    private final UsuarioRepository usuarioRepository;

    public AuthServiceImpl(UsuarioRepository usuarioRepository) { this.usuarioRepository = usuarioRepository; }

    @Override
    public Usuario autenticar(String nombreUsuario, String clave) throws CredencialesInvalidasException {
        if (nombreUsuario == null || nombreUsuario.isBlank() || clave == null) {
            throw new CredencialesInvalidasException("Usuario o contrasena incorrectos.");
        }
        return usuarioRepository.buscarPorNombre(nombreUsuario.trim())
                .filter(u -> PasswordHasher.verificar(clave, u.getClaveHash()))
                .orElseThrow(() -> new CredencialesInvalidasException("Usuario o contrasena incorrectos."));
    }
}
