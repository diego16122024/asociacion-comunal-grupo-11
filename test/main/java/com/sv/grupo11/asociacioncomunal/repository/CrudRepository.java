package com.sv.grupo11.asociacioncomunal.repository;

import java.util.List;
import java.util.Optional;

/** Contrato generico de persistencia: T = entidad, ID = tipo de la llave primaria (genericos). */
public interface CrudRepository<T, ID> {
    T guardar(T entidad);
    Optional<T> buscarPorId(ID id);
    List<T> listarTodos();
}
