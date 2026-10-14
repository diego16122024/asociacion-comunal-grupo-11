package com.sv.grupo11.asociacioncomunal.dto;

import com.sv.grupo11.asociacioncomunal.model.Cuota;
import com.sv.grupo11.asociacioncomunal.model.Socio;
import java.math.BigDecimal;
import java.util.List;

/** Vista de "Mi cuenta": datos del socio, su historial y el total que adeuda. */
public record CuentaSocio(Socio socio, List<Cuota> pendientes, List<Cuota> historial, BigDecimal totalPendiente) { }
