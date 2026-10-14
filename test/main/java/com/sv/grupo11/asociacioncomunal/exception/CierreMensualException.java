package com.sv.grupo11.asociacioncomunal.exception;

import java.util.List;

/**
 * Excepcion CHECKED: el cierre mensual termino con errores en una o mas tareas concurrentes.
 * Al ser checked obliga a quien la invoca a decidir que hacer (throws / try-catch).
 */
public class CierreMensualException extends Exception {
    private final List<String> errores;

    public CierreMensualException(String mensaje, List<String> errores) {
        super(mensaje + ": " + String.join(" | ", errores));
        this.errores = List.copyOf(errores);
    }

    public CierreMensualException(String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.errores = List.of(String.valueOf(causa.getMessage()));
    }

    public List<String> getErrores() { return errores; }
}
