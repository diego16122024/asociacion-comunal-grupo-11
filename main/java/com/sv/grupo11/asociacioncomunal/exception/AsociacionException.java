package com.sv.grupo11.asociacioncomunal.exception;

/** Raiz de las excepciones propias del sistema (errores de negocio y de persistencia). */
public class AsociacionException extends RuntimeException {
    public AsociacionException(String mensaje) { super(mensaje); }
    public AsociacionException(String mensaje, Throwable causa) { super(mensaje, causa); }
}
