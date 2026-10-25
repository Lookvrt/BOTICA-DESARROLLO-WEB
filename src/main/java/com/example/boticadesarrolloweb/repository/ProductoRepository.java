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
           "(LOWER(p.codigo) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.nombre) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.principioActivo) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.concentracion) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<ProductoEntity> buscar(@Param("query") String query);

    @Query("SELECT p FROM ProductoEntity p LEFT JOIN FETCH p.categoria WHERE p.categoria.idCategoria = :idCategoria")
    List<ProductoEntity> findByCategoriaId(@Param("idCategoria") Integer idCategoria);

    @Query("SELECT p FROM ProductoEntity p LEFT JOIN FETCH p.categoria WHERE p.categoria.idCategoria = :idCategoria AND " +
           "(LOWER(p.codigo) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.nombre) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.principioActivo) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<ProductoEntity> buscarPorCategoriaYTexto(@Param("idCategoria") Integer idCategoria, @Param("query") String query);

    @Query("SELECT p FROM ProductoEntity p LEFT JOIN FETCH p.categoria ORDER BY p.idProducto DESC")
    List<ProductoEntity> findAllConCategoria();
}
