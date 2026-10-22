package com.example.boticadesarrolloweb.repository;

import com.example.boticadesarrolloweb.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Integer> {

    Optional<UsuarioEntity> findByUsuario(String usuario);

    Optional<UsuarioEntity> findByEmail(String email);

    boolean existsByUsuario(String usuario);

    boolean existsByEmail(String email);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM UsuarioEntity u WHERE " +
           "LOWER(u.nombres) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.apellidos) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.usuario) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    java.util.List<UsuarioEntity> buscar(@org.springframework.data.repository.query.Param("query") String query);
}
