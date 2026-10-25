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

    List<VentaEntity> findTop10ByEstadoOrderByFechaVentaDesc(EstadoVenta estado);

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
            "WHERE v.estado = 'REGISTRADA' AND DATE(v.fecha_venta) = CURRENT_DATE() " +
            "GROUP BY HOUR(v.fecha_venta) " +
            "ORDER BY hora ASC", nativeQuery = true)
    List<Object[]> obtenerVentasHoyPorHora();

    // 2. Ventas por Día (Últimos N días)
    @Query(value = "SELECT DATE(v.fecha_venta) as fecha, SUM(v.total) as total " +
            "FROM ventas v " +
            "WHERE v.estado = 'REGISTRADA' AND v.fecha_venta >= :desde " +
            "GROUP BY DATE(v.fecha_venta) " +
            "ORDER BY fecha ASC", nativeQuery = true)
    List<Object[]> obtenerVentasDiariasDesde(@Param("desde") LocalDateTime desde);
}