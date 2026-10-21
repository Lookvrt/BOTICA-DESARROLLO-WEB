package com.example.boticadesarrolloweb.enums;

import lombok.Getter;

@Getter
public enum EstadoCompra {
    REGISTRADA("Registrada"),
    ANULADA("Anulada");

    private final String descripcion;

    EstadoCompra(String descripcion) {
        this.descripcion = descripcion;
    }
}