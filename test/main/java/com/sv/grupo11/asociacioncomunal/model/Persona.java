package com.sv.grupo11.asociacioncomunal.model;

/** Clase abstracta: datos personales comunes. Encapsulamiento con atributos privados. */
public abstract class Persona {
    private String nombre;
    private String dui;
    private String telefono;
    private String direccion;

    protected Persona(String nombre, String dui, String telefono, String direccion) {
        this.nombre = nombre;
        this.dui = dui;
        this.telefono = telefono;
        this.direccion = direccion;
    }

    /** Metodo abstracto: cada tipo de persona se describe distinto (polimorfismo). */
    public abstract String descripcionCorta();

    public String getNombre() { return nombre; }
    public String getDui() { return dui; }
    public String getTelefono() { return telefono; }
    public String getDireccion() { return direccion; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
}
