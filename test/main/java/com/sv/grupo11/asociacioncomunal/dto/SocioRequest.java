package com.sv.grupo11.asociacioncomunal.dto;

import java.time.LocalDate;

/** Datos de entrada para registrar un socio o miembro de la directiva. */
public record SocioRequest(String nombre, String dui, String telefono, String direccion,
                           LocalDate fechaIngreso, String cargo, String periodoDirectiva) {
    public boolean esDirectiva() { return cargo != null && !cargo.isBlank(); }
}
