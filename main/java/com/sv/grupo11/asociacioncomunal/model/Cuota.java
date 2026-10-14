package com.sv.grupo11.asociacioncomunal.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public class Cuota {
    private Long id;
    private final long idSocio;
    private final BigDecimal monto;
    private final YearMonth periodo;
    private final LocalDate fechaVencimiento;
    private LocalDate fechaPago;
    private EstadoCuota estado;

    public Cuota(Long id, long idSocio, BigDecimal monto, YearMonth periodo,
                 LocalDate fechaVencimiento, LocalDate fechaPago, EstadoCuota estado) {
        this.id = id;
        this.idSocio = idSocio;
        this.monto = monto;
        this.periodo = periodo;
        this.fechaVencimiento = fechaVencimiento;
        this.fechaPago = fechaPago;
        this.estado = estado;
    }

    /** Fabrica: una cuota nueva se calcula polimorficamente segun el tipo de socio. */
    public static Cuota generarPara(Socio socio, YearMonth periodo) {
        return new Cuota(null, socio.getId(), socio.calcularCuotaMensual(), periodo,
                periodo.atEndOfMonth(), null, EstadoCuota.PENDIENTE);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getIdSocio() { return idSocio; }
    public BigDecimal getMonto() { return monto; }
    public YearMonth getPeriodo() { return periodo; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public LocalDate getFechaPago() { return fechaPago; }
    public EstadoCuota getEstado() { return estado; }
    public boolean estaPagada() { return estado == EstadoCuota.PAGADA; }
}
