package com.example.boticadesarrolloweb.controller;

import com.example.boticadesarrolloweb.entity.ProductoEntity;
import com.example.boticadesarrolloweb.repository.CompraRepository;
import com.example.boticadesarrolloweb.repository.LoteRepository;
import com.example.boticadesarrolloweb.repository.ProductoRepository;
import com.example.boticadesarrolloweb.repository.ProveedorRepository;
import com.example.boticadesarrolloweb.repository.UsuarioRepository;
import com.example.boticadesarrolloweb.service.AlertaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prueba el flujo HTTP real del formulario de compras (binding de detalles[i].*, CSRF, vistas).
 * Usa el bypass de desarrollo para autenticarse como "admin" (MockMvc llama desde 127.0.0.1).
 * @Transactional: todo lo que se guarda se deshace al terminar cada test.
 */
@SpringBootTest(properties = "app.security.dev-bypass=true")
@AutoConfigureMockMvc
@Transactional
class ComprasControllerTest {

    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"");

    @Autowired MockMvc mvc;
    @Autowired AlertaService alertaService;
    @Autowired ProductoRepository productoRepository;
    @Autowired ProveedorRepository proveedorRepository;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired LoteRepository loteRepository;
    @Autowired CompraRepository compraRepository;

    private MockHttpSession session;
    private String csrf;

    @BeforeEach
    void abrirSesion() throws Exception {
        session = new MockHttpSession();
        String html = mvc.perform(get("/compras").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Matcher m = CSRF.matcher(html);
        assertTrue(m.find(), "La página debe incluir el token CSRF");
        csrf = m.group(1);
    }

    private MockHttpServletRequestBuilder formularioCompra() {
        var proveedor = proveedorRepository.findAll().stream().findFirst();
        assumeTrue(proveedor.isPresent(), "No hay proveedores para probar");
        return post("/compras/guardar").session(session)
                .param("_csrf", csrf)
                .param("idProveedor", proveedor.get().getIdProveedor().toString())
                .param("idUsuario", usuarioRepository.findByUsuario("admin").orElseThrow().getIdUsuario().toString())
                .param("tipoComprobante", "FA")
                .param("numeroComprobante", "F001-TEST");
    }

    @Test
    void desdeAlertasAbreModalConProductosPrecargados() throws Exception {
        var alertas = alertaService.listarProductosBajoStockMinimo();
        assumeTrue(!alertas.isEmpty(), "No hay productos en alerta");
        var item = alertas.get(0);
        int sugerida = 2 * item.getProducto().getStockMinimo() - item.getStockTotal();

        mvc.perform(get("/compras").param("alerta", item.getProducto().getIdProducto().toString()).session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("modalAbrir", "nuevo"))
                .andExpect(content().string(containsString("data-abrir=\"true\"")))
                .andExpect(content().string(containsString(item.getProducto().getNombre())))
                .andExpect(content().string(containsString("value=\"" + sugerida + "\"")));
    }

    @Test
    void guardaCompraConVariosProductosDesdeElFormulario() throws Exception {
        List<ProductoEntity> productos = productoRepository.findAll().stream().limit(2).toList();
        assumeTrue(productos.size() == 2, "Se necesitan 2 productos");
        ProductoEntity a = productos.get(0), b = productos.get(1);
        int stockA = loteRepository.sumStockByProductoId(a.getIdProducto());
        int stockB = loteRepository.sumStockByProductoId(b.getIdProducto());
        String venc = LocalDate.now().plusYears(2).toString();

        mvc.perform(formularioCompra()
                        .param("detalles[0].idProducto", a.getIdProducto().toString())
                        .param("detalles[0].cantidad", "7")
                        .param("detalles[0].precioUnitario", "2.50")
                        .param("detalles[0].numeroLote", "")
                        .param("detalles[0].fechaVencimiento", venc)
                        .param("detalles[1].idProducto", b.getIdProducto().toString())
                        .param("detalles[1].cantidad", "3")
                        .param("detalles[1].precioUnitario", "4.00")
                        .param("detalles[1].numeroLote", "TEST-MVC-1")
                        .param("detalles[1].fechaVencimiento", venc))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/compras"))
                .andExpect(flash().attribute("mensaje", containsString("2 producto(s) ingresaron al inventario")));

        assertEquals(stockA + 7, loteRepository.sumStockByProductoId(a.getIdProducto()));
        assertEquals(stockB + 3, loteRepository.sumStockByProductoId(b.getIdProducto()));
    }

    @Test
    void editarAbreElMismoModalConLosProductosDeLaCompra() throws Exception {
        ProductoEntity p = productoRepository.findAll().stream().findFirst().orElseThrow();
        mvc.perform(formularioCompra()
                        .param("detalles[0].idProducto", p.getIdProducto().toString())
                        .param("detalles[0].cantidad", "4")
                        .param("detalles[0].precioUnitario", "1.50")
                        .param("detalles[0].numeroLote", "TEST-EDIT-1")
                        .param("detalles[0].fechaVencimiento", LocalDate.now().plusYears(1).toString()))
                .andExpect(status().is3xxRedirection());
        Integer idCompra = compraRepository.findAll().stream()
                .filter(c -> "F001-TEST".equals(c.getNumeroComprobante()))
                .map(c -> c.getIdCompra()).max(Integer::compareTo).orElseThrow();

        String html = mvc.perform(get("/compras/editar/" + idCompra).session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("modalAbrir", "editar"))
                .andReturn().getResponse().getContentAsString();

        assertTrue(html.contains("Editar Compra CMP-"), "Título de edición");
        assertTrue(html.contains("Actualizar Compra"), "Botón de edición");
        assertTrue(html.contains("TEST-EDIT-1"), "Fila con el lote de la compra");
        assertTrue(html.contains(p.getNombre()), "Fila con el producto de la compra");
        assertFalse(html.contains("modalEditarCompra"), "El modal antiguo ya no existe");
    }

    @Test
    void errorDeNegocioVuelveAlModalSinPerderLosProductos() throws Exception {
        ProductoEntity p = productoRepository.findAll().stream().findFirst().orElseThrow();
        var loteExistente = loteRepository.findByProductoIdProductoOrderByFechaVencimientoAsc(p.getIdProducto())
                .stream().findFirst();
        assumeTrue(loteExistente.isPresent(), "El producto necesita un lote existente");
        int stockAntes = loteRepository.sumStockByProductoId(p.getIdProducto());

        mvc.perform(formularioCompra()
                        .param("detalles[0].idProducto", p.getIdProducto().toString())
                        .param("detalles[0].cantidad", "5")
                        .param("detalles[0].precioUnitario", "1.00")
                        .param("detalles[0].numeroLote", loteExistente.get().getNumeroLote())
                        .param("detalles[0].fechaVencimiento", LocalDate.now().plusYears(1).toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("compras"))
                .andExpect(model().attribute("modalAbrir", "nuevo"))
                .andExpect(model().attribute("error", containsString("ya existe")))
                .andExpect(content().string(containsString(p.getNombre())));

        assertEquals(stockAntes, loteRepository.sumStockByProductoId(p.getIdProducto()));
    }
}
