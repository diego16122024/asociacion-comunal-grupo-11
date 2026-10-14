package com.sv.grupo11.asociacioncomunal.repository;

import com.sv.grupo11.asociacioncomunal.model.Usuario;
import java.util.Optional;

public interface UsuarioRepository extends CrudRepository<Usuario, Long> {
    Optional<Usuario> buscarPorNombre(String nombreUsuario);
    boolean existePorNombre(String nombreUsuario);
}
