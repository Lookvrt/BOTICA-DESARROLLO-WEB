package com.example.boticadesarrolloweb.repository;

import com.example.boticadesarrolloweb.entity.DetalleVentaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DetalleVentaRepository extends JpaRepository<DetalleVentaEntity, Integer> {

    boolean existsByLoteIdLote(Integer idLote);

    @Query("SELECT d FROM DetalleVentaEntity d JOIN FETCH d.lote JOIN FETCH d.producto WHERE d.venta.idVenta = :idVenta ORDER BY d.idDetalleVenta")
    List<DetalleVentaEntity> findByVentaConLote(@Param("idVenta") Integer idVenta);
}
