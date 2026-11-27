package com.example.boticadesarrolloweb.model;

import com.example.boticadesarrolloweb.enums.MetodoPago;

import java.math.BigDecimal;
import java.util.List;

/**
 * Objetos JSON que intercambia la pantalla de Punto de Venta (static/js/punto-de-venta.js).
 */
public final class PuntoVentaDto {

    private PuntoVentaDto() {
    }

    /** Resultado de búsqueda: stock = solo lotes no vencidos (lo que realmente se puede vender). */
    public record ProductoVenta(Integer idProducto, String codigo, String nombre, String presentacion,
                                BigDecimal precio, Integer stock, boolean requiereReceta) {
    }

    public record ItemVenta(Integer idProducto, Integer cantidad) {
    }

    public record VentaRequest(MetodoPago metodoPago, List<ItemVenta> items) {
    }

    public record VentaResponse(Integer idVenta, String numeroComprobante, BigDecimal total, String metodoPago) {
    }
}
