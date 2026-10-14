package com.sv.grupo11.asociacioncomunal.controller;

import com.sv.grupo11.asociacioncomunal.dto.MorosoDTO;
import com.sv.grupo11.asociacioncomunal.dto.ResumenPanel;
import com.sv.grupo11.asociacioncomunal.model.Permiso;
import com.sv.grupo11.asociacioncomunal.service.ReporteService;
import com.sv.grupo11.asociacioncomunal.util.SesionUsuario;
import java.util.List;

public class ReporteController {
    private final ReporteService reporteService;
    private final SesionUsuario sesion;

    public ReporteController(ReporteService reporteService, SesionUsuario sesion) {
        this.reporteService = reporteService;
        this.sesion = sesion;
    }

    public ResumenPanel resumen() {
        sesion.exigir(Permiso.VER_SOCIOS);
        return reporteService.resumen();
    }

    public List<MorosoDTO> morosos() {
        sesion.exigir(Permiso.VER_MOROSOS);
        return reporteService.morosos();
    }
}
