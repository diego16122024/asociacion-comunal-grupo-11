package com.sv.grupo11.asociacioncomunal.service.impl;

import com.sv.grupo11.asociacioncomunal.dto.MorosoDTO;
import com.sv.grupo11.asociacioncomunal.dto.ResumenPanel;
import com.sv.grupo11.asociacioncomunal.model.Cuota;
import com.sv.grupo11.asociacioncomunal.model.EstadoCuota;
import com.sv.grupo11.asociacioncomunal.model.Socio;
import com.sv.grupo11.asociacioncomunal.repository.CierreRepository;
import com.sv.grupo11.asociacioncomunal.repository.CuotaRepository;
import com.sv.grupo11.asociacioncomunal.repository.CuotaRepository.TotalPorEstado;
import com.sv.grupo11.asociacioncomunal.repository.SocioRepository;
import com.sv.grupo11.asociacioncomunal.service.ReporteService;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReporteServiceImpl implements ReporteService {
    private static final int ULTIMOS_PAGOS = 6;

    private final SocioRepository socioRepository;
    private final CuotaRepository cuotaRepository;
    private final CierreRepository cierreRepository;

    public ReporteServiceImpl(SocioRepository socioRepository, CuotaRepository cuotaRepository,
                              CierreRepository cierreRepository) {
        this.socioRepository = socioRepository;
        this.cuotaRepository = cuotaRepository;
        this.cierreRepository = cierreRepository;
    }

    @Override
    public ResumenPanel resumen() {
        List<Socio> socios = socioRepository.listarTodos();
        Map<EstadoCuota, TotalPorEstado> t = cuotaRepository.totalesPorEstado();
        TotalPorEstado pagadas = t.get(EstadoCuota.PAGADA);
        TotalPorEstado pendientes = t.get(EstadoCuota.PENDIENTE);
        TotalPorEstado vencidas = t.get(EstadoCuota.VENCIDA);
        return new ResumenPanel(
                socios.stream().filter(Socio::estaActivo).count(), socios.size(),
                socios.stream().filter(Socio::esDirectiva).count(),
                pagadas.monto(), pagadas.cantidad(),
                pendientes.monto(), pendientes.cantidad(),
                vencidas.monto(), vencidas.cantidad(),
                cuotaRepository.ultimosPagos(ULTIMOS_PAGOS),
                cierreRepository.ultimo().orElse(null));
    }

    @Override
    public List<MorosoDTO> morosos() {
        Map<Long, List<Cuota>> vencidasPorSocio = cuotaRepository.buscarPorEstado(EstadoCuota.VENCIDA).stream()
                .collect(Collectors.groupingBy(Cuota::getIdSocio));
        Map<Long, Socio> sociosPorId = socioRepository.listarTodos().stream()
                .collect(Collectors.toMap(Socio::getId, Function.identity()));
        return vencidasPorSocio.entrySet().stream()
                .filter(e -> sociosPorId.containsKey(e.getKey()))
                .map(e -> new MorosoDTO(sociosPorId.get(e.getKey()), e.getValue().size(),
                        e.getValue().stream().map(Cuota::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add)))
                .sorted(Comparator.comparing(MorosoDTO::montoAdeudado).reversed())
                .toList();
    }
}
