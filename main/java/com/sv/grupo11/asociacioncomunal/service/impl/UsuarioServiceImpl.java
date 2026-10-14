package com.sv.grupo11.asociacioncomunal.service.impl;

import com.sv.grupo11.asociacioncomunal.dto.NuevoUsuarioRequest;
import com.sv.grupo11.asociacioncomunal.exception.DuplicateKeyException;
import com.sv.grupo11.asociacioncomunal.exception.RecursoNoEncontradoException;
import com.sv.grupo11.asociacioncomunal.exception.ReglaNegocioException;
import com.sv.grupo11.asociacioncomunal.exception.ValidacionException;
import com.sv.grupo11.asociacioncomunal.model.Rol;
import com.sv.grupo11.asociacioncomunal.model.Usuario;
import com.sv.grupo11.asociacioncomunal.repository.SocioRepository;
import com.sv.grupo11.asociacioncomunal.repository.UsuarioRepository;
import com.sv.grupo11.asociacioncomunal.service.UsuarioService;
import com.sv.grupo11.asociacioncomunal.util.PasswordHasher;
import com.sv.grupo11.asociacioncomunal.util.ValidadorPersona;
import java.util.List;

public class UsuarioServiceImpl implements UsuarioService {
    static final int LONGITUD_MINIMA_CLAVE = 6;

    private final UsuarioRepository usuarioRepository;
    private final SocioRepository socioRepository;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, SocioRepository socioRepository) {
        this.usuarioRepository = usuarioRepository;
        this.socioRepository = socioRepository;
    }

    @Override
    public Usuario crear(NuevoUsuarioRequest s) {
        String nombre = ValidadorPersona.requerirTexto(s.nombreUsuario(), "usuario");
        if (s.clave() == null || s.clave().length() < LONGITUD_MINIMA_CLAVE) {
            throw new ValidacionException("La contrasena debe tener al menos " + LONGITUD_MINIMA_CLAVE + " caracteres.");
        }
        if (s.rol() == null) throw new ValidacionException("El rol es obligatorio.");
        if (usuarioRepository.existePorNombre(nombre)) {
            throw new ReglaNegocioException("Ya existe el usuario " + nombre);
        }
        Long idSocio = null;
        if (s.rol() == Rol.SOCIO) {
            if (s.idSocio() == null) throw new ValidacionException("Un usuario de rol Socio debe asociarse a un socio.");
            socioRepository.buscarPorId(s.idSocio())
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe el socio #" + s.idSocio()));
            idSocio = s.idSocio();
        }
        try {
            return usuarioRepository.guardar(new Usuario(null, nombre, PasswordHasher.hash(s.clave()), s.rol(), idSocio));
        } catch (DuplicateKeyException e) {
            throw new ReglaNegocioException("Ya existe el usuario " + nombre, e);
        }
    }

    @Override
    public List<Usuario> listar() { return usuarioRepository.listarTodos(); }
}
