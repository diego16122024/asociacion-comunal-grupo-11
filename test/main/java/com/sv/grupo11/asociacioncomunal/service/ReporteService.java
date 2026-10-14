package com.sv.grupo11.asociacioncomunal.service;

import com.sv.grupo11.asociacioncomunal.dto.MorosoDTO;
import com.sv.grupo11.asociacioncomunal.dto.ResumenPanel;
import java.util.List;

public interface ReporteService {
    ResumenPanel resumen();
    List<MorosoDTO> morosos();
}
