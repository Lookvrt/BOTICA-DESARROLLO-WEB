package com.example.boticadesarrolloweb.repository;

import com.example.boticadesarrolloweb.entity.DetalleCompraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DetalleCompraRepository extends JpaRepository<DetalleCompraEntity, Integer> {

    @Query("SELECT d FROM DetalleCompraEntity d JOIN FETCH d.lote JOIN FETCH d.producto WHERE d.compra.idCompra = :idCompra")
    List<DetalleCompraEntity> findByCompraConLote(@Param("idCompra") Integer idCompra);

    boolean existsByCompraIdCompra(Integer idCompra);
}
