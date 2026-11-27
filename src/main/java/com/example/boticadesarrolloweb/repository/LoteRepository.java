package com.example.boticadesarrolloweb.repository;

import com.example.boticadesarrolloweb.entity.LoteEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public interface LoteRepository extends JpaRepository<LoteEntity, Integer> {

    List<LoteEntity> findByProductoIdProductoOrderByFechaVencimientoAsc(Integer idProducto);

    /** Lotes de varios productos en una sola consulta (evita una consulta por producto). */
    List<LoteEntity> findByProductoIdProductoInOrderByFechaVencimientoAsc(Collection<Integer> idsProducto);

    @Query("SELECT l.producto.idProducto, SUM(l.stockActual) FROM LoteEntity l " +
            "WHERE l.producto.idProducto IN :ids AND l.stockActual > 0 AND l.fechaVencimiento >= :hoy " +
            "GROUP BY l.producto.idProducto")
    List<Object[]> sumStockVendiblePorProductos(@Param("ids") Collection<Integer> ids, @Param("hoy") LocalDate hoy);

    @Query("SELECT l.producto.idProducto, SUM(l.stockActual) FROM LoteEntity l " +
            "WHERE l.producto.idProducto IN :ids GROUP BY l.producto.idProducto")
    List<Object[]> sumStockPorProductos(@Param("ids") Collection<Integer> ids);

    /** Stock vendible (con stock y no vencido) por producto, en una sola consulta. Sin lotes = sin entrada en el mapa. */
    default Map<Integer, Integer> stockVendiblePorProducto(Collection<Integer> ids, LocalDate hoy) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return sumStockVendiblePorProductos(ids, hoy).stream().collect(Collectors.toMap(
                fila -> ((Number) fila[0]).intValue(), fila -> ((Number) fila[1]).intValue()));
    }

    /** Stock total por producto, en una sola consulta. Sin lotes = sin entrada en el mapa. */
    default Map<Integer, Integer> stockTotalPorProducto(Collection<Integer> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return sumStockPorProductos(ids).stream().collect(Collectors.toMap(
                fila -> ((Number) fila[0]).intValue(), fila -> ((Number) fila[1]).intValue()));
    }

    @Query("SELECT COALESCE(SUM(l.stockActual), 0) FROM LoteEntity l WHERE l.producto.idProducto = :idProducto")
    Integer sumStockByProductoId(@Param("idProducto") Integer idProducto);

    @Query("SELECT l FROM LoteEntity l WHERE l.producto.idProducto = :idProducto AND l.stockActual > 0 ORDER BY l.fechaVencimiento ASC")
    List<LoteEntity> findLotesDisponiblesByProductoId(@Param("idProducto") Integer idProducto);

    List<LoteEntity> findByFechaVencimientoBetweenOrderByFechaVencimientoAsc(LocalDate desde, LocalDate hasta);

    List<LoteEntity> findByFechaVencimientoBeforeOrderByFechaVencimientoAsc(LocalDate fecha);

    boolean existsByProductoIdProductoAndNumeroLoteIgnoreCase(Integer idProducto, String numeroLote);

    // ── Punto de venta: solo lotes con stock y no vencidos (orden FEFO) ──

    @Query("SELECT COALESCE(SUM(l.stockActual), 0) FROM LoteEntity l " +
            "WHERE l.producto.idProducto = :idProducto AND l.stockActual > 0 AND l.fechaVencimiento >= :hoy")
    Integer sumStockVendible(@Param("idProducto") Integer idProducto, @Param("hoy") LocalDate hoy);

    /** Bloquea los lotes (SELECT ... FOR UPDATE) para que dos ventas simultáneas no vendan el mismo stock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM LoteEntity l WHERE l.producto.idProducto = :idProducto " +
            "AND l.stockActual > 0 AND l.fechaVencimiento >= :hoy ORDER BY l.fechaVencimiento ASC, l.idLote ASC")
    List<LoteEntity> findLotesVendiblesParaVenta(@Param("idProducto") Integer idProducto, @Param("hoy") LocalDate hoy);
}
