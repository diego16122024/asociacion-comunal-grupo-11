package com.sv.grupo11.asociacioncomunal.util;

import com.sv.grupo11.asociacioncomunal.exception.ValidacionException;
import java.util.regex.Pattern;

/** Validaciones reutilizables de datos personales (una sola fuente de verdad para todos los servicios). */
public final class ValidadorPersona {
    private static final Pattern DUI = Pattern.compile("^\\d{8}-\\d$");
    private static final Pattern TELEFONO = Pattern.compile("^\\d{4}-\\d{4}$");

    private ValidadorPersona() { }

    public static String requerirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ValidacionException("El campo " + campo + " es obligatorio.");
        }
        return valor.trim();
    }

    public static String validarDui(String dui) {
        String d = requerirTexto(dui, "DUI");
        if (!DUI.matcher(d).matches()) throw new ValidacionException("El DUI debe tener el formato ########-#");
        return d;
    }

    public static String validarTelefono(String telefono) {
        String t = requerirTexto(telefono, "telefono");
        if (!TELEFONO.matcher(t).matches()) throw new ValidacionException("El telefono debe tener el formato ####-####");
        return t;
    }
}
