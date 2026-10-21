package com.example.boticadesarrolloweb.model;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venta {

    private Integer idVenta;

    @NotNull(message = "El usuario vendedor es obligatorio")
    private Integer idUsuario;

    private LocalDateTime fechaVenta;

    @Size(max = 30, message = "El tipo de comprobante no debe superar 30 caracteres")
    private String tipoComprobante;

    @Size(max = 50, message = "El número de comprobante no debe superar 50 caracteres")
    private String numeroComprobante;

    @NotNull(message = "Debe seleccionar un método de pago")
    @Builder.Default
    private MetodoPago metodoPago = MetodoPago.EFECTIVO;

    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Builder.Default
    private EstadoVenta estado = EstadoVenta.REGISTRADA;

    public enum MetodoPago {
        EFECTIVO,
        TARJETA,
        TRANSFERENCIA,
        YAPE,
        PLIN,
        OTRO
    }

    public enum EstadoVenta {
        REGISTRADA,
        ANULADA
    }
}