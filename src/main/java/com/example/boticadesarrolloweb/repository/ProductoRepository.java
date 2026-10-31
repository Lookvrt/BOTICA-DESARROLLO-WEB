package com.example.boticadesarrolloweb.repository;

import com.example.boticadesarrolloweb.entity.ProductoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<ProductoEntity, Integer> {

    Optional<ProductoEntity> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    @Query("SELECT p FROM ProductoEntity p LEFT JOIN FETCH p.categoria WHERE " +
            "(:idCategoria IS NULL OR :idCategoria = 0 OR p.categoria.idCategoria = :idCategoria) AND " +
            "(:query IS NULL OR :query = '' OR LOWER(REPLACE(p.nombre, ' ', '')) LIKE LOWER(CONCAT('%', REPLACE(:query, ' ', ''), '%'))) " +
            "ORDER BY p.idProducto DESC")
    List<ProductoEntity> buscarPorNombreYCategoria(@Param("query") String query,
                                                   @Param("idCategoria") Integer idCategoria);

    @Query("SELECT p FROM ProductoEntity p LEFT JOIN FETCH p.categoria ORDER BY p.idProducto DESC")
    List<ProductoEntity> findAllConCategoria();
}