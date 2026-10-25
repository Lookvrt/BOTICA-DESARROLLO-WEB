package com.example.boticadesarrolloweb.repository;

import com.example.boticadesarrolloweb.entity.CompraEntity;
import com.example.boticadesarrolloweb.enums.EstadoCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CompraRepository extends JpaRepository<CompraEntity, Integer> {

    @Query("SELECT COALESCE(SUM(c.total), 0) FROM CompraEntity c WHERE c.estado = :estado AND c.fechaCompra >= :inicio AND c.fechaCompra <= :fin")
    BigDecimal sumarTotalPorRangoFechas(@Param("estado") EstadoCompra estado,
                                        @Param("inicio") LocalDateTime inicio,
                                        @Param("fin") LocalDateTime fin);
}