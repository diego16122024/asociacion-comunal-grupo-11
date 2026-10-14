package com.sv.grupo11.asociacioncomunal.model;

import java.math.BigDecimal;

/** Contrato para todo objeto que genera una cuota mensual. */
public interface Cobrable {
    BigDecimal calcularCuotaMensual();
}
