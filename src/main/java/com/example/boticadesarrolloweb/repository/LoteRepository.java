package com.example.boticadesarrolloweb.repository;

import com.example.boticadesarrolloweb.entity.LoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LoteRepository extends JpaRepository<LoteEntity, Integer> {

    List<LoteEntity> findByProductoIdProductoOrderByFechaVencimientoAsc(Integer idProducto);

    @Query("SELECT COALESCE(SUM(l.stockActual), 0) FROM LoteEntity l WHERE l.producto.idProducto = :idProducto")
    Integer sumStockByProductoId(@Param("idProducto") Integer idProducto);

    @Query("SELECT l FROM LoteEntity l WHERE l.producto.idProducto = :idProducto AND l.stockActual > 0 ORDER BY l.fechaVencimiento ASC")
    List<LoteEntity> findLotesDisponiblesByProductoId(@Param("idProducto") Integer idProducto);

    List<LoteEntity> findByFechaVencimientoBetweenOrderByFechaVencimientoAsc(LocalDate desde, LocalDate hasta);

    List<LoteEntity> findByFechaVencimientoBeforeOrderByFechaVencimientoAsc(LocalDate fecha);

    boolean existsByProductoIdProductoAndNumeroLoteIgnoreCase(Integer idProducto, String numeroLote);
}
