package com.example.boticadesarrolloweb.repository;

import com.example.boticadesarrolloweb.entity.ProductoEntity;
import org.springframework.data.domain.Pageable;
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

    // Punto de venta: busca por nombre, código o principio activo
    @Query("SELECT p FROM ProductoEntity p WHERE " +
            "LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(p.codigo) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(p.principioActivo) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "ORDER BY p.nombre ASC")
    List<ProductoEntity> buscarParaVenta(@Param("q") String q, Pageable pageable);

    @Query("SELECT p FROM ProductoEntity p LEFT JOIN FETCH p.categoria ORDER BY p.idProducto DESC")
    List<ProductoEntity> findAllConCategoria();

    /** Mayor número entre los códigos MED### (SQL nativo: incluye productos dados de baja, cuyo código sigue ocupado). */
    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(codigo, 4) AS UNSIGNED)), 0) FROM productos " +
            "WHERE codigo REGEXP '^MED[0-9]+$'", nativeQuery = true)
    Integer maxNumeroCodigoMed();

    /** Cuántos productos usan el código (activos o dados de baja), sin contar el producto indicado. */
    @Query(value = "SELECT COUNT(*) FROM productos WHERE codigo = :codigo AND id_producto <> :idExcluir",
            nativeQuery = true)
    long contarPorCodigo(@Param("codigo") String codigo, @Param("idExcluir") Integer idExcluir);
}