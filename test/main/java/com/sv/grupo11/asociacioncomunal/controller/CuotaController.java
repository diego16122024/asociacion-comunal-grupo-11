package com.sv.grupo11.asociacioncomunal.controller;

import com.sv.grupo11.asociacioncomunal.dto.CuentaSocio;
import com.sv.grupo11.asociacioncomunal.exception.RecursoNoEncontradoException;
import com.sv.grupo11.asociacioncomunal.model.Cuota;
import com.sv.grupo11.asociacioncomunal.model.Permiso;
import com.sv.grupo11.asociacioncomunal.model.Usuario;
import com.sv.grupo11.asociacioncomunal.service.CuotaService;
import com.sv.grupo11.asociacioncomunal.util.SesionUsuario;
import java.time.LocalDate;
import java.util.List;

public class CuotaController {
    private final CuotaService cuotaService;
    private final SesionUsuario sesion;

    public CuotaController(CuotaService cuotaService, SesionUsuario sesion) {
        this.cuotaService = cuotaService;
        this.sesion = sesion;
    }

    public CuentaSocio cuentaDeSocio(long idSocio) {
        sesion.exigir(Permiso.VER_FINANZAS);
        return cuotaService.cuentaDe(idSocio);
    }

    public List<Cuota> cuotasDeSocio(long idSocio) {
        sesion.exigir(Permiso.VER_FINANZAS);
        return cuotaService.cuotasDeSocio(idSocio);
    }

    public Cuota registrarPago(long idCuota, LocalDate fecha) {
        sesion.exigir(Permiso.REGISTRAR_PAGOS);
        return cuotaService.registrarPago(idCuota, fecha);
    }

    /** "Mi cuenta": un usuario solo puede consultar el socio que tiene asociado. */
    public CuentaSocio miCuenta() {
        Usuario u = sesion.requerirUsuario();
        if (u.getIdSocio() == null) {
            throw new RecursoNoEncontradoException("Su usuario no tiene un socio asociado. Pida al administrador que lo vincule.");
        }
        return cuotaService.cuentaDe(u.getIdSocio());
    }
}
