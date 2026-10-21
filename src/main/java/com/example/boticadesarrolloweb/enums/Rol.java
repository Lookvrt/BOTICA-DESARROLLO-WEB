package com.example.boticadesarrolloweb.enums;

import lombok.Getter;

@Getter
public enum Rol {
    ADMINISTRADOR("Administrador"),
    VENDEDOR("Vendedor"),
    ALMACENERO("Almacenero");

    private final String descripcion;

    Rol(String descripcion) {
        this.descripcion = descripcion;
    }
}