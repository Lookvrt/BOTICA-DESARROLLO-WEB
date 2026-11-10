package com.example.boticadesarrolloweb.security;

import com.example.boticadesarrolloweb.entity.UsuarioEntity;
import com.example.boticadesarrolloweb.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            OAuth2User oAuth2User = super.loadUser(userRequest);
            Map<String, Object> attributes = oAuth2User.getAttributes();
            String email = (String) attributes.get("email");

            // 1. Google debe entregar el correo
            if (email == null || email.isBlank()) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("email_missing"),
                        "No se pudo obtener el correo de la cuenta de Google."
                );
            }

            // 2. El correo debe estar verificado por Google
            boolean emailVerificado = Boolean.TRUE.equals(attributes.get("email_verified"));
            if (!emailVerificado) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("email_not_verified"),
                        "El correo de Google no está verificado."
                );
            }

            // 3. El correo debe estar pre-registrado por un administrador
            UsuarioEntity usuario = usuarioRepository.findByEmailIgnoreCase(email.trim())
                    .orElseThrow(() -> new OAuth2AuthenticationException(
                            new OAuth2Error("unauthorized_google_email"),
                            "Correo no autorizado."
                    ));

            // 4. La cuenta debe estar activa
            if (usuario.getEstado() != null && usuario.getEstado() == 0) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("user_disabled"),
                        "La cuenta asociada a este correo se encuentra inactiva."
                );
            }

            String roleName = "ROLE_" + usuario.getRol().name();
            return new DefaultOAuth2User(
                    Collections.singletonList(new SimpleGrantedAuthority(roleName)),
                    attributes,
                    "email"
            );

        } catch (OAuth2AuthenticationException ex) {
            throw ex; // Excepción controlada: conserva su código de error
        } catch (Exception ex) {
            log.error("Error inesperado en login con Google", ex);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("oauth2_auth_failed"),
                    "Error interno durante la autenticación."
            );
        }
    }
}