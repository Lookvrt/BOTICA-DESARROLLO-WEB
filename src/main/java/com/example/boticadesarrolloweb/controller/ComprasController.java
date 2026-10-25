package com.example.boticadesarrolloweb.controller;

import com.example.boticadesarrolloweb.entity.CompraEntity;
import com.example.boticadesarrolloweb.model.Compra;
import com.example.boticadesarrolloweb.service.CompraService;
import com.example.boticadesarrolloweb.service.ProveedorService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    private void cargarListas(Model model) {
        model.addAttribute("compras", compraService.listarCompras());
        model.addAttribute("proveedores", proveedorService.listarProveedores());
        model.addAttribute("usuarios", compraService.listarUsuarios());
    }

    @GetMapping("/compras")
    public String compras(Model model) {
        if (!model.containsAttribute("compra")) {
            model.addAttribute("compra", new Compra());
        }

        cargarListas(model);
        return "compras";
    }

    @PostMapping("/compras/guardar")
    public String guardar(
            @Valid @ModelAttribute("compra") Compra compra, BindingResult resultado, Model model, RedirectAttributes redirectAttributes) {

        if (resultado.hasErrors()) {
            cargarListas(model);
            model.addAttribute("modalAbrir", compra.getIdCompra() == null ? "nuevo" : "editar");
            model.addAttribute("error", "Revisa los datos ingresados en el formulario.");

            return "compras";
        }

        try {
            compraService.guardarCompra(compra);
            redirectAttributes.addFlashAttribute(
                    "mensaje", "Compra guardada correctamente.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute(
                    "error", "No se pudo guardar la compra. Verifica los datos.");
        }

        return "redirect:/compras";
    }

    @GetMapping("/compras/editar/{id}")
    public String editar(
            @PathVariable Integer id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            CompraEntity entidad = compraService.buscarPorId(id);

            Compra compra = Compra.builder()
                    .idCompra(entidad.getIdCompra())
                    .idProveedor(entidad.getProveedor() != null ? entidad.getProveedor().getIdProveedor() : null)
                    .idUsuario(entidad.getUsuario() != null ? entidad.getUsuario().getIdUsuario() : null)
                    .fechaCompra(entidad.getFechaCompra())
                    .tipoComprobante(entidad.getTipoComprobante())
                    .numeroComprobante(entidad.getNumeroComprobante())
                    .total(entidad.getTotal())
                    .estado(entidad.getEstado())
                    .build();

            model.addAttribute("compra", compra);
            model.addAttribute("modalAbrir", "editar");
            cargarListas(model);

            return "compras";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error", "No se pudo cargar la compra: " + e.getMessage());
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
                    "mensaje", "Compra anulada correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error", "No se pudo anular la compra: " + e.getMessage());
        }

        return "redirect:/compras";
    }

}
