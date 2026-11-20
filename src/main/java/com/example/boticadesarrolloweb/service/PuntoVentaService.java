package com.example.boticadesarrolloweb.service;

import com.example.boticadesarrolloweb.entity.*;
import com.example.boticadesarrolloweb.enums.EstadoVenta;
import com.example.boticadesarrolloweb.enums.MetodoPago;
import com.example.boticadesarrolloweb.model.PuntoVentaDto.ItemVenta;
import com.example.boticadesarrolloweb.model.PuntoVentaDto.ProductoVenta;
import com.example.boticadesarrolloweb.model.PuntoVentaDto.VentaRequest;
import com.example.boticadesarrolloweb.model.PuntoVentaDto.VentaResponse;
import com.example.boticadesarrolloweb.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lógica del Punto de Venta: búsqueda de productos vendibles y registro de ventas.
 * El stock se descuenta de los lotes no vencidos en orden FEFO (primero el que vence antes),
 * generando un detalle de venta por cada lote utilizado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PuntoVentaService {

    private static final int MAX_RESULTADOS = 20;

    private final ProductoRepository productoRepository;
    private final LoteRepository loteRepository;
    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final UsuarioRepository usuarioRepository;

    /** Error de negocio cuyo mensaje se muestra tal cual al cajero. */
    public static class VentaException extends RuntimeException {
        public VentaException(String mensaje) {
            super(mensaje);
        }
    }

    @Transactional(readOnly = true)
    public List<ProductoVenta> buscarProductos(String q) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        LocalDate hoy = LocalDate.now();
        return productoRepository.buscarParaVenta(q.trim(), PageRequest.of(0, MAX_RESULTADOS)).stream()
                .map(p -> new ProductoVenta(
                        p.getIdProducto(),
                        p.getCodigo(),
                        p.getNombre(),
                        p.getPresentacion(),
                        p.getPrecioVenta(),
                        loteRepository.sumStockVendible(p.getIdProducto(), hoy),
                        p.getRequiereReceta() != null && p.getRequiereReceta() == 1))
                .toList();
    }

    @Transactional
    public VentaResponse registrarVenta(VentaRequest request, Integer idUsuario) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new VentaException("El carrito está vacío.");
        }

        UsuarioEntity usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new VentaException("No se pudo identificar al usuario que realiza la venta."));

        // Agrupar por producto por si el mismo producto llega en varias líneas
        Map<Integer, Integer> cantidades = new LinkedHashMap<>();
        for (ItemVenta item : request.items()) {
            if (item.idProducto() == null || item.cantidad() == null || item.cantidad() <= 0) {
                throw new VentaException("Hay una cantidad inválida en el carrito.");
            }
            cantidades.merge(item.idProducto(), item.cantidad(), Integer::sum);
        }

        VentaEntity venta = ventaRepository.save(VentaEntity.builder()
                .usuario(usuario)
                .tipoComprobante("BOLETA")
                .metodoPago(request.metodoPago() != null ? request.metodoPago() : MetodoPago.EFECTIVO)
                .estado(EstadoVenta.REGISTRADA)
                .total(BigDecimal.ZERO)
                .build());

        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Integer, Integer> entry : cantidades.entrySet()) {
            ProductoEntity producto = productoRepository.findById(entry.getKey())
                    .orElseThrow(() -> new VentaException("Uno de los productos ya no está disponible."));
            // El precio se toma siempre de la BD, nunca del navegador
            total = total.add(descontarStockFefo(venta, producto, entry.getValue(), producto.getPrecioVenta()));
        }

        venta.setTotal(total);
        venta.setNumeroComprobante(String.format("B001-%08d", venta.getIdVenta()));

        log.info("Venta {} registrada por '{}': total S/ {} ({})",
                venta.getNumeroComprobante(), usuario.getUsuario(), total, venta.getMetodoPago());

        return new VentaResponse(venta.getIdVenta(), venta.getNumeroComprobante(), total,
                venta.getMetodoPago().getDescripcion());
    }

    /**
     * Descuenta {@code cantidad} unidades del producto de sus lotes no vencidos en orden FEFO
     * (bloqueándolos), creando un detalle de venta por cada lote usado. Devuelve el subtotal.
     * Lo usan el Punto de Venta y el módulo Ventas.
     */
    @Transactional
    public BigDecimal descontarStockFefo(VentaEntity venta, ProductoEntity producto, int cantidad, BigDecimal precio) {
        List<LoteEntity> lotes = loteRepository.findLotesVendiblesParaVenta(producto.getIdProducto(), LocalDate.now());
        int disponible = lotes.stream().mapToInt(LoteEntity::getStockActual).sum();
        if (disponible < cantidad) {
            throw new VentaException("Stock insuficiente para \"" + producto.getNombre()
                    + "\": disponible " + disponible + ", solicitado " + cantidad + ".");
        }

        BigDecimal total = BigDecimal.ZERO;
        int porVender = cantidad;
        for (LoteEntity lote : lotes) {
            if (porVender == 0) break;
            int tomar = Math.min(porVender, lote.getStockActual());
            lote.setStockActual(lote.getStockActual() - tomar);
            porVender -= tomar;

            BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(tomar));
            detalleVentaRepository.save(DetalleVentaEntity.builder()
                    .venta(venta)
                    .producto(producto)
                    .lote(lote)
                    .cantidad(tomar)
                    .precioUnitario(precio)
                    .subtotal(subtotal)
                    .build());
            total = total.add(subtotal);
        }
        return total;
    }
}
