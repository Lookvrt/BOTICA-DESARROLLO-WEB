package com.example.boticadesarrolloweb.enums;

import lombok.Getter;

@Getter
public enum EstadoVenta {
    REGISTRADA("Registrada"),
    ANULADA("Anulada");

    private final String descripcion;

    EstadoVenta(String descripcion) {
        this.descripcion = descripcion;
    }
}