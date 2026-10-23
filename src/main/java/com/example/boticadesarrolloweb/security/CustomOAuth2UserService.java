package com.example.boticadesarrolloweb.security;

import com.example.boticadesarrolloweb.entity.UsuarioEntity;
import com.example.boticadesarrolloweb.enums.Rol;
import com.example.boticadesarrolloweb.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

/**
 * Servicio personalizado para procesar usuarios autenticados mediante Google OAuth2.
 * Si el usuario no existe en la base de datos, lo registra automáticamente con rol VENDEDOR.
 * Mapea el rol de la base de datos como autoridad de Spring Security (ej: ROLE_ADMINISTRADOR).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String givenName = oAuth2User.getAttribute("given_name");
        String familyName = oAuth2User.getAttribute("family_name");

        if (email == null) {
            log.error("Google no proporcionó el email del usuario");
            throw new OAuth2AuthenticationException("Google no proporcionó el email del usuario");
        }

        // Buscar en la base de datos o registrar nuevo usuario
        UsuarioEntity usuario = usuarioRepository.findByEmail(email).orElseGet(() -> {
            log.info("Registrando nuevo usuario desde Google OAuth2: {}", email);

            String baseUsername = email.split("@")[0];
            String username = baseUsername;
            int suffix = 1;
            while (usuarioRepository.existsByUsuario(username)) {
                username = baseUsername + suffix++;
            }

            String nombres = (givenName != null && !givenName.isBlank()) ? givenName : (name != null ? name : "Usuario");
            String apellidos = (familyName != null && !familyName.isBlank()) ? familyName : "Google";

            UsuarioEntity nuevo = UsuarioEntity.builder()
                    .usuario(username)
                    .email(email)
                    .nombres(nombres)
                    .apellidos(apellidos)
                    .clave(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .rol(Rol.VENDEDOR)
                    .estado(1)
                    .build();

            return usuarioRepository.save(nuevo);
        });

        // Crear autoridad según el rol del usuario en la base de datos
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name());

        return new DefaultOAuth2User(
                Collections.singleton(authority),
                oAuth2User.getAttributes(),
                "email"
        );
    }
}
