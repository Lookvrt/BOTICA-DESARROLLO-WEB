package com.example.boticadesarrolloweb.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Se ejecuta cuando falla el login con Google. Traduce el código de error
 * lanzado en CustomOAuth2UserService a un parámetro de URL distinto,
 * para que el login muestre el mensaje correcto.
 */
@Slf4j
@Component
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String codigo = "oauth2_auth_failed";
        if (exception instanceof OAuth2AuthenticationException oae && oae.getError() != null) {
            codigo = oae.getError().getErrorCode();
        }

        log.warn("Fallo de login con Google. Código: {}", codigo);

        String destino = switch (codigo) {
            case "unauthorized_google_email"           -> "/login?error=oauth2_unregistered";
            case "user_disabled"                       -> "/login?error=oauth2_disabled";
            case "email_missing", "email_not_verified" -> "/login?error=oauth2_email";
            default                                    -> "/login?error=oauth2_failed";
        };

        getRedirectStrategy().sendRedirect(request, response, destino);
    }
}