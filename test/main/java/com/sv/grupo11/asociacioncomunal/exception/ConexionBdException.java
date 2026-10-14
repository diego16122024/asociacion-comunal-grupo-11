package com.sv.grupo11.asociacioncomunal.exception;

/** No se pudo obtener o usar una conexion del pool (BD caida, pool agotado, timeout). */
public class ConexionBdException extends RepositoryException {
    public ConexionBdException(String mensaje, Throwable causa) { super(mensaje, causa); }
}
