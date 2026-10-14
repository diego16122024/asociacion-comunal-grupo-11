package com.sv.grupo11.asociacioncomunal.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Socio extends Persona implements Cobrable {
    public static final BigDecimal CUOTA_BASE = new BigDecimal("5.00");

    private Long id;
    private LocalDate fechaIngreso;
    private EstadoSocio estado;

    public Socio(Long id, String nombre, String dui, String telefono, String direccion,
                 LocalDate fechaIngreso, EstadoSocio estado) {
        super(nombre, dui, telefono, direccion);
        this.id = id;
        this.fechaIngreso = fechaIngreso;
        this.estado = estado;
    }

    @Override
    public BigDecimal calcularCuotaMensual() { return CUOTA_BASE; }

    @Override
    public String descripcionCorta() { return "Socio #" + id + " - " + getNombre(); }

    /** Tipo persistido en la columna socio.tipo. */
    public String getTipo() { return "SOCIO"; }
    /** Cargo en la directiva; vacio para socios comunes. */
    public String getCargo() { return null; }
    public String getPeriodoDirectiva() { return null; }
    public boolean esDirectiva() { return false; }
    public boolean estaActivo() { return estado == EstadoSocio.ACTIVO; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getFechaIngreso() { return fechaIngreso; }
    public EstadoSocio getEstado() { return estado; }
    public void setEstado(EstadoSocio estado) { this.estado = estado; }
}
