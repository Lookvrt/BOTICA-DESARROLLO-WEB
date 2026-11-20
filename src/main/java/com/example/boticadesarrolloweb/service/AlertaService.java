package com.example.boticadesarrolloweb.service;

import com.example.boticadesarrolloweb.model.InventarioItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertaService {

    private final ProductoService productoService;

    /**
     * Productos activos cuyo stock total (suma de lotes) es menor a su stock mínimo,
     * ordenados por el faltante más grande primero.
     */
    @Transactional(readOnly = true)
    public List<InventarioItemDto> listarProductosBajoStockMinimo() {
        return productoService.listarInventario(null, null, null).stream()
                .filter(i -> i.getStockTotal() < stockMinimo(i))
                .sorted(Comparator.comparingInt((InventarioItemDto i) -> stockMinimo(i) - i.getStockTotal()).reversed())
                .toList();
    }

    private int stockMinimo(InventarioItemDto item) {
        Integer min = item.getProducto().getStockMinimo();
        return min != null ? min : 0;
    }
}
