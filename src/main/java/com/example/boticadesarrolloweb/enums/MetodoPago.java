package com.example.boticadesarrolloweb.enums;

import lombok.Getter;

@Getter
public enum MetodoPago {
    EFECTIVO("Efectivo"),
    TARJETA("Tarjeta de Débito/Crédito"),
    TRANSFERENCIA("Transferencia Bancaria"),
    YAPE("Yape"),
    PLIN("Plin"),
    OTRO("Otro");

    private final String descripcion;

    MetodoPago(String descripcion) {
        this.descripcion = descripcion;
    }
}