package com.example.boticadesarrolloweb.model;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lote {

    private Integer idLote;

    @NotNull(message = "Debe vincular el lote a un producto")
    private Integer idProducto;

    @NotBlank(message = "El número de lote es obligatorio")
    @Size(max = 50, message = "El número de lote no debe superar 50 caracteres")
    private String numeroLote;

    private LocalDate fechaFabricacion;

    @NotNull(message = "La fecha de vencimiento es obligatoria")
    private LocalDate fechaVencimiento;

    @NotNull(message = "La cantidad inicial es obligatoria")
    @Min(value = 0, message = "La cantidad inicial no puede ser negativa")
    @Builder.Default
    private Integer cantidadInicial = 0;

    @NotNull(message = "El stock actual es obligatorio")
    @Min(value = 0, message = "El stock actual no puede ser negativo")
    @Builder.Default
    private Integer stockActual = 0;

    @NotNull(message = "El precio de compra es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio de compra no puede ser negativo")
    @Builder.Default
    private BigDecimal precioCompra = BigDecimal.ZERO;
}