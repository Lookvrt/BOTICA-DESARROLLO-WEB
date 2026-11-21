package com.example.boticadesarrolloweb.repository;

import com.example.boticadesarrolloweb.entity.VentaEntity;
import com.example.boticadesarrolloweb.enums.EstadoVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<VentaEntity, Integer> {

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM VentaEntity v WHERE v.estado = :estado AND v.fechaVenta >= :inicio AND v.fechaVenta <= :fin")
    BigDecimal sumarTotalPorRangoFechas(@Param("estado") EstadoVenta estado,
                                        @Param("inicio") LocalDateTime inicio,
                                        @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM VentaEntity v WHERE v.estado = :estado")
    BigDecimal sumarTotalVentas(@Param("estado") EstadoVenta estado);

    long countByEstado(EstadoVenta estado);

    boolean existsByClienteIdCliente(Integer idCliente);

    /** Listado del módulo Ventas: por comprobante, razón social o RUC del cliente; más recientes primero. */
    @Query("SELECT v FROM VentaEntity v LEFT JOIN FETCH v.cliente c JOIN FETCH v.usuario u WHERE " +
            "(:q IS NULL OR :q = '' OR LOWER(v.numeroComprobante) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR LOWER(c.razonSocial) LIKE LOWER(CONCAT('%', :q, '%')) OR c.ruc LIKE CONCAT('%', :q, '%')) " +
            "ORDER BY v.fechaVenta DESC, v.idVenta DESC")
    List<VentaEntity> buscarParaListado(@Param("q") String q);

    /** Último número de una serie (p. ej. 'F001-'); el relleno con ceros hace que MAX funcione como texto. */
    @Query("SELECT MAX(v.numeroComprobante) FROM VentaEntity v WHERE v.numeroComprobante LIKE CONCAT(:serie, '%')")
    String ultimoNumeroDeSerie(@Param("serie") String serie);

    List<VentaEntity> findTop7ByEstadoOrderByFechaVentaDesc(EstadoVenta estado);

    @Query(value = "SELECT p.nombre FROM detalle_ventas dv " +
            "JOIN productos p ON dv.id_producto = p.id_producto " +
            "JOIN ventas v ON dv.id_venta = v.id_venta " +
            "WHERE v.estado = 'REGISTRADA' " +
            "GROUP BY p.id_producto, p.nombre " +
            "ORDER BY SUM(dv.cantidad) DESC LIMIT 1", nativeQuery = true)
    String obtenerProductoMasVendido();

    // 1. Ventas de Hoy por Hora (08:00 a 22:00)
    @Query(value = "SELECT HOUR(v.fecha_venta) as hora, SUM(v.total) as total " +
            "FROM ventas v " +
            "WHERE v.estado = 'REGISTRADA' " +
            "AND v.fecha_venta >= :inicio AND v.fecha_venta <= :fin " +
            "GROUP BY HOUR(v.fecha_venta) " +
            "ORDER BY hora ASC", nativeQuery = true)
    List<Object[]> obtenerVentasHoyPorHora(@Param("inicio") LocalDateTime inicio,
                                           @Param("fin") LocalDateTime fin);

    // 2. Ventas por Día (Últimos N días)
    @Query(value = "SELECT DATE(v.fecha_venta) as fecha, SUM(v.total) as total " +
            "FROM ventas v " +
            "WHERE v.estado = 'REGISTRADA' AND v.fecha_venta >= :desde " +
            "GROUP BY DATE(v.fecha_venta) " +
            "ORDER BY fecha ASC", nativeQuery = true)
    List<Object[]> obtenerVentasDiariasDesde(@Param("desde") LocalDateTime desde);

    @Query("SELECT v FROM VentaEntity v LEFT JOIN FETCH v.usuario " +
            "WHERE (:estado IS NULL OR v.estado = :estado) " +
            "AND (:inicio IS NULL OR v.fechaVenta >= :inicio) " +
            "AND (:fin IS NULL OR v.fechaVenta <= :fin) " +
            "ORDER BY v.fechaVenta DESC")
    List<VentaEntity> buscarVentasParaReporte(
            @Param("estado") EstadoVenta estado,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);
}