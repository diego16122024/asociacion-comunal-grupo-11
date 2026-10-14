package com.sv.grupo11.asociacioncomunal.exception;

/** El rol del usuario no tiene permiso para la operacion. */
public class AccesoDenegadoException extends AsociacionException {
    public AccesoDenegadoException(String mensaje) { super(mensaje); }
}
