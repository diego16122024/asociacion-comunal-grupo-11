package com.sv.grupo11.asociacioncomunal.repository;

import com.sv.grupo11.asociacioncomunal.dto.ResultadoCierre;
import java.util.Optional;

public interface CierreRepository {
    void guardar(ResultadoCierre resultado);
    Optional<ResultadoCierre> ultimo();
}
