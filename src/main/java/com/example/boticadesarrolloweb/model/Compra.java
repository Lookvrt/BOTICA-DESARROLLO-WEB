package com.example.boticadesarrolloweb.model;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compra {

    private Integer idCompra;

    @NotNull(message = "Debe seleccionar un proveedor")
    private Integer idProveedor;

    @NotNull(message = "El usuario responsable es obligatorio")
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

    public enum EstadoCompra {
        REGISTRADA,
        ANULADA
    }
}