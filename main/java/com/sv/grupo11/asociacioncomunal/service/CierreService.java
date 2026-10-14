package com.sv.grupo11.asociacioncomunal.service;

import com.sv.grupo11.asociacioncomunal.dto.ResultadoCierre;
import com.sv.grupo11.asociacioncomunal.exception.CierreMensualException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

public interface CierreService {
    /**
     * Genera las cuotas del periodo y audita la morosidad de forma CONCURRENTE.
     * @throws CierreMensualException si alguna tarea falla, se interrumpe o excede el tiempo limite
     */
    ResultadoCierre ejecutarCierre(YearMonth periodo, LocalDate corte, String ejecutadoPor) throws CierreMensualException;

    Optional<ResultadoCierre> ultimoCierre();
}
