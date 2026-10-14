package com.sv.grupo11.asociacioncomunal.dto;

import java.time.LocalDate;
import java.time.YearMonth;

/** Resultado (y registro historico) de un cierre mensual. */
public record ResultadoCierre(YearMonth periodo, int cuotasGeneradas, int cuotasVencidas, long duracionMs,
                              LocalDate fechaCorte, LocalDate fechaEjecucion, String ejecutadoPor) { }
