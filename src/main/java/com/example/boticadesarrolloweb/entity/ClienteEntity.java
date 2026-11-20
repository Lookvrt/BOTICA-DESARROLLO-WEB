package com.example.boticadesarrolloweb.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Cliente empresa (con RUC) al que se emiten facturas desde el módulo de Ventas.
 * Las ventas del Punto de Venta (boleta, público general) no tienen cliente.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "clientes")
public class ClienteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer idCliente;

    @Column(name = "ruc", nullable = false, length = 11, unique = true)
    private String ruc;

    @Column(name = "razon_social", nullable = false, length = 150)
    private String razonSocial;

    @Column(name = "direccion", length = 200)
    private String direccion;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "correo", length = 100)
    private String correo;

    /** 1 = activo, 0 = inactivo (no aparece al registrar ventas). */
    @Column(name = "estado", nullable = false)
    @Builder.Default
    private Integer estado = 1;
}
