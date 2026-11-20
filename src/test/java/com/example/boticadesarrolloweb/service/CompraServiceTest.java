package com.example.boticadesarrolloweb.service;

import com.example.boticadesarrolloweb.entity.CompraEntity;
import com.example.boticadesarrolloweb.entity.ProductoEntity;
import com.example.boticadesarrolloweb.enums.EstadoCompra;
import com.example.boticadesarrolloweb.model.Compra;
import com.example.boticadesarrolloweb.model.DetalleCompra;
import com.example.boticadesarrolloweb.model.InventarioItemDto;
import com.example.boticadesarrolloweb.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Prueba de integración contra la BD configurada. @Transactional hace ROLLBACK al final
 * de cada test: no quedan compras, lotes ni cambios de stock en la base compartida.
 */
@SpringBootTest
@Transactional
class CompraServiceTest {

    @Autowired CompraService compraService;
    @Autowired AlertaService alertaService;
    @Autowired ProductoRepository productoRepository;
    @Autowired ProveedorRepository proveedorRepository;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired LoteRepository loteRepository;
    @Autowired DetalleCompraRepository detalleCompraRepository;
    @Autowired EntityManager em;

    private Compra cabecera(List<DetalleCompra> detalles) {
        var proveedor = proveedorRepository.findAll().stream().findFirst();
        assumeTrue(proveedor.isPresent(), "No hay proveedores para probar");
        return Compra.builder()
                .idProveedor(proveedor.get().getIdProveedor())
                .idUsuario(usuarioRepository.findByUsuario("admin").orElseThrow().getIdUsuario())
                .tipoComprobante("FA")
                .numeroComprobante("TEST-0001")
                .detalles(new ArrayList<>(detalles))
                .build();
    }

    private DetalleCompra linea(ProductoEntity p, int cantidad, String precio) {
        return DetalleCompra.builder()
                .idProducto(p.getIdProducto())
                .cantidad(cantidad)
                .precioUnitario(new BigDecimal(precio))
                .fechaVencimiento(LocalDate.now().plusYears(2))
                .build();
    }

    @Test
    void compraVariosProductosCreaLotesYSumaStock() {
        List<ProductoEntity> productos = productoRepository.findAll().stream().limit(2).toList();
        assumeTrue(productos.size() == 2, "Se necesitan 2 productos");
        ProductoEntity a = productos.get(0), b = productos.get(1);
        int stockA = loteRepository.sumStockByProductoId(a.getIdProducto());
        int stockB = loteRepository.sumStockByProductoId(b.getIdProducto());

        DetalleCompra lineaB = linea(b, 5, "2.00");
        lineaB.setNumeroLote("TEST-LOTE-XYZ");
        CompraEntity compra = compraService.guardarCompra(cabecera(List.of(linea(a, 10, "3.50"), lineaB)));
        em.flush();
        em.clear();

        assertEquals(0, new BigDecimal("45.00").compareTo(compra.getTotal()));
        assertEquals(stockA + 10, loteRepository.sumStockByProductoId(a.getIdProducto()));
        assertEquals(stockB + 5, loteRepository.sumStockByProductoId(b.getIdProducto()));

        var detalles = detalleCompraRepository.findByCompraConLote(compra.getIdCompra());
        assertEquals(2, detalles.size());
        assertTrue(detalles.stream().anyMatch(d -> d.getLote().getNumeroLote().equals("TEST-LOTE-XYZ")));
        assertTrue(detalles.stream().anyMatch(d -> d.getLote().getNumeroLote().startsWith("LT-")),
                "Lote sin número debe generarse automáticamente");

        // Anular retira el stock que ingresó
        compraService.anularCompra(compra.getIdCompra());
        em.flush();
        em.clear();
        assertEquals(stockA, loteRepository.sumStockByProductoId(a.getIdProducto()));
        assertEquals(EstadoCompra.ANULADA, compraService.buscarPorId(compra.getIdCompra()).getEstado());
    }

    @Test
    void detallesDesdeAlertasSugierenDobleDelMinimo() {
        List<InventarioItemDto> alertas = alertaService.listarProductosBajoStockMinimo();
        assumeTrue(!alertas.isEmpty(), "No hay productos en alerta");

        List<DetalleCompra> detalles = compraService.detallesDesdeAlertas(
                alertas.stream().map(i -> i.getProducto().getIdProducto()).toList());

        assertEquals(alertas.size(), detalles.size());
        for (DetalleCompra d : detalles) {
            assertTrue(d.getCantidad() > d.getStockMinimo(), "La cantidad debe superar el stock mínimo");
            assertEquals(2 * d.getStockMinimo() - d.getStockActual(), d.getCantidad());
            assertEquals(d.getStockMinimo() + 1, d.getCantidadMinima());
        }
    }

    @Test
    void rechazaCantidadNoMayorAlMinimoDesdeAlertas() {
        List<InventarioItemDto> alertas = alertaService.listarProductosBajoStockMinimo();
        assumeTrue(!alertas.isEmpty(), "No hay productos en alerta");
        DetalleCompra d = compraService.detallesDesdeAlertas(List.of(alertas.get(0).getProducto().getIdProducto())).get(0);
        d.setCantidad(d.getStockMinimo()); // igual al mínimo: no permitido
        d.setFechaVencimiento(LocalDate.now().plusYears(1));

        var ex = assertThrows(CompraService.CompraException.class,
                () -> compraService.guardarCompra(cabecera(List.of(d))));
        assertTrue(ex.getMessage().contains("mayor a su stock mínimo"));
    }

    @Test
    void editarCompraAjustaElInventario() {
        List<ProductoEntity> productos = productoRepository.findAll().stream().limit(3).toList();
        assumeTrue(productos.size() == 3, "Se necesitan 3 productos");
        ProductoEntity a = productos.get(0), b = productos.get(1), c = productos.get(2);
        int stockA = loteRepository.sumStockByProductoId(a.getIdProducto());
        int stockB = loteRepository.sumStockByProductoId(b.getIdProducto());
        int stockC = loteRepository.sumStockByProductoId(c.getIdProducto());

        CompraEntity creada = compraService.guardarCompra(cabecera(List.of(linea(a, 10, "1.00"), linea(b, 4, "1.00"))));
        em.flush();
        em.clear();

        // Editar: A pasa de 10 a 6, se quita B y se agrega C x 5
        Compra edicion = compraService.cargarParaEditar(creada.getIdCompra());
        assertEquals(2, edicion.getDetalles().size());
        DetalleCompra lineaA = edicion.getDetalles().stream()
                .filter(d -> d.getIdProducto().equals(a.getIdProducto())).findFirst().orElseThrow();
        lineaA.setCantidad(6);
        lineaA.setPrecioUnitario(new BigDecimal("2.00"));
        edicion.setDetalles(new ArrayList<>(List.of(lineaA, linea(c, 5, "3.00"))));

        CompraEntity editada = compraService.guardarCompra(edicion);
        em.flush();
        em.clear();

        assertEquals(0, new BigDecimal("27.00").compareTo(editada.getTotal())); // 6x2 + 5x3
        assertEquals(stockA + 6, loteRepository.sumStockByProductoId(a.getIdProducto()));
        assertEquals(stockB, loteRepository.sumStockByProductoId(b.getIdProducto()));
        assertEquals(stockC + 5, loteRepository.sumStockByProductoId(c.getIdProducto()));
        assertEquals(2, detalleCompraRepository.findByCompraConLote(creada.getIdCompra()).size());
    }

    @Test
    void editarNoPermiteBajarNiQuitarLoQueYaSeVendio() {
        ProductoEntity a = productoRepository.findAll().stream().findFirst().orElseThrow();
        CompraEntity creada = compraService.guardarCompra(cabecera(List.of(linea(a, 10, "1.00"))));
        em.flush();

        // Simular que se vendieron 4 unidades del lote de la compra
        var lote = detalleCompraRepository.findByCompraConLote(creada.getIdCompra()).get(0).getLote();
        lote.setStockActual(6);
        em.flush();
        em.clear();

        Compra edicion = compraService.cargarParaEditar(creada.getIdCompra());
        assertEquals(4, edicion.getDetalles().get(0).getCantidadVendida());

        edicion.getDetalles().get(0).setCantidad(3);
        var ex = assertThrows(CompraService.CompraException.class, () -> compraService.guardarCompra(edicion));
        assertTrue(ex.getMessage().contains("no puede ser menor a 4"));

        Compra sinLinea = compraService.cargarParaEditar(creada.getIdCompra());
        sinLinea.setDetalles(new ArrayList<>(List.of(linea(productoRepository.findAll().get(1), 2, "1.00"))));
        var ex2 = assertThrows(CompraService.CompraException.class, () -> compraService.guardarCompra(sinLinea));
        assertTrue(ex2.getMessage().contains("No se puede quitar"));
    }

    @Test
    void compraAntiguaSinProductosConservaSuTotalManual() {
        CompraEntity antigua = new CompraEntity();
        var base = cabecera(List.of());
        antigua.setProveedor(proveedorRepository.findById(base.getIdProveedor()).orElseThrow());
        antigua.setUsuario(usuarioRepository.findById(base.getIdUsuario()).orElseThrow());
        antigua.setTotal(new BigDecimal("50.00"));
        antigua = em.merge(antigua);
        em.flush();

        assertTrue(compraService.esCompraSinProductos(antigua.getIdCompra()));
        Compra edicion = compraService.cargarParaEditar(antigua.getIdCompra());
        edicion.setTotal(new BigDecimal("75.50"));
        assertEquals(0, new BigDecimal("75.50").compareTo(compraService.guardarCompra(edicion).getTotal()));
    }

    @Test
    void rechazaCompraSinProductos() {
        assertThrows(CompraService.CompraException.class,
                () -> compraService.guardarCompra(cabecera(List.of())));
    }
}
