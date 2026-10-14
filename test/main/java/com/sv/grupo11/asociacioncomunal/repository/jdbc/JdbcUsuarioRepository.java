package com.sv.grupo11.asociacioncomunal.repository.jdbc;

import com.sv.grupo11.asociacioncomunal.model.Rol;
import com.sv.grupo11.asociacioncomunal.model.Usuario;
import com.sv.grupo11.asociacioncomunal.repository.UsuarioRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

public class JdbcUsuarioRepository extends JdbcSupport implements UsuarioRepository {

    private static final String COLUMNAS = "id, nombre_usuario, clave_hash, rol, id_socio";

    public JdbcUsuarioRepository(DataSource dataSource) { super(dataSource); }

    @Override
    public Usuario guardar(Usuario u) {
        long id = insertar("INSERT INTO usuario (nombre_usuario, clave_hash, rol, id_socio) VALUES (?, ?, ?, ?)", ps -> {
            ps.setString(1, u.getNombreUsuario());
            ps.setString(2, u.getClaveHash());
            ps.setString(3, u.getRol().name());
            setLongNulo(ps, 4, u.getIdSocio());
        });
        u.setId(id);
        return u;
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return consultarUno("SELECT " + COLUMNAS + " FROM usuario WHERE id = ?", ps -> ps.setLong(1, id), this::mapear);
    }

    @Override
    public Optional<Usuario> buscarPorNombre(String nombre) {
        return consultarUno("SELECT " + COLUMNAS + " FROM usuario WHERE LOWER(nombre_usuario) = LOWER(?)",
                ps -> ps.setString(1, nombre), this::mapear);
    }

    @Override
    public boolean existePorNombre(String nombre) { return buscarPorNombre(nombre).isPresent(); }

    @Override
    public List<Usuario> listarTodos() {
        return consultar("SELECT " + COLUMNAS + " FROM usuario ORDER BY id", SIN_PARAMETROS, this::mapear);
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        return new Usuario(rs.getLong("id"), rs.getString("nombre_usuario"), rs.getString("clave_hash"),
                Rol.valueOf(rs.getString("rol")), getLongNulo(rs, "id_socio"));
    }
}
