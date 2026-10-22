package com.example.boticadesarrolloweb.controller;

import com.example.boticadesarrolloweb.entity.UsuarioEntity;
import com.example.boticadesarrolloweb.enums.Rol;
import com.example.boticadesarrolloweb.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controlador para la gestión (CRUD) de usuarios del sistema.
 * Permite listar, buscar, registrar, editar y desactivar usuarios con roles.
 */
@Slf4j
@Controller
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuariosController {

    private final UsuarioService usuarioService;

    // ─────────────────────────────────────────────────────────────
    // [1] Listar usuarios y búsqueda
    // ─────────────────────────────────────────────────────────────
    @GetMapping
    public String usuarios(@RequestParam(name = "q", required = false) String query,
                           Model model) {
        List<UsuarioEntity> lista = usuarioService.listar(query);

        model.addAttribute("usuarios", lista);
        model.addAttribute("totalUsuarios", lista.size());
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("roles", Rol.values());

        return "usuarios";
    }

    // ─────────────────────────────────────────────────────────────
    // [2] Guardar usuario (Crear o Actualizar)
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute UsuarioEntity usuario,
                          RedirectAttributes redirectAttributes) {
        try {
            boolean esNuevo = (usuario.getIdUsuario() == null || usuario.getIdUsuario() == 0);

            if (esNuevo) {
                // Validar unicidad de usuario
                if (usuarioService.existeUsuario(usuario.getUsuario().trim())) {
                    redirectAttributes.addFlashAttribute("mensajeError",
                            "El nombre de usuario '" + usuario.getUsuario() + "' ya está registrado.");
                    return "redirect:/usuarios";
                }

                // Validar unicidad de correo
                if (usuarioService.existeEmail(usuario.getEmail().trim())) {
                    redirectAttributes.addFlashAttribute("mensajeError",
                            "El correo electrónico '" + usuario.getEmail() + "' ya está registrado.");
                    return "redirect:/usuarios";
                }

                // Validar contraseña obligatoria al crear
                if (usuario.getClave() == null || usuario.getClave().isBlank()) {
                    redirectAttributes.addFlashAttribute("mensajeError",
                            "Debe ingresar una contraseña para el nuevo usuario.");
                    return "redirect:/usuarios";
                }

                usuarioService.crear(usuario);
                redirectAttributes.addFlashAttribute("mensajeExito",
                        "Usuario '" + usuario.getUsuario() + "' registrado exitosamente.");
            } else {
                // Validar si el nuevo username o email colisiona con otro usuario
                UsuarioEntity actual = usuarioService.buscarPorId(usuario.getIdUsuario()).orElse(null);
                if (actual != null) {
                    if (!actual.getUsuario().equalsIgnoreCase(usuario.getUsuario().trim())
                            && usuarioService.existeUsuario(usuario.getUsuario().trim())) {
                        redirectAttributes.addFlashAttribute("mensajeError",
                                "El nombre de usuario '" + usuario.getUsuario() + "' ya pertenece a otra persona.");
                        return "redirect:/usuarios";
                    }

                    if (!actual.getEmail().equalsIgnoreCase(usuario.getEmail().trim())
                            && usuarioService.existeEmail(usuario.getEmail().trim())) {
                        redirectAttributes.addFlashAttribute("mensajeError",
                                "El correo electrónico '" + usuario.getEmail() + "' ya pertenece a otra persona.");
                        return "redirect:/usuarios";
                    }
                }

                usuarioService.actualizar(usuario);
                redirectAttributes.addFlashAttribute("mensajeExito",
                        "Usuario '" + usuario.getUsuario() + "' actualizado exitosamente.");
            }
        } catch (Exception e) {
            log.error("Error al guardar usuario: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("mensajeError",
                    "Error al guardar usuario: " + e.getMessage());
        }

        return "redirect:/usuarios";
    }

    // ─────────────────────────────────────────────────────────────
    // [3] Eliminar usuario (Soft delete)
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable("id") Integer id,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        try {
            Integer usuarioEnSesionId = (Integer) session.getAttribute("usuario_autenticado_id");
            if (usuarioEnSesionId != null && usuarioEnSesionId.equals(id)) {
                redirectAttributes.addFlashAttribute("mensajeError",
                        "No puedes eliminar tu propia cuenta en sesión activa.");
                return "redirect:/usuarios";
            }

            usuarioService.eliminar(id);
            redirectAttributes.addFlashAttribute("mensajeExito", "Usuario eliminado correctamente.");
        } catch (Exception e) {
            log.error("Error al eliminar usuario: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("mensajeError",
                    "No se pudo eliminar el usuario: " + e.getMessage());
        }

        return "redirect:/usuarios";
    }
}
