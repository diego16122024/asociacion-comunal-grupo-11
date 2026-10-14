package com.sv.grupo11.asociacioncomunal.dto;

import com.sv.grupo11.asociacioncomunal.model.Socio;
import java.math.BigDecimal;

public record MorosoDTO(Socio socio, int cuotasVencidas, BigDecimal montoAdeudado) { }
