package com.sv.grupo11.asociacioncomunal.service;

import com.sv.grupo11.asociacioncomunal.dto.CuentaSocio;
import com.sv.grupo11.asociacioncomunal.model.Cuota;
import java.time.LocalDate;
import java.util.List;

public interface CuotaService {
    List<Cuota> cuotasDeSocio(long idSocio);
    Cuota registrarPago(long idCuota, LocalDate fechaPago);
    CuentaSocio cuentaDe(long idSocio);
}
