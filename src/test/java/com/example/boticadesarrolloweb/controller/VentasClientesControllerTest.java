package com.example.boticadesarrolloweb.controller;

import com.example.boticadesarrolloweb.entity.ClienteEntity;
import com.example.boticadesarrolloweb.model.Cliente;
import com.example.boticadesarrolloweb.model.PuntoVentaDto.ProductoVenta;
import com.example.boticadesarrolloweb.repository.ClienteRepository;
import com.example.boticadesarrolloweb.repository.VentaRepository;
import com.example.boticadesarrolloweb.service.ClienteService;
import com.example.boticadesarrolloweb.service.PuntoVentaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.boticadesarrolloweb.service.GestionVentaServiceTest.rucDePrueba;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Flujo HTTP real de Clientes y Ventas (formularios, binding, CSRF, vistas).
 * Se autentica como "admin" con spring-security-test (user(...)) y hace ROLLBACK de todo al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VentasClientesControllerTest {

    /** Usuario "admin" (creado por DataInitializer) autenticado como ADMINISTRADOR. */
    private static final RequestPostProcessor ADMIN = user("admin").roles("ADMINISTRADOR");

    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"");

    @Autowired MockMvc mvc;
    @Autowired ClienteService clienteService;
    @Autowired ClienteRepository clienteRepository;
    @Autowired VentaRepository ventaRepository;
    @Autowired PuntoVentaService puntoVentaService;

    private MockHttpSession session;
    private String csrf;

    @BeforeEach
    void abrirSesion() throws Exception {
        session = new MockHttpSession();
        String html = mvc.perform(get("/ventas").session(session).with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Gestión de Ventas")))
                .andReturn().getResponse().getContentAsString();
        Matcher m = CSRF.matcher(html);
        assertTrue(m.find());
        csrf = m.group(1);
    }

    @Test
    void registraClienteConRucValido() throws Exception {
        String ruc = rucDePrueba();
        mvc.perform(post("/clientes/guardar").session(session).with(ADMIN).param("_csrf", csrf)
                        .param("ruc", ruc).param("razonSocial", "Clínica de Prueba S.A.C.").param("estado", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clientes"));
        assertTrue(clienteRepository.findByRuc(ruc).isPresent());
    }

    @Test
    void rechazaClienteConRucInvalido() throws Exception {
        mvc.perform(post("/clientes/guardar").session(session).with(ADMIN).param("_csrf", csrf)
                        .param("ruc", "20100070971").param("razonSocial", "Empresa X").param("estado", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("clientes"))
                .andExpect(model().attributeHasFieldErrors("cliente", "ruc"))
                .andExpect(model().attribute("modalAbrir", "nuevo"));
        assertTrue(clienteRepository.findByRuc("20100070971").isEmpty());
    }

    @Test
    void registraFacturaDesdeElFormularioYLuegoAbreSuEdicion() throws Exception {
        ProductoVenta p = puntoVentaService.buscarProductos("a").stream().filter(x -> x.stock() >= 2).findFirst().orElse(null);
        assumeTrue(p != null, "No hay productos con stock");
        ClienteEntity c = clienteService.guardar(Cliente.builder().ruc(rucDePrueba()).razonSocial("Cliente Factura SAC").build());

        mvc.perform(post("/ventas/guardar").session(session).with(ADMIN).param("_csrf", csrf)
                        .param("idCliente", c.getIdCliente().toString())
                        .param("metodoPago", "TRANSFERENCIA")
                        .param("detalles[0].idProducto", p.idProducto().toString())
                        .param("detalles[0].cantidad", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", containsString("Factura F001-")));

        Integer idVenta = ventaRepository.findAll().stream()
                .filter(v -> v.getCliente() != null && v.getCliente().getIdCliente().equals(c.getIdCliente()))
                .map(v -> v.getIdVenta()).findFirst().orElseThrow();

        String html = mvc.perform(get("/ventas/editar/" + idVenta).session(session).with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(model().attribute("modalAbrir", "editar"))
                .andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("Editar Venta F001-"));
        assertTrue(html.contains("Actualizar Venta"));
        assertTrue(html.contains(p.nombre()));

        // El listado muestra la factura con su cliente
        mvc.perform(get("/ventas").param("q", c.getRuc()).session(session).with(ADMIN))
                .andExpect(content().string(containsString("Cliente Factura SAC")));
    }

    @Test
    void facturaSinClienteVuelveAlModalConElError() throws Exception {
        ProductoVenta p = puntoVentaService.buscarProductos("a").stream().filter(x -> x.stock() >= 1).findFirst().orElse(null);
        assumeTrue(p != null, "No hay productos con stock");
        mvc.perform(post("/ventas/guardar").session(session).with(ADMIN).param("_csrf", csrf)
                        .param("metodoPago", "EFECTIVO")
                        .param("detalles[0].idProducto", p.idProducto().toString())
                        .param("detalles[0].cantidad", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("ventas"))
                .andExpect(model().attribute("modalAbrir", "nuevo"))
                .andExpect(model().attribute("error", containsString("cliente")))
                .andExpect(content().string(containsString(p.nombre())));
    }

    @Test
    void paginaDeClientesCarga() throws Exception {
        mvc.perform(get("/clientes").session(session).with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Gestión de Clientes")));
    }
}
