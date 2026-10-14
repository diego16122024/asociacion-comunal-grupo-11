package com.sv.grupo11.asociacioncomunal.repository;

import com.sv.grupo11.asociacioncomunal.model.Socio;

public interface SocioRepository extends CrudRepository<Socio, Long> {
    boolean existePorDui(String dui);
    /** Actualiza telefono, direccion y estado de un socio existente. */
    void actualizar(Socio socio);
}
