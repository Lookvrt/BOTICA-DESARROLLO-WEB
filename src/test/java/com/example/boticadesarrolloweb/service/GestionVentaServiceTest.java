package com.example.boticadesarrolloweb.service;

import com.example.boticadesarrolloweb.entity.ClienteEntity;
import com.example.boticadesarrolloweb.entity.VentaEntity;
import com.example.boticadesarrolloweb.enums.EstadoVenta;
import com.example.boticadesarrolloweb.enums.MetodoPago;
import com.example.boticadesarrolloweb.model.Cliente;
import com.example.boticadesarrolloweb.model.DetalleVenta;
import com.example.boticadesarrolloweb.model.PuntoVentaDto;
import com.example.boticadesarrolloweb.model.PuntoVentaDto.ProductoVenta;
import com.example.boticadesarrolloweb.model.Venta;
import com.example.boticadesarrolloweb.repository.*;
import com.example.boticadesarrolloweb.validation.RucValido;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Pruebas de integración de Clientes y del módulo Ventas contra la BD configurada.
 * @Transactional: ROLLBACK al final de cada test, no quedan clientes, ventas ni cambios de stock.
 */
@SpringBootTest
@Transactional
public class GestionVentaServiceTest {

    @Autowired GestionVentaService gestionVentaService;
    @Autowired PuntoVentaService puntoVentaService;
    @Autowired ClienteService clienteService;
    @Autowired LoteRepository loteRepository;
    @Autowired ProductoRepository productoRepository;
    @Autowired VentaRepository ventaRepository;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired EntityManager em;

    /** RUC ficticio válido (prefijo 20 + 8 dígitos al azar + dígito verificador). */
    public static String rucDePrueba() {
        String base = String.format("20%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        return base + RucValido.Validador.calcularDigitoVerificador(base);
    }

    private ClienteEntity cliente() {
        return clienteService.guardar(Cliente.builder().ruc(rucDePrueba()).razonSocial("Empresa de Prueba S.A.C.").build());
    }

    private Integer idAdmin() {
        return usuarioRepository.findByUsuario("admin").orElseThrow().getIdUsuario();
    }

    private ProductoVenta productoConStock(int minimo) {
        List<ProductoVenta> candidatos = puntoVentaService.buscarProductos("a").stream()
                .filter(p -> p.stock() >= minimo).toList();
        assumeTrue(!candidatos.isEmpty(), "No hay productos con stock suficiente");
        return candidatos.get(0);
    }

    private int stock(Integer idProducto) {
        return loteRepository.sumStockVendible(idProducto, LocalDate.now());
    }

    private Venta factura(Integer idCliente, Integer idProducto, int cantidad) {
        return Venta.builder()
                .idCliente(idCliente)
                .metodoPago(MetodoPago.TRANSFERENCIA)
                .detalles(new ArrayList<>(List.of(DetalleVenta.builder().idProducto(idProducto).cantidad(cantidad).build())))
                .build();
    }

    // ── Clientes ──

    @Test
    void clienteConRucRepetidoSeRechaza() {
        ClienteEntity c = cliente();
        var ex = assertThrows(ClienteService.ClienteException.class, () -> clienteService.guardar(
                Cliente.builder().ruc(c.getRuc()).razonSocial("Otra").build()));
        assertTrue(ex.getMessage().contains("Ya existe un cliente"));
    }

    @Test
    void clienteConVentasSeDesactivaEnLugarDeEliminarse() {
        ProductoVenta p = productoConStock(1);
        ClienteEntity conVentas = cliente();
        gestionVentaService.guardar(factura(conVentas.getIdCliente(), p.idProducto(), 1), idAdmin());
        ClienteEntity sinVentas = cliente();
        em.flush();

        assertFalse(clienteService.eliminar(conVentas.getIdCliente()));
        assertEquals(0, clienteService.buscarPorId(conVentas.getIdCliente()).getEstado());
        assertTrue(clienteService.eliminar(sinVentas.getIdCliente()));
    }

    // ── Ventas ──

    @Test
    void registraFacturaConCorrelativoYDescuentaStock() {
        ProductoVenta p = productoConStock(3);
        ClienteEntity c = cliente();
        int antes = stock(p.idProducto());

        VentaEntity f1 = gestionVentaService.guardar(factura(c.getIdCliente(), p.idProducto(), 2), idAdmin());
        VentaEntity f2 = gestionVentaService.guardar(factura(c.getIdCliente(), p.idProducto(), 1), idAdmin());
        em.flush();

        assertEquals("FACTURA", f1.getTipoComprobante());
        assertTrue(f1.getNumeroComprobante().matches("F001-\\d{8}"));
        assertEquals(Integer.parseInt(f1.getNumeroComprobante().substring(5)) + 1,
                Integer.parseInt(f2.getNumeroComprobante().substring(5)), "Correlativo consecutivo");
        assertEquals(c.getIdCliente(), f1.getCliente().getIdCliente());
        assertEquals(0, p.precio().multiply(BigDecimal.valueOf(2)).compareTo(f1.getTotal()));
        assertEquals(antes - 3, stock(p.idProducto()));
    }

    @Test
    void facturaSinClienteSeRechaza() {
        ProductoVenta p = productoConStock(1);
        var ex = assertThrows(PuntoVentaService.VentaException.class,
                () -> gestionVentaService.guardar(factura(null, p.idProducto(), 1), idAdmin()));
        assertTrue(ex.getMessage().contains("cliente"));
    }

    @Test
    void editarAjustaStockYConservaElPrecioOriginal() {
        ProductoVenta p = productoConStock(5);
        ClienteEntity c = cliente();
        int antes = stock(p.idProducto());
        VentaEntity venta = gestionVentaService.guardar(factura(c.getIdCliente(), p.idProducto(), 2), idAdmin());
        em.flush();

        // El precio del producto sube después de la venta
        var producto = productoRepository.findById(p.idProducto()).orElseThrow();
        producto.setPrecioVenta(p.precio().add(new BigDecimal("10.00")));
        em.flush();
        em.clear();

        Venta edicion = gestionVentaService.cargarParaEditar(venta.getIdVenta());
        assertEquals(1, edicion.getDetalles().size());
        assertEquals(2, edicion.getDetalles().get(0).getCantidad());
        assertEquals(antes, edicion.getDetalles().get(0).getStockDisponible(), "Disponible incluye lo de esta venta");

        edicion.getDetalles().get(0).setCantidad(4);
        edicion.setMetodoPago(MetodoPago.YAPE);
        VentaEntity editada = gestionVentaService.guardar(edicion, idAdmin());
        em.flush();

        assertEquals(antes - 4, stock(p.idProducto()));
        assertEquals(0, p.precio().multiply(BigDecimal.valueOf(4)).compareTo(editada.getTotal()), "Precio original");
        assertEquals(MetodoPago.YAPE, editada.getMetodoPago());
        assertEquals(venta.getNumeroComprobante(), editada.getNumeroComprobante(), "El número no cambia al editar");
    }

    @Test
    void anularDevuelveElStock() {
        ProductoVenta p = productoConStock(2);
        int antes = stock(p.idProducto());
        VentaEntity venta = gestionVentaService.guardar(factura(cliente().getIdCliente(), p.idProducto(), 2), idAdmin());
        em.flush();
        assertEquals(antes - 2, stock(p.idProducto()));

        gestionVentaService.anular(venta.getIdVenta());
        em.flush();
        assertEquals(antes, stock(p.idProducto()));
        assertEquals(EstadoVenta.ANULADA, ventaRepository.findById(venta.getIdVenta()).orElseThrow().getEstado());
        assertThrows(PuntoVentaService.VentaException.class, () -> gestionVentaService.cargarParaEditar(venta.getIdVenta()));
    }

    @Test
    void boletaDelPuntoDeVentaSeEditaSinCliente() {
        ProductoVenta p = productoConStock(3);
        var r = puntoVentaService.registrarVenta(new PuntoVentaDto.VentaRequest(MetodoPago.EFECTIVO,
                List.of(new PuntoVentaDto.ItemVenta(p.idProducto(), 1))), idAdmin());
        em.flush();
        em.clear();

        Venta edicion = gestionVentaService.cargarParaEditar(r.idVenta());
        assertEquals("BOLETA", edicion.getTipoComprobante());
        assertNull(edicion.getIdCliente());
        edicion.getDetalles().get(0).setCantidad(2);
        VentaEntity editada = gestionVentaService.guardar(edicion, idAdmin());
        assertNull(editada.getCliente());
        assertEquals(0, p.precio().multiply(BigDecimal.valueOf(2)).compareTo(editada.getTotal()));
    }

    @Test
    void ventaAntiguaSinProductosConservaSuTotalAlEditarLaCabecera() {
        var vendedor = usuarioRepository.findByUsuario("admin").orElseThrow();
        VentaEntity antigua = ventaRepository.save(VentaEntity.builder()
                .usuario(vendedor).tipoComprobante("BOLETA").numeroComprobante("B999-TEST")
                .metodoPago(MetodoPago.EFECTIVO).total(new BigDecimal("45.50")).build());
        em.flush();
        em.clear();

        assertTrue(gestionVentaService.esVentaSinProductos(antigua.getIdVenta()));
        Venta edicion = gestionVentaService.cargarParaEditar(antigua.getIdVenta());
        assertTrue(edicion.getDetalles().isEmpty());
        edicion.setMetodoPago(MetodoPago.PLIN);

        VentaEntity editada = gestionVentaService.guardar(edicion, idAdmin());
        assertEquals(0, new BigDecimal("45.50").compareTo(editada.getTotal()), "Conserva su total");
        assertEquals(MetodoPago.PLIN, editada.getMetodoPago());
    }

    @Test
    void listadoIncluyeBoletasYFacturas() {
        ProductoVenta p = productoConStock(2);
        ClienteEntity c = cliente();
        VentaEntity f = gestionVentaService.guardar(factura(c.getIdCliente(), p.idProducto(), 1), idAdmin());
        em.flush();

        List<VentaEntity> porRuc = gestionVentaService.listar(c.getRuc());
        assertEquals(1, porRuc.size());
        assertEquals(f.getIdVenta(), porRuc.get(0).getIdVenta());
        assertTrue(gestionVentaService.listar(null).stream().anyMatch(v -> v.getCliente() == null),
                "Las boletas del Punto de Venta (sin cliente) también aparecen");
    }
}
