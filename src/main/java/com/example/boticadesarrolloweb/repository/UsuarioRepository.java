package com.example.boticadesarrolloweb.repository;

import com.example.boticadesarrolloweb.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Integer> {

    Optional<UsuarioEntity> findByUsuario(String usuario);

    Optional<UsuarioEntity> findByEmail(String email);

    boolean existsByUsuario(String usuario);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM UsuarioEntity u WHERE " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(REPLACE(u.nombres, ' ', '')) LIKE LOWER(CONCAT('%', REPLACE(:query, ' ', ''), '%')) OR " +
            "LOWER(REPLACE(u.apellidos, ' ', '')) LIKE LOWER(CONCAT('%', REPLACE(:query, ' ', ''), '%')) OR " +
            "LOWER(REPLACE(CONCAT(u.nombres, ' ', u.apellidos), ' ', '')) LIKE LOWER(CONCAT('%', REPLACE(:query, ' ', ''), '%'))) " +
            "ORDER BY " +
            "CASE WHEN :orden = 'desc' THEN u.nombres END DESC, " +
            "CASE WHEN :orden = 'asc' OR :orden IS NULL OR :orden = '' THEN u.nombres END ASC")
    List<UsuarioEntity> buscarYOrdenarPorNombre(@Param("query") String query,
                                                @Param("orden") String orden);
}