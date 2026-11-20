package com.example.boticadesarrolloweb.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * Línea del formulario de venta: un producto y su cantidad. El precio no se envía desde el navegador:
 * se toma del producto (o se conserva el original al editar una venta).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleVenta {

    @NotNull(message = "Debe seleccionar un producto")
    private Integer idProducto;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima a vender es 1")
    private Integer cantidad;

    // ── Datos de apoyo para la vista (no se guardan) ──
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private String codigoProducto;
    private String nombreProducto;

    /** Unidades que se pueden vender de este producto (incluye las que ya tiene esta venta al editar). */
    private Integer stockDisponible;
}
