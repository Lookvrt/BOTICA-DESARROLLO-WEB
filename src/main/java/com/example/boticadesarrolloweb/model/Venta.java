package com.example.boticadesarrolloweb.model;

import com.example.boticadesarrolloweb.enums.EstadoVenta;
import com.example.boticadesarrolloweb.enums.MetodoPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Formulario de venta del módulo Ventas (crear / editar). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venta {

    private Integer idVenta;

    /** Obligatorio en facturas; las boletas del Punto de Venta no tienen cliente. */
    private Integer idCliente;

    @NotNull(message = "Debe seleccionar un método de pago")
    @Builder.Default
    private MetodoPago metodoPago = MetodoPago.EFECTIVO;

    // ── Solo lectura en el formulario ──
    private LocalDateTime fechaVenta;
    private String tipoComprobante;
    private String numeroComprobante;
    private String vendedor;

    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Builder.Default
    private EstadoVenta estado = EstadoVenta.REGISTRADA;

    /** Una línea por producto (al guardar se reparte entre lotes en orden FEFO). */
    @Valid
    @Builder.Default
    private List<DetalleVenta> detalles = new ArrayList<>();
}
