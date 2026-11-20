package com.example.boticadesarrolloweb.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Manejador personalizado de fallos de autenticación para protección contra
 * fuerza bruta (Hallazgo HZ-01 / Prueba PS-02).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String username = request.getParameter("usuario");
        String clientIp = extraerIp(request);

        log.warn("Fallo de autenticación para usuario='{}', IP='{}', motivo={}",
                username, clientIp, exception.getClass().getSimpleName());

        // Si Spring Security ya detectó que la cuenta está bloqueada o desactivada
        if (exception instanceof LockedException) {
            getRedirectStrategy().sendRedirect(request, response, "/login?error=locked");
            return;
        }

        if (exception instanceof DisabledException) {
            getRedirectStrategy().sendRedirect(request, response, "/login?error=disabled");
            return;
        }

        // Registrar el intento fallido y evaluar si corresponde bloqueo por 24 horas
        LoginAttemptService.ResultadoLogin resultado = loginAttemptService.registrarFallo(username, clientIp);

        String urlDestino = switch (resultado.getEstado()) {
            case BLOQUEADO_AHORA -> "/login?error=locked_now";
            case YA_BLOQUEADO    -> "/login?error=locked";
            case IP_BLOQUEADA    -> "/login?error=ip_locked";
            case INTENTO_REGISTRADO -> {
                if (resultado.getIntentosRestantes() != null) {
                    yield "/login?error=bad_credentials&restantes=" + resultado.getIntentosRestantes();
                }
                yield "/login?error=bad_credentials";
            }
        };

        getRedirectStrategy().sendRedirect(request, response, urlDestino);
    }

    private String extraerIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank() && !"unknown".equalsIgnoreCase(xf)) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

