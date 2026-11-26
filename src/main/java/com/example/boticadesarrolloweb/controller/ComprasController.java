package com.example.boticadesarrolloweb.controller;

import com.example.boticadesarrolloweb.entity.CompraEntity;
import com.example.boticadesarrolloweb.model.Compra;

import com.example.boticadesarrolloweb.model.DetalleCompra;
import com.example.boticadesarrolloweb.service.CompraService;
import com.example.boticadesarrolloweb.service.CompraService.CompraException;
import com.example.boticadesarrolloweb.service.CompraService.ProductoCompra;
import com.example.boticadesarrolloweb.service.ProveedorService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
public class ComprasController {

    private final CompraService compraService;
    private final ProveedorService proveedorService;

    public ComprasController(
            CompraService compraService,
            ProveedorService proveedorService) {
        this.compraService = compraService;
        this.proveedorService = proveedorService;
    }

    private void cargarListas(Model model, String query, String orden, HttpSession session) {
        model.addAttribute("compras", compraService.listarCompras(query, orden));
        model.addAttribute("proveedores", proveedorService.listarProveedores());
        model.addAttribute("responsable", etiquetaResponsable((Compra) model.getAttribute("compra"), session));
    }

    /** "Tú" si la compra es (o será) del usuario en sesión; si no, el nombre de quien la registró. */
    private String etiquetaResponsable(Compra compra, HttpSession session) {
        Integer idResponsable = null;
        if (compra != null) {
            idResponsable = compra.getIdCompra() != null
                    ? compraService.idResponsable(compra.getIdCompra())
                    : compra.getIdUsuario();
        }
        if (idResponsable == null || idResponsable.equals(session.getAttribute("usuario_autenticado_id"))) {
            return "Tú";
        }
        String nombre = compraService.nombreUsuario(idResponsable);
        return nombre.isBlank() ? "—" : nombre;
    }

    /**
     * Listado de compras. Con ?alerta=1,2,3 (desde el módulo de Alertas) abre el modal de
     * nueva compra con esos productos precargados.
     */
    @GetMapping("/compras")
    public String compras(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "orden", required = false, defaultValue = "asc") String orden,
            @RequestParam(name = "alerta", required = false) List<Integer> idsAlerta,
            HttpSession session,
            Model model) {

        if (!model.containsAttribute("compra")) {
            Compra compra = new Compra();
            if (session.getAttribute("usuario_autenticado_id") instanceof Integer idUsuario) {
                compra.setIdUsuario(idUsuario);
            }
            if (idsAlerta != null && !idsAlerta.isEmpty()) {
                List<DetalleCompra> detalles = compraService.detallesDesdeAlertas(idsAlerta);
                compra.setDetalles(detalles);
                model.addAttribute("modalAbrir", "nuevo");
                if (detalles.isEmpty()) {
                    model.addAttribute("error", "Los productos seleccionados ya no están por debajo de su stock mínimo.");
                }
            }
            model.addAttribute("compra", compra);
        }

        cargarListas(model, query, orden, session);
        model.addAttribute("query", query);
        model.addAttribute("orden", orden);

        return "compras";
    }

    @GetMapping("/compras/api/productos")
    @ResponseBody
    public List<ProductoCompra> buscarProductos(@RequestParam(name = "q", required = false) String q) {
        return compraService.buscarProductos(q);
    }

    @PostMapping("/compras/guardar")
    public String guardar(
            @Valid @ModelAttribute("compra") Compra compra, BindingResult resultado, Model model,
            RedirectAttributes redirectAttributes, HttpSession session, Authentication authentication) {

        // El responsable no viene del formulario: en una compra nueva es el usuario en sesión
        // (en una edición el servicio conserva el original).
        if (compra.getIdCompra() == null) {
            try {
                compra.setIdUsuario(resolverUsuario(session, authentication));
            } catch (CompraException e) {
                return volverAlFormulario(compra, model, e.getMessage(), session);
            }
        }

        if (resultado.hasErrors()) {
            return volverAlFormulario(compra, model, "Revisa los datos ingresados en el formulario.", session);
        }

        try {
            CompraEntity guardada = compraService.guardarCompra(compra);
            redirectAttributes.addFlashAttribute("mensaje", compra.getIdCompra() == null
                    ? "Compra CMP-" + String.format("%03d", guardada.getIdCompra()) + " registrada: "
                    + compra.getDetalles().size() + " producto(s) ingresaron al inventario."
                    : "Compra actualizada correctamente.");
        } catch (CompraException e) {
            return volverAlFormulario(compra, model, e.getMessage(), session);
        } catch (RuntimeException e) {
            log.error("Error al guardar compra", e);
            redirectAttributes.addFlashAttribute(
                    "error", "No se pudo guardar la compra. Verifica los datos.");
        }

        return "redirect:/compras";
    }

    private Integer resolverUsuario(HttpSession session, Authentication authentication) {
        if (session.getAttribute("usuario_autenticado_id") instanceof Integer id) {
            return id;
        }
        if (authentication == null) {
            throw new CompraException("No se pudo identificar al usuario de la sesión.");
        }
        return compraService.idUsuarioDe(authentication.getName());
    }

    /** Vuelve a mostrar el modal con los datos ingresados (sin perder los productos agregados). */
    private String volverAlFormulario(Compra compra, Model model, String error, HttpSession session) {
        compraService.completarDatosVista(compra);
        cargarListas(model, null, "asc", session);
        model.addAttribute("modalAbrir", compra.getIdCompra() == null ? "nuevo" : "editar");
        model.addAttribute("compraSinProductos", compraService.esCompraSinProductos(compra.getIdCompra()));
        model.addAttribute("error", error);
        return "compras";
    }

    @GetMapping("/compras/editar/{id}")
    public String editar(
            @PathVariable Integer id,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            Compra compra = compraService.cargarParaEditar(id);

            model.addAttribute("compra", compra);
            model.addAttribute("modalAbrir", "editar");
            model.addAttribute("compraSinProductos", compra.getDetalles().isEmpty());
            cargarListas(model, null, "asc", session);

            return "compras";
        } catch (CompraException e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo cargar la compra: " + e.getMessage());
            return "redirect:/compras";
        } catch (RuntimeException e) {
            log.error("Error al cargar la compra {}", id, e);
            redirectAttributes.addFlashAttribute("error", "No se pudo cargar la compra. Intenta nuevamente.");
            return "redirect:/compras";
        }
    }

    @PostMapping("/compras/anular/{id}")
    public String anular(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {

        try {
            compraService.anularCompra(id);
            redirectAttributes.addFlashAttribute(
                    "mensaje", "Compra anulada correctamente. Su stock fue retirado del inventario.");
        } catch (CompraException | IllegalArgumentException | IllegalStateException e) {
            // Mensajes de negocio (compra inexistente, ya anulada, lote con ventas...)
            redirectAttributes.addFlashAttribute("error", "No se pudo anular la compra: " + e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al anular la compra {}", id, e);
            redirectAttributes.addFlashAttribute("error", "No se pudo anular la compra. Intenta nuevamente.");
        }

        return "redirect:/compras";
    }

}
