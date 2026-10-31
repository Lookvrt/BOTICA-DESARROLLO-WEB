package com.example.boticadesarrolloweb.repository;

import com.example.boticadesarrolloweb.entity.ProveedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProveedorRepository extends JpaRepository<ProveedorEntity, Integer> {

    @Query("SELECT p FROM ProveedorEntity p WHERE " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(REPLACE(p.razonSocial, ' ', '')) LIKE LOWER(CONCAT('%', REPLACE(:query, ' ', ''), '%'))) " +
            "ORDER BY " +
            "CASE WHEN :orden = 'desc' THEN p.razonSocial END DESC, " +
            "CASE WHEN :orden = 'asc' OR :orden IS NULL OR :orden = '' THEN p.razonSocial END ASC")
    List<ProveedorEntity> buscarYOrdenarPorRazonSocial(@Param("query") String query,
                                                       @Param("orden") String orden);
}