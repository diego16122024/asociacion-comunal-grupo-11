package com.sv.grupo11.asociacioncomunal.service.impl;

import com.sv.grupo11.asociacioncomunal.dto.CuentaSocio;
import com.sv.grupo11.asociacioncomunal.exception.RecursoNoEncontradoException;
import com.sv.grupo11.asociacioncomunal.exception.ReglaNegocioException;
import com.sv.grupo11.asociacioncomunal.model.Cuota;
import com.sv.grupo11.asociacioncomunal.model.EstadoCuota;
import com.sv.grupo11.asociacioncomunal.model.Socio;
import com.sv.grupo11.asociacioncomunal.repository.CuotaRepository;
import com.sv.grupo11.asociacioncomunal.repository.SocioRepository;
import com.sv.grupo11.asociacioncomunal.service.CuotaService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public class CuotaServiceImpl implements CuotaService {
    private final CuotaRepository cuotaRepository;
    private final SocioRepository socioRepository;
    private final Clock clock;

    public CuotaServiceImpl(CuotaRepository cuotaRepository, SocioRepository socioRepository, Clock clock) {
        this.cuotaRepository = cuotaRepository;
        this.socioRepository = socioRepository;
        this.clock = clock;
    }

    @Override
    public List<Cuota> cuotasDeSocio(long idSocio) {
        socioRepository.buscarPorId(idSocio)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el socio #" + idSocio));
        return cuotaRepository.buscarPorSocio(idSocio);
    }

    @Override
    public Cuota registrarPago(long idCuota, LocalDate fechaPago) {
        Cuota cuota = cuotaRepository.buscarPorId(idCuota)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la cuota #" + idCuota));
        if (cuota.estaPagada()) {
            throw new ReglaNegocioException("La cuota " + idCuota + " ya fue pagada.");
        }
        LocalDate fecha = fechaPago != null ? fechaPago : LocalDate.now(clock);
        // UPDATE atomico: si otro hilo/usuario la pago entre la lectura y aqui, devuelve false.
        if (!cuotaRepository.marcarPagada(idCuota, fecha)) {
            throw new ReglaNegocioException("La cuota " + idCuota + " ya fue pagada.");
        }
        return cuotaRepository.buscarPorId(idCuota)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la cuota #" + idCuota));
    }

    @Override
    public CuentaSocio cuentaDe(long idSocio) {
        Socio socio = socioRepository.buscarPorId(idSocio)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el socio #" + idSocio));
        List<Cuota> historial = cuotaRepository.buscarPorSocio(idSocio);
        List<Cuota> pendientes = historial.stream().filter(c -> c.getEstado() != EstadoCuota.PAGADA).toList();
        BigDecimal total = pendientes.stream().map(Cuota::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CuentaSocio(socio, pendientes, historial, total);
    }
}
