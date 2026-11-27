package com.example.boticadesarrolloweb.service;

import com.example.boticadesarrolloweb.entity.DetalleVentaEntity;
import com.example.boticadesarrolloweb.entity.LoteEntity;
import com.example.boticadesarrolloweb.entity.VentaEntity;
import com.example.boticadesarrolloweb.enums.MetodoPago;
import com.example.boticadesarrolloweb.model.PuntoVentaDto.ItemVenta;
import com.example.boticadesarrolloweb.model.PuntoVentaDto.ProductoVenta;
import com.example.boticadesarrolloweb.model.PuntoVentaDto.VentaRequest;
import com.example.boticadesarrolloweb.model.PuntoVentaDto.VentaResponse;
import com.example.boticadesarrolloweb.repository.DetalleVentaRepository;
import com.example.boticadesarrolloweb.repository.LoteRepository;
import com.example.boticadesarrolloweb.repository.UsuarioRepository;
import com.example.boticadesarrolloweb.repository.VentaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Prueba de integración contra la BD configurada. @Transactional hace ROLLBACK al final
 * de cada test, así que no quedan ventas ni cambios de stock en la base compartida.
 */
@SpringBootTest
@Transactional
class PuntoVentaServiceTest {

    @Autowired PuntoVentaService puntoVentaService;
    @Autowired LoteRepository loteRepository;
    @Autowired VentaRepository ventaRepository;
    @Autowired DetalleVentaRepository detalleVentaRepository;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired EntityManager em;

    private Integer idAdmin() {
        return usuarioRepository.findByUsuario("admin").orElseThrow().getIdUsuario();
    }

    /** Un producto con al menos 2 unidades vendibles. */
    private ProductoVenta productoConStock() {
        List<ProductoVenta> candidatos = puntoVentaService.buscarProductos("a").stream()
                .filter(p -> p.stock() >= 2).toList();
        assumeTrue(!candidatos.isEmpty(), "No hay productos con stock para probar");
        return candidatos.get(0);
    }

    @Test
    void registraVentaYDescuentaStockFefo() {
        ProductoVenta p = productoConStock();
        LocalDate hoy = LocalDate.now();
        int stockAntes = loteRepository.sumStockVendible(p.idProducto(), hoy);

        VentaResponse r = puntoVentaService.registrarVenta(
                new VentaRequest(MetodoPago.YAPE, List.of(new ItemVenta(p.idProducto(), 2))), idAdmin());
        em.flush();
        em.clear();

        assertEquals(p.precio().multiply(BigDecimal.valueOf(2)), r.total());
        assertTrue(r.numeroComprobante().startsWith("B001-"));
        assertEquals(stockAntes - 2, loteRepository.sumStockVendible(p.idProducto(), hoy));

        VentaEntity venta = ventaRepository.findById(r.idVenta()).orElseThrow();
        assertEquals(MetodoPago.YAPE, venta.getMetodoPago());
        assertEquals(0, r.total().compareTo(venta.getTotal()));
        assertNotNull(venta.getFechaVenta(), "fecha_venta debe tomar el valor por defecto de la BD");

        List<DetalleVentaEntity> detalles = detalleVentaRepository.findAll().stream()
                .filter(d -> d.getVenta().getIdVenta().equals(r.idVenta())).toList();
        assertEquals(2, detalles.stream().mapToInt(DetalleVentaEntity::getCantidad).sum());

        // FEFO: el primer detalle sale del lote vendible que vence antes
        LoteEntity primerLote = detalles.get(0).getLote();
        loteRepository.findByProductoIdProductoOrderByFechaVencimientoAsc(p.idProducto()).stream()
                .filter(l -> !l.getFechaVencimiento().isBefore(hoy))
                .filter(l -> l.getStockActual() > 0)   // FEFO solo considera lotes con stock
                .findFirst()
                .ifPresent(l -> assertFalse(l.getFechaVencimiento().isBefore(primerLote.getFechaVencimiento())));
    }

    @Test
    void rechazaVentaSiNoHayStockSuficiente() {
        ProductoVenta p = productoConStock();
        var ex = assertThrows(PuntoVentaService.VentaException.class, () ->
                puntoVentaService.registrarVenta(
                        new VentaRequest(MetodoPago.EFECTIVO, List.of(new ItemVenta(p.idProducto(), p.stock() + 1))),
                        idAdmin()));
        assertTrue(ex.getMessage().contains("Stock insuficiente"));
    }

    @Test
    void rechazaCarritoVacio() {
        assertThrows(PuntoVentaService.VentaException.class, () ->
                puntoVentaService.registrarVenta(new VentaRequest(MetodoPago.EFECTIVO, List.of()), idAdmin()));
    }
}
