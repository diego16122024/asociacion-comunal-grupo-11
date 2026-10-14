package com.sv.grupo11.asociacioncomunal.model;

import java.util.EnumSet;
import java.util.Set;

import static com.sv.grupo11.asociacioncomunal.model.Permiso.*;

public enum Rol {
    ADMINISTRADOR("Administrador", EnumSet.allOf(Permiso.class)),
    SECRETARIO("Secretario", EnumSet.of(VER_SOCIOS, EDITAR_SOCIOS)),
    TESORERO("Tesorero", EnumSet.of(VER_SOCIOS, VER_FINANZAS, REGISTRAR_PAGOS, VER_MOROSOS, EJECUTAR_CIERRE)),
    SOCIO("Socio", EnumSet.noneOf(Permiso.class));

    private final String etiqueta;
    private final Set<Permiso> permisos;

    Rol(String etiqueta, Set<Permiso> permisos) {
        this.etiqueta = etiqueta;
        this.permisos = permisos;
    }

    public String getEtiqueta() { return etiqueta; }
    public boolean tiene(Permiso permiso) { return permisos.contains(permiso); }
}
