package com.example.boticadesarrolloweb.model;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Línea de una compra en el formulario. Al registrarse, cada línea crea un lote nuevo
 * del producto (numeroLote + fechaVencimiento) con la cantidad comprada.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleCompra {

    private Integer idDetalleCompra;

    private Integer idCompra;

    @NotNull(message = "Debe seleccionar un producto")
    private Integer idProducto;

    /** Lote generado al registrar la compra (no se envía desde el formulario). */
    private Integer idLote;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
    private BigDecimal precioUnitario;

    private BigDecimal subtotal;

    /** Opcional: si se deja vacío se genera automáticamente. */
    @Size(max = 50, message = "El número de lote no debe superar 50 caracteres")
    private String numeroLote;

    @NotNull(message = "La fecha de vencimiento es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaVencimiento;

    // ── Datos de apoyo para la vista (no se guardan) ──
    private String codigoProducto;
    private String nombreProducto;
    private Integer stockActual;
    private Integer stockMinimo;

    /** Cantidad mínima exigida (p. ej. productos que vienen de Alertas: stock mínimo + 1). */
    private Integer cantidadMinima;

    /** Al editar: unidades de este lote que ya se vendieron (la cantidad no puede bajar de aquí). */
    private Integer cantidadVendida;
}
