package com.sv.grupo11.asociacioncomunal.controller;

import com.sv.grupo11.asociacioncomunal.dto.ResultadoCierre;
import com.sv.grupo11.asociacioncomunal.exception.CierreMensualException;
import com.sv.grupo11.asociacioncomunal.model.Permiso;
import com.sv.grupo11.asociacioncomunal.model.Usuario;
import com.sv.grupo11.asociacioncomunal.service.CierreService;
import com.sv.grupo11.asociacioncomunal.util.SesionUsuario;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

public class CierreController {
    private final CierreService cierreService;
    private final SesionUsuario sesion;

    public CierreController(CierreService cierreService, SesionUsuario sesion) {
        this.cierreService = cierreService;
        this.sesion = sesion;
    }

    public ResultadoCierre ejecutar(YearMonth periodo, LocalDate corte) throws CierreMensualException {
        Usuario u = sesion.exigir(Permiso.EJECUTAR_CIERRE);
        return cierreService.ejecutarCierre(periodo, corte, u.getNombreUsuario());
    }

    public Optional<ResultadoCierre> ultimo() {
        sesion.exigir(Permiso.VER_FINANZAS);
        return cierreService.ultimoCierre();
    }
}
