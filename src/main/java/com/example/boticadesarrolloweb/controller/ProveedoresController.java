package com.example.boticadesarrolloweb.controller;

import com.example.boticadesarrolloweb.entity.ProveedorEntity;
import com.example.boticadesarrolloweb.model.Proveedor;
import com.example.boticadesarrolloweb.service.ProveedorService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;

@Controller
public class ProveedoresController {

    private final ProveedorService proveedorService;

    public ProveedoresController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @GetMapping("/proveedores")
    public String proveedores(Model model) {
        model.addAttribute("proveedor", new Proveedor());
        model.addAttribute("proveedores",
                proveedorService.listarProveedores());
        return "proveedores";
    }

    @PostMapping("/proveedores/guardar")
    public String guardar(
            @Valid @ModelAttribute("proveedor") Proveedor proveedor,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (resultado.hasErrors()) {
            model.addAttribute("proveedores",
                    proveedorService.listarProveedores());
            model.addAttribute("modalAbrir", proveedor.getIdProveedor() == null ? "nuevo" : "editar");
            return "proveedores";
        }

        try {
            proveedorService.guardarProveedor(proveedor);
            redirectAttributes.addFlashAttribute(
                    "mensaje", proveedor.getIdProveedor() == null ? "Proveedor registrado correctamente." : "Proveedor actualizado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error", "Error al guardar proveedor: " + e.getMessage());
        }

        return "redirect:/proveedores";
    }

    @GetMapping("/proveedores/editar/{id}")
    public String editarProveedor(
            @PathVariable Integer id,
            Model model) {

        ProveedorEntity entidad = proveedorService.buscarPorId(id);

        Proveedor proveedor = Proveedor.builder()
                .idProveedor(entidad.getIdProveedor())
                .razonSocial(entidad.getRazonSocial())
                .ruc(entidad.getRuc())
                .telefono(entidad.getTelefono())
                .correo(entidad.getCorreo())
                .direccion(entidad.getDireccion())
                .estado(entidad.getEstado())
                .build();

        model.addAttribute("proveedor", proveedor);
        model.addAttribute("modalAbrir", "editar");
        model.addAttribute("proveedores", proveedorService.listarProveedores());

        return "proveedores";
    }

    @PostMapping("/proveedores/eliminar/{id}")
    public String eliminar(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {

        try {
            proveedorService.eliminarProveedor(id);
            redirectAttributes.addFlashAttribute("mensaje", "Proveedor eliminado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se puede eliminar el proveedor porque tiene compras o registros asociados.");
        }

        return "redirect:/proveedores";
    }
}
