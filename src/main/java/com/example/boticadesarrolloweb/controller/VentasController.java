package com.example.boticadesarrolloweb.controller;

import com.example.boticadesarrolloweb.entity.VentaEntity;
import com.example.boticadesarrolloweb.enums.MetodoPago;
import com.example.boticadesarrolloweb.model.PuntoVentaDto.ProductoVenta;
import com.example.boticadesarrolloweb.model.Venta;
import com.example.boticadesarrolloweb.service.ClienteService;
import com.example.boticadesarrolloweb.service.GestionVentaService;
import com.example.boticadesarrolloweb.service.PuntoVentaService;
import com.example.boticadesarrolloweb.service.PuntoVentaService.VentaException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Módulo Ventas (/ventas): listado de todas las ventas y facturas a clientes.
 * El Punto de Venta (/venta) sigue siendo la caja rápida con boleta.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/ventas")
public class VentasController {

    private final GestionVentaService gestionVentaService;
    private final PuntoVentaService puntoVentaService;
    private final ClienteService clienteService;

    private void cargarListado(Model model, String q, HttpSession session) {
        model.addAttribute("ventas", gestionVentaService.listar(q));
        model.addAttribute("clientes", clienteService.listarActivos());
        model.addAttribute("metodosPago", MetodoPago.values());
        model.addAttribute("query", q);
        model.addAttribute("responsable", etiquetaResponsable((Venta) model.getAttribute("venta"), session));
    }

    private String etiquetaResponsable(Venta venta, HttpSession session) {
        Integer idResponsable = (venta != null && venta.getIdVenta() != null)
                ? gestionVentaService.idResponsable(venta.getIdVenta())
                : null;
        if (idResponsable == null || idResponsable.equals(session.getAttribute("usuario_autenticado_id"))) {
            return "Tú";
        }
        String nombre = gestionVentaService.nombreUsuario(idResponsable);
        return nombre.isBlank() ? "—" : nombre;
    }

    @GetMapping
    public String ventas(@RequestParam(name = "q", required = false) String q,
                         HttpSession session, Model model) {
        if (!model.containsAttribute("venta")) {
            Venta venta = new Venta();
            venta.setTipoComprobante(GestionVentaService.TIPO_FACTURA);
            model.addAttribute("venta", venta);
        }
        cargarListado(model, q, session);
        return "ventas";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, HttpSession session,
                         Model model, RedirectAttributes redirectAttributes) {
        try {
            Venta venta = gestionVentaService.cargarParaEditar(id);
            model.addAttribute("venta", venta);
            model.addAttribute("modalAbrir", "editar");
            model.addAttribute("ventaSinProductos", venta.getDetalles().isEmpty());
            cargarListado(model, null, session);
            return "ventas";
        } catch (VentaException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ventas";
        }
    }

    @GetMapping("/api/productos")
    @ResponseBody
    public List<ProductoVenta> buscarProductos(@RequestParam(name = "q", required = false) String q) {
        return puntoVentaService.buscarProductos(q);
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("venta") Venta venta, BindingResult resultado,
                          HttpSession session, Authentication authentication,
                          Model model, RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            return volverAlFormulario(venta, session, model, "Revisa los datos ingresados en el formulario.");
        }
        try {
            VentaEntity guardada = gestionVentaService.guardar(venta, resolverUsuario(session, authentication));
            redirectAttributes.addFlashAttribute("mensaje", venta.getIdVenta() == null
                    ? "Factura " + guardada.getNumeroComprobante() + " registrada por S/ " + guardada.getTotal() + "."
                    : "Venta " + guardada.getNumeroComprobante() + " actualizada. Total: S/ " + guardada.getTotal() + ".");
            return "redirect:/ventas";
        } catch (VentaException e) {
            return volverAlFormulario(venta, session, model, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al guardar venta", e);
            redirectAttributes.addFlashAttribute("error", "No se pudo guardar la venta. Intente nuevamente.");
            return "redirect:/ventas";
        }
    }

    private String volverAlFormulario(Venta venta, HttpSession session, Model model, String error) {
        gestionVentaService.completarDatosVista(venta);
        if (venta.getIdVenta() == null) venta.setTipoComprobante(GestionVentaService.TIPO_FACTURA);
        model.addAttribute("venta", venta);
        model.addAttribute("modalAbrir", venta.getIdVenta() == null ? "nuevo" : "editar");
        model.addAttribute("ventaSinProductos", gestionVentaService.esVentaSinProductos(venta.getIdVenta()));
        model.addAttribute("error", error);
        cargarListado(model, null, session);
        return "ventas";
    }

    @PostMapping("/anular/{id}")
    public String anular(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            gestionVentaService.anular(id);
            redirectAttributes.addFlashAttribute("mensaje", "Venta anulada. Sus unidades volvieron al inventario.");
        } catch (VentaException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ventas";
    }

    /** ID del usuario logueado: de la sesión o, si no está (remember-me), por su nombre de usuario. */
    private Integer resolverUsuario(HttpSession session, Authentication authentication) {
        if (session.getAttribute("usuario_autenticado_id") instanceof Integer id) {
            return id;
        }
        if (authentication == null) {
            throw new VentaException("No se pudo identificar al usuario de la sesión.");
        }
        return gestionVentaService.idUsuarioDe(authentication.getName());
    }
}
