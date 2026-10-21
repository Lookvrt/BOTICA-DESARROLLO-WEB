package com.example.boticadesarrolloweb.model;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleVenta {

    private Integer idDetalleVenta;

    private Integer idVenta;

    @NotNull(message = "Debe seleccionar un producto")
    private Integer idProducto;

    @NotNull(message = "Debe seleccionar el lote de salida")
    private Integer idLote;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima a vender es 1")
    private Integer cantidad;

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
    private BigDecimal precioUnitario;

    private BigDecimal subtotal;
}