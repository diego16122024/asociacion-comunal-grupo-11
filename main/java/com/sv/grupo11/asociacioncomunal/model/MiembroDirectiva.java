package com.sv.grupo11.asociacioncomunal.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Especializacion de Socio: paga un aporte adicional por pertenecer a la junta directiva. */
public class MiembroDirectiva extends Socio {
    public static final BigDecimal APORTE_DIRECTIVA = new BigDecimal("2.50");

    private final String cargo;
    private final String periodoDirectiva;

    public MiembroDirectiva(Long id, String nombre, String dui, String telefono, String direccion,
                            LocalDate fechaIngreso, EstadoSocio estado, String cargo, String periodoDirectiva) {
        super(id, nombre, dui, telefono, direccion, fechaIngreso, estado);
        this.cargo = cargo;
        this.periodoDirectiva = periodoDirectiva;
    }

    @Override
    public BigDecimal calcularCuotaMensual() { return super.calcularCuotaMensual().add(APORTE_DIRECTIVA); }

    @Override
    public String descripcionCorta() { return cargo + " #" + getId() + " - " + getNombre(); }

    @Override public String getTipo() { return "DIRECTIVA"; }
    @Override public String getCargo() { return cargo; }
    @Override public String getPeriodoDirectiva() { return periodoDirectiva; }
    @Override public boolean esDirectiva() { return true; }
}
