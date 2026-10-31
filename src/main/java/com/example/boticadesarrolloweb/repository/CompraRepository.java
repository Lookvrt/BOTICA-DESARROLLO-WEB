package com.example.boticadesarrolloweb.repository;

import com.example.boticadesarrolloweb.entity.CompraEntity;
import com.example.boticadesarrolloweb.enums.EstadoCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface CompraRepository extends JpaRepository<CompraEntity, Integer> {

    @Query("SELECT COALESCE(SUM(c.total), 0) FROM CompraEntity c WHERE c.estado = :estado AND c.fechaCompra >= :inicio AND c.fechaCompra <= :fin")
    BigDecimal sumarTotalPorRangoFechas(@Param("estado") EstadoCompra estado,
                                        @Param("inicio") LocalDateTime inicio,
                                        @Param("fin") LocalDateTime fin);

    @Query("SELECT c FROM CompraEntity c WHERE c.estado = :estado " +
            "AND (:query IS NULL OR :query = '' OR LOWER(c.proveedor.razonSocial) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY " +
            "CASE WHEN :orden = 'desc' THEN c.proveedor.razonSocial END DESC, " +
            "CASE WHEN :orden = 'asc' OR :orden IS NULL OR :orden = '' THEN c.proveedor.razonSocial END ASC")
    List<CompraEntity> buscarYOrdenarPorProveedor(@Param("query") String query,
                                                  @Param("orden") String orden,
                                                  @Param("estado") EstadoCompra estado);
}