package com.example.boticadesarrolloweb.model;

import com.example.boticadesarrolloweb.enums.EstadoCompra;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compra {

    private Integer idCompra;

    @NotNull(message = "Debe seleccionar un proveedor")
    private Integer idProveedor;

    /** Responsable de la compra. Lo asigna el servidor (usuario en sesión); el valor del formulario se ignora. */
    private Integer idUsuario;

    private LocalDateTime fechaCompra;

    @Size(max = 30, message = "El tipo de comprobante no debe superar 30 caracteres")
    private String tipoComprobante;

    @Size(max = 50, message = "El número de comprobante no debe superar 50 caracteres")
    private String numeroComprobante;

    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Builder.Default
    private EstadoCompra estado = EstadoCompra.REGISTRADA;

    /** Productos de la compra (solo al registrar una compra nueva). */
    @Valid
    @Builder.Default
    private List<DetalleCompra> detalles = new ArrayList<>();

}