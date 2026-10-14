package com.sv.grupo11.asociacioncomunal.exception;

/** El socio, cuota o usuario solicitado no existe. */
public class RecursoNoEncontradoException extends AsociacionException {
    public RecursoNoEncontradoException(String mensaje) { super(mensaje); }
}
