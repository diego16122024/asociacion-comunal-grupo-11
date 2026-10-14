package com.sv.grupo11.asociacioncomunal.exception;

/** Datos de entrada invalidos (formato de DUI, telefono, campos obligatorios). */
public class ValidacionException extends AsociacionException {
    public ValidacionException(String mensaje) { super(mensaje); }
}
