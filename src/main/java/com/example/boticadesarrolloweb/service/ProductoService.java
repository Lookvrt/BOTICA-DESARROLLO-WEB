package com.example.boticadesarrolloweb.service;

import com.example.boticadesarrolloweb.entity.LoteEntity;
import com.example.boticadesarrolloweb.entity.ProductoEntity;
import com.example.boticadesarrolloweb.model.InventarioItemDto;
import com.example.boticadesarrolloweb.repository.LoteRepository;
import com.example.boticadesarrolloweb.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final LoteRepository loteRepository;

    @Transactional(readOnly = true)
    public String generarSiguienteCodigo() {
        return String.format("MED%03d", productoRepository.maxNumeroCodigoMed() + 1);
    }

    public List<ProductoEntity> listar(String query, Integer idCategoria) {
        String queryLimpio = (query != null) ? query.trim() : "";
        return productoRepository.buscarPorNombreYCategoria(queryLimpio, idCategoria);
    }

    public List<InventarioItemDto> listarInventario(String query, Integer idCategoria, String filtroStock) {
        List<ProductoEntity> productos = listar(query, idCategoria);

        Map<Integer, List<LoteEntity>> lotesPorProducto = productos.isEmpty() ? Map.of()
                : loteRepository.findByProductoIdProductoInOrderByFechaVencimientoAsc(
                        productos.stream().map(ProductoEntity::getIdProducto).toList())
                .stream()
                .collect(Collectors.groupingBy(l -> l.getProducto().getIdProducto()));

        List<InventarioItemDto> items = productos.stream()
                .map(p -> InventarioItemDto.from(p, lotesPorProducto.getOrDefault(p.getIdProducto(), List.of())))
                .collect(Collectors.toList());

        if (filtroStock != null && !filtroStock.isBlank() && !filtroStock.equalsIgnoreCase("TODOS")) {
            items = items.stream()
                    .filter(i -> i.getEstadoStock().equalsIgnoreCase(filtroStock.trim()))
                    .collect(Collectors.toList());
        }

        return items;
    }

    public Optional<ProductoEntity> buscarPorId(Integer id) {
        return productoRepository.findById(id);
    }

    public Optional<ProductoEntity> buscarPorCodigo(String codigo) {
        return productoRepository.findByCodigo(codigo.trim().toUpperCase());
    }

    public boolean existeCodigo(String codigo) {
        return productoRepository.contarPorCodigo(codigo.trim().toUpperCase(), 0) > 0;
    }

    /** Lanza IllegalArgumentException si otro producto, activo o dado de baja, ya usa el código. */
    @Transactional(readOnly = true)
    public void validarCodigoDisponible(String codigo, Integer idExcluir) {
        if (productoRepository.contarPorCodigo(codigo, idExcluir != null ? idExcluir : 0) > 0) {
            throw new IllegalArgumentException("El código " + codigo
                    + " ya está en uso (puede pertenecer a un producto dado de baja).");
        }
    }

    @Transactional
    public ProductoEntity crear(ProductoEntity producto,
                                String numeroLote,
                                LocalDate fechaVencimiento,
                                Integer stockInicial) {

        if (producto.getCodigo() == null || producto.getCodigo().isBlank()) {
            producto.setCodigo(generarSiguienteCodigo());
        } else {
            producto.setCodigo(producto.getCodigo().trim().toUpperCase());
        }
        validarCodigoDisponible(producto.getCodigo(), null);
        if (producto.getEstado() == null) {
            producto.setEstado(1);
        }
        if (producto.getRequiereReceta() == null) {
            producto.setRequiereReceta(0);
        }
        if (producto.getPrecioCompra() == null) {
            producto.setPrecioCompra(BigDecimal.ZERO);
        }

        ProductoEntity guardado = productoRepository.save(producto);

        if (stockInicial != null && stockInicial > 0) {
            String nroLote = (numeroLote != null && !numeroLote.isBlank())
                    ? numeroLote.trim().toUpperCase()
                    : "LT-" + LocalDate.now().getYear() + "-001";

            LocalDate venc = (fechaVencimiento != null)
                    ? fechaVencimiento
                    : LocalDate.now().plusYears(2);

            LoteEntity lote = LoteEntity.builder()
                    .producto(guardado)
                    .numeroLote(nroLote)
                    .fechaFabricacion(LocalDate.now())
                    .fechaVencimiento(venc)
                    .cantidadInicial(stockInicial)
                    .stockActual(stockInicial)
                    .precioCompra(guardado.getPrecioCompra())
                    .build();

            loteRepository.save(lote);
            log.info("Lote inicial registrado para producto {}: Lote {}, Stock {}",
                    guardado.getCodigo(), nroLote, stockInicial);
        }

        return guardado;
    }

    @Transactional
    public ProductoEntity actualizar(ProductoEntity producto) {
        return productoRepository.findById(producto.getIdProducto()).map(actual -> {
            actual.setNombre(producto.getNombre().trim());
            actual.setPrincipioActivo(producto.getPrincipioActivo());
            actual.setConcentracion(producto.getConcentracion());
            actual.setPresentacion(producto.getPresentacion());
            actual.setCategoria(producto.getCategoria());
            actual.setPrecioCompra(producto.getPrecioCompra());
            actual.setPrecioVenta(producto.getPrecioVenta());
            actual.setStockMinimo(producto.getStockMinimo());
            actual.setRequiereReceta(producto.getRequiereReceta() != null ? producto.getRequiereReceta() : 0);
            return productoRepository.save(actual);
        }).orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + producto.getIdProducto()));
    }

    @Transactional
    public void eliminar(Integer idProducto) {
        productoRepository.deleteById(idProducto);
    }
}