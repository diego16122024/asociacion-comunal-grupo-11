package com.sv.grupo11.asociacioncomunal.model;

public class Usuario {
    private Long id;
    private final String nombreUsuario;
    private final String claveHash;
    private final Rol rol;
    private final Long idSocio;

    public Usuario(Long id, String nombreUsuario, String claveHash, Rol rol, Long idSocio) {
        this.id = id;
        this.nombreUsuario = nombreUsuario;
        this.claveHash = claveHash;
        this.rol = rol;
        this.idSocio = idSocio;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombreUsuario() { return nombreUsuario; }
    public String getClaveHash() { return claveHash; }
    public Rol getRol() { return rol; }
    public Long getIdSocio() { return idSocio; }
    public boolean puede(Permiso permiso) { return rol.tiene(permiso); }
}
