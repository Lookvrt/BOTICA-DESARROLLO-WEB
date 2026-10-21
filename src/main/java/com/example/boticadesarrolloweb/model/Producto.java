package com.example.boticadesarrolloweb.model;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    private Integer idProducto;

    @NotBlank(message = "El código del producto es obligatorio")
    @Size(max = 30, message = "El código no debe superar 30 caracteres")
    private String codigo;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 150, message = "El nombre no debe superar 150 caracteres")
    private String nombre;

    private String descripcion;

    @Size(max = 150, message = "El principio activo no debe superar 150 caracteres")
    private String principioActivo;

    @Size(max = 50, message = "La concentración no debe superar 50 caracteres")
    private String concentracion;

    @Size(max = 100, message = "La presentación no debe superar 100 caracteres")
    private String presentacion;

    @NotNull(message = "Debe seleccionar una categoría")
    private Integer idCategoria;

    @NotNull(message = "El precio de compra es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio de compra no puede ser negativo")
    @Builder.Default
    private BigDecimal precioCompra = BigDecimal.ZERO;

    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio de venta debe ser mayor a cero")
    @Builder.Default
    private BigDecimal precioVenta = BigDecimal.ZERO;

    @NotNull(message = "El stock mínimo es obligatorio")
    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    @Builder.Default
    private Integer stockMinimo = 5;

    @NotNull(message = "Indique si requiere receta médica")
    @Builder.Default
    private Integer requiereReceta = 0;

    @Builder.Default
    private Integer estado = 1;
}