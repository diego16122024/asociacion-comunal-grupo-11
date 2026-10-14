package com.sv.grupo11.asociacioncomunal.exception;

/** Violacion de restriccion UNIQUE (SQLState 23xxx), p. ej. DUI repetido o cuota duplicada. */
public class DuplicateKeyException extends RepositoryException {
    public DuplicateKeyException(String mensaje, Throwable causa) { super(mensaje, causa); }
}
