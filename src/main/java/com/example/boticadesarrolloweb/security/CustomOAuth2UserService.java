package com.example.boticadesarrolloweb.security;

import com.example.boticadesarrolloweb.entity.UsuarioEntity;
import com.example.boticadesarrolloweb.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_missing"),
                    "No se pudo obtener el correo de la cuenta de Google."
            );
        }

        // 1. Buscar si el correo ya fue registrado por el Administrador
        UsuarioEntity usuario = usuarioRepository.findByEmail(email.trim())
                .orElseThrow(() -> new OAuth2AuthenticationException(
                        new OAuth2Error("unauthorized_google_email"),
                        "El correo " + email + " no está autorizado. Solicite acceso al administrador."
                ));

        // 2. Verificar que el usuario no esté inactivo (soft delete)
        if (usuario.getEstado() != null && usuario.getEstado() == 0) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("user_disabled"),
                    "La cuenta asociada a este correo se encuentra inactiva."
            );
        }

        String roleName = "ROLE_" + usuario.getRol().name();
        return new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority(roleName)),
                oAuth2User.getAttributes(),
                "email"
        );
    }
}