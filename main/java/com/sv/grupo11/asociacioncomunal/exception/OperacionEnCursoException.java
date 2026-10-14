package com.sv.grupo11.asociacioncomunal.exception;

/** Ya hay un cierre mensual en ejecucion; se rechaza el segundo para evitar condiciones de carrera. */
public class OperacionEnCursoException extends AsociacionException {
    public OperacionEnCursoException(String mensaje) { super(mensaje); }
}
