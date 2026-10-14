package com.sv.grupo11.asociacioncomunal;

import com.sv.grupo11.asociacioncomunal.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/** Fabrica de objetos de prueba (evita repetir constructores largos en cada test). */
public final class TestData {
    private TestData() { }

    public static Socio socio(long id, String dui) {
        return new Socio(id, "Socio " + id, dui, "7000-0000", "Calle " + id, LocalDate.of(2025, 1, 1), EstadoSocio.ACTIVO);
    }

    public static Socio socioInactivo(long id, String dui) {
        Socio s = socio(id, dui);
        s.setEstado(EstadoSocio.INACTIVO);
        return s;
    }

    public static MiembroDirectiva directiva(long id, String dui) {
        return new MiembroDirectiva(id, "Directivo " + id, dui, "7000-0001", "Calle", LocalDate.of(2024, 1, 1),
                EstadoSocio.ACTIVO, "Presidente", "2026-2028");
    }

    public static Cuota cuota(long id, long idSocio, String monto, String periodo, EstadoCuota estado) {
        YearMonth p = YearMonth.parse(periodo);
        return new Cuota(id, idSocio, new BigDecimal(monto), p, p.atEndOfMonth(),
                estado == EstadoCuota.PAGADA ? p.atDay(10) : null, estado);
    }

    public static Usuario usuario(Rol rol, Long idSocio) {
        return new Usuario(1L, rol.name().toLowerCase(), "hash", rol, idSocio);
    }
}
