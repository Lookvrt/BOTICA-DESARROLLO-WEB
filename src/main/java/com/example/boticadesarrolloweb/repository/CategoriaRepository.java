package com.example.boticadesarrolloweb.repository;

import com.example.boticadesarrolloweb.entity.CategoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<CategoriaEntity, Integer> {

    Optional<CategoriaEntity> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    List<CategoriaEntity> findByEstado(Integer estado);
}
