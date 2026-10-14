package com.sv.grupo11.asociacioncomunal.dto;

import com.sv.grupo11.asociacioncomunal.model.Cuota;
import java.math.BigDecimal;
import java.util.List;

public record ResumenPanel(long sociosActivos, long sociosRegistrados, long miembrosDirectiva,
                           BigDecimal recaudado, long cuotasPagadas,
                           BigDecimal porCobrar, long cuotasPendientes,
                           BigDecimal enMora, long cuotasVencidas,
                           List<Cuota> ultimosPagos, ResultadoCierre ultimoCierre) { }
