package com.sv.grupo11.asociacioncomunal.exception;

/** Violacion de una regla de negocio (DUI repetido, cuota ya pagada, usuario duplicado, etc.). */
public class ReglaNegocioException extends AsociacionException {
    public ReglaNegocioException(String mensaje) { super(mensaje); }
    public ReglaNegocioException(String mensaje, Throwable causa) { super(mensaje, causa); }
}
