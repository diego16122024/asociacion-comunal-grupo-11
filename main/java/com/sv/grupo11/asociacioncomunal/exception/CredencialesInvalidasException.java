package com.sv.grupo11.asociacioncomunal.exception;

/** Usuario o contrasena incorrectos. */
public class CredencialesInvalidasException extends AsociacionException {
    public CredencialesInvalidasException(String mensaje) { super(mensaje); }
}
