package com.example.boticadesarrolloweb.controller;

import com.example.boticadesarrolloweb.model.Cliente;
import com.example.boticadesarrolloweb.service.ClienteService;
import com.example.boticadesarrolloweb.service.ClienteService.ClienteException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/clientes")
public class ClientesController {

    private final ClienteService clienteService;

    private void cargarListado(Model model, String query, String orden) {
        model.addAttribute("clientes", clienteService.listar(query, orden));
        model.addAttribute("query", query);
        model.addAttribute("orden", orden);
    }

    @GetMapping
    public String clientes(@RequestParam(name = "q", required = false) String query,
                           @RequestParam(name = "orden", required = false, defaultValue = "asc") String orden,
                           Model model) {
        if (!model.containsAttribute("cliente")) {
            model.addAttribute("cliente", new Cliente());
        }
        cargarListado(model, query, orden);
        return "clientes";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("cliente", clienteService.cargarParaEditar(id));
            model.addAttribute("modalAbrir", "editar");
            cargarListado(model, null, "asc");
            return "clientes";
        } catch (ClienteException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/clientes";
        }
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("cliente") Cliente cliente, BindingResult resultado,
                          Model model, RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            return volverAlFormulario(cliente, model, "Revisa los datos ingresados en el formulario.");
        }
        try {
            clienteService.guardar(cliente);
            redirectAttributes.addFlashAttribute("mensaje", cliente.getIdCliente() == null
                    ? "Cliente registrado correctamente." : "Cliente actualizado correctamente.");
            return "redirect:/clientes";
        } catch (ClienteException e) {
            return volverAlFormulario(cliente, model, e.getMessage());
        }
    }

    private String volverAlFormulario(Cliente cliente, Model model, String error) {
        model.addAttribute("modalAbrir", cliente.getIdCliente() == null ? "nuevo" : "editar");
        model.addAttribute("error", error);
        cargarListado(model, null, "asc");
        return "clientes";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            boolean eliminado = clienteService.eliminar(id);
            redirectAttributes.addFlashAttribute("mensaje", eliminado
                    ? "Cliente eliminado correctamente."
                    : "El cliente tiene ventas registradas, así que se desactivó en lugar de eliminarse.");
        } catch (ClienteException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/clientes";
    }
}
