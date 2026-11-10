package com.example.boticadesarrolloweb.security;

import com.example.boticadesarrolloweb.entity.UsuarioEntity;
import com.example.boticadesarrolloweb.repository.UsuarioRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Manejador ejecutado tras un inicio de sesión exitoso con Google OAuth2.
 * Configura la sesión HTTP con los atributos del usuario y redirige a la vista
 * correspondiente según su rol sin solicitar 2FA (Google ya certifica la identidad).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UsuarioRepository usuarioRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        UsuarioEntity usuario = usuarioRepository.findByEmail(email != null ? email.trim() : "")
                .orElse(null);

        if (usuario != null && usuario.getEstado() != null && usuario.getEstado() == 1) {
            // Guardar atributos de usuario en la sesión HTTP
            HttpSession session = request.getSession(true);
            session.setAttribute("usuario_autenticado_id", usuario.getIdUsuario());
            session.setAttribute("usuario_autenticado_nombre", usuario.getNombres() + " " + usuario.getApellidos());
            session.setAttribute("usuario_autenticado_rol", usuario.getRol().name());

            log.info("Inicio de sesión exitoso con Google para: {} (Rol: {})", email, usuario.getRol());

            // Redirección directa por rol sin pasar por la raíz '/'
            String targetUrl = switch (usuario.getRol()) {
                case ADMINISTRADOR -> "/dashboard";
                case ALMACENERO -> "/compras";
                case VENDEDOR -> "/venta";
            };

            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } else {
            getRedirectStrategy().sendRedirect(request, response, "/login?error=oauth2");
        }
    }
}