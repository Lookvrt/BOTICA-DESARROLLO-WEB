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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final LoteRepository loteRepository;

    public List<ProductoEntity> listar(String query, Integer idCategoria) {
        if (idCategoria != null && idCategoria > 0) {
            if (query != null && !query.isBlank()) {
                return productoRepository.buscarPorCategoriaYTexto(idCategoria, query.trim());
            }
            return productoRepository.findByCategoriaId(idCategoria);
        }

        if (query != null && !query.isBlank()) {
            return productoRepository.buscar(query.trim());
        }

        return productoRepository.findAllConCategoria();
    }

    public List<InventarioItemDto> listarInventario(String query, Integer idCategoria, String filtroStock) {
        List<ProductoEntity> productos = listar(query, idCategoria);

        List<InventarioItemDto> items = productos.stream().map(p -> {
            List<LoteEntity> lotes = loteRepository.findByProductoIdProductoOrderByFechaVencimientoAsc(p.getIdProducto());
            return InventarioItemDto.from(p, lotes);
        }).collect(Collectors.toList());

        // Filtrar por estado de stock si se seleccionó
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
        return productoRepository.existsByCodigo(codigo.trim().toUpperCase());
    }

    @Transactional
    public ProductoEntity crear(ProductoEntity producto,
                                String numeroLote,
                                LocalDate fechaVencimiento,
                                Integer stockInicial) {

        producto.setCodigo(producto.getCodigo().trim().toUpperCase());
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

        // Si se especificó un lote o stock inicial, crearlo de inmediato
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
            actual.setCodigo(producto.getCodigo().trim().toUpperCase());
            actual.setNombre(producto.getNombre().trim());
            actual.setDescripcion(producto.getDescripcion());
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
