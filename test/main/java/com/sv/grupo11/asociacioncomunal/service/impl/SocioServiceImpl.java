package com.sv.grupo11.asociacioncomunal.service.impl;

import com.sv.grupo11.asociacioncomunal.dto.SocioRequest;
import com.sv.grupo11.asociacioncomunal.exception.DuplicateKeyException;
import com.sv.grupo11.asociacioncomunal.exception.RecursoNoEncontradoException;
import com.sv.grupo11.asociacioncomunal.exception.ReglaNegocioException;
import com.sv.grupo11.asociacioncomunal.exception.ValidacionException;
import com.sv.grupo11.asociacioncomunal.model.EstadoSocio;
import com.sv.grupo11.asociacioncomunal.model.MiembroDirectiva;
import com.sv.grupo11.asociacioncomunal.model.Socio;
import com.sv.grupo11.asociacioncomunal.repository.SocioRepository;
import com.sv.grupo11.asociacioncomunal.service.SocioService;
import com.sv.grupo11.asociacioncomunal.util.ValidadorPersona;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public class SocioServiceImpl implements SocioService {
    private final SocioRepository socioRepository;   // depende de la INTERFAZ, no del JDBC concreto
    private final Clock clock;

    public SocioServiceImpl(SocioRepository socioRepository, Clock clock) {
        this.socioRepository = socioRepository;
        this.clock = clock;
    }

    @Override
    public Socio registrar(SocioRequest s) {
        String nombre = ValidadorPersona.requerirTexto(s.nombre(), "nombre");
        String dui = ValidadorPersona.validarDui(s.dui());
        String telefono = ValidadorPersona.validarTelefono(s.telefono());
        String direccion = s.direccion() == null ? "" : s.direccion().trim();
        LocalDate ingreso = s.fechaIngreso() != null ? s.fechaIngreso() : LocalDate.now(clock);

        if (s.esDirectiva() && (s.periodoDirectiva() == null || s.periodoDirectiva().isBlank())) {
            throw new ValidacionException("La directiva requiere cargo y periodo.");
        }
        if (socioRepository.existePorDui(dui)) {
            throw new ReglaNegocioException("Ya existe un socio registrado con el DUI " + dui);
        }
        Socio nuevo = s.esDirectiva()
                ? new MiembroDirectiva(null, nombre, dui, telefono, direccion, ingreso, EstadoSocio.ACTIVO,
                        s.cargo().trim(), s.periodoDirectiva().trim())
                : new Socio(null, nombre, dui, telefono, direccion, ingreso, EstadoSocio.ACTIVO);
        try {
            return socioRepository.guardar(nuevo);
        } catch (DuplicateKeyException e) {      // otra transaccion se nos adelanto con el mismo DUI
            throw new ReglaNegocioException("Ya existe un socio registrado con el DUI " + dui, e);
        }
    }

    @Override
    public Socio actualizarContacto(long idSocio, String telefono, String direccion) {
        Socio socio = obtener(idSocio);
        if (telefono != null && !telefono.isBlank()) socio.setTelefono(ValidadorPersona.validarTelefono(telefono));
        if (direccion != null && !direccion.isBlank()) socio.setDireccion(direccion.trim());
        socioRepository.actualizar(socio);
        return socio;
    }

    @Override
    public Socio cambiarEstado(long idSocio) {
        Socio socio = obtener(idSocio);
        socio.setEstado(socio.estaActivo() ? EstadoSocio.INACTIVO : EstadoSocio.ACTIVO);
        socioRepository.actualizar(socio);
        return socio;
    }

    @Override
    public Socio obtener(long idSocio) {
        return socioRepository.buscarPorId(idSocio)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el socio #" + idSocio));
    }

    @Override
    public List<Socio> listar() { return socioRepository.listarTodos(); }

    @Override
    public List<Socio> buscar(String texto) {
        if (texto == null || texto.isBlank()) return listar();
        String q = texto.trim().toLowerCase();
        return listar().stream()
                .filter(s -> s.getNombre().toLowerCase().contains(q) || s.getDui().contains(q))
                .toList();
    }
}
