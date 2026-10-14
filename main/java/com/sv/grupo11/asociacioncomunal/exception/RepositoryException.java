package com.sv.grupo11.asociacioncomunal.exception;

/** Envuelve una SQLException de JDBC para no filtrar detalles de la capa de datos hacia arriba. */
public class RepositoryException extends AsociacionException {
    public RepositoryException(String mensaje, Throwable causa) { super(mensaje, causa); }
}
