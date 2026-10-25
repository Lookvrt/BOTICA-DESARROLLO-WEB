package com.example.boticadesarrolloweb.security;

import com.example.boticadesarrolloweb.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

/**
 * Asegura que cuando un usuario inicia sesión vía 'Recordar sesión' (Remember-Me)
 * o acceso directo autenticado, sus variables de sesión principales (nombre, rol, id)
 * se encuentren siempre disponibles para el menú lateral y las vistas.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalSessionControllerAdvice {

    private final UsuarioRepository usuarioRepository;

    @ModelAttribute
    public void populateSessionIfAuthenticated(HttpSession session, Principal principal) {
        if (principal != null && session.getAttribute("usuario_autenticado_nombre") == null) {
            String username = principal.getName();
            usuarioRepository.findByUsuario(username)
                    .or(() -> usuarioRepository.findByEmail(username))
                    .ifPresent(u -> {
                        session.setAttribute("usuario_autenticado_id", u.getIdUsuario());
                        session.setAttribute("usuario_autenticado_rol", u.getRol().name());
                        session.setAttribute("usuario_autenticado_nombre", u.getNombres());
                    });
        }
    }
}
