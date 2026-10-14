package com.sv.grupo11.asociacioncomunal.util;

import com.sv.grupo11.asociacioncomunal.exception.AccesoDenegadoException;
import com.sv.grupo11.asociacioncomunal.model.Permiso;
import com.sv.grupo11.asociacioncomunal.model.Usuario;
import java.util.Optional;

/** Usuario autenticado actual. Los controladores consultan aqui los permisos antes de actuar. */
public class SesionUsuario {
    private volatile Usuario actual;

    public void iniciar(Usuario usuario) { this.actual = usuario; }
    public void cerrar() { this.actual = null; }
    public Optional<Usuario> usuario() { return Optional.ofNullable(actual); }

    public Usuario requerirUsuario() {
        Usuario u = actual;
        if (u == null) throw new AccesoDenegadoException("Debe iniciar sesion.");
        return u;
    }

    public Usuario exigir(Permiso permiso) {
        Usuario u = requerirUsuario();
        if (!u.puede(permiso)) {
            throw new AccesoDenegadoException("El rol " + u.getRol().getEtiqueta() + " no tiene permiso para esta accion.");
        }
        return u;
    }
}
