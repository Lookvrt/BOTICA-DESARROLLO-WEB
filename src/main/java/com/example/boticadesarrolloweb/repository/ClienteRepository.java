package com.example.boticadesarrolloweb.repository;

import com.example.boticadesarrolloweb.entity.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<ClienteEntity, Integer> {

    @Query("SELECT c FROM ClienteEntity c WHERE " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(c.razonSocial) LIKE LOWER(CONCAT('%', :query, '%')) OR c.ruc LIKE CONCAT('%', :query, '%')) " +
            "ORDER BY " +
            "CASE WHEN :orden = 'desc' THEN c.razonSocial END DESC, " +
            "CASE WHEN :orden = 'asc' OR :orden IS NULL OR :orden = '' THEN c.razonSocial END ASC")
    List<ClienteEntity> buscar(@Param("query") String query, @Param("orden") String orden);

    List<ClienteEntity> findByEstadoOrderByRazonSocialAsc(Integer estado);

    Optional<ClienteEntity> findByRuc(String ruc);
}
