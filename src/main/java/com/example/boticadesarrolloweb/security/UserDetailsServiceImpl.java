package com.example.boticadesarrolloweb.security;

import com.example.boticadesarrolloweb.entity.UsuarioEntity;
import com.example.boticadesarrolloweb.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementación de UserDetailsService para integrar la BD con Spring Security.
 * Carga el usuario por nombre de usuario, valida estado, bloqueo temporal por fuerza bruta
 * y asigna el ROL como autoridad.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UsuarioEntity usuario = usuarioRepository.findByUsuario(username)
                .or(() -> usuarioRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + username));

        boolean accountNonLocked = true;
        if (usuario.getBloqueadoHasta() != null) {
            if (LocalDateTime.now().isAfter(usuario.getBloqueadoHasta())) {
                // Período de bloqueo de 24h expirado: desbloquear automáticamente
                usuario.setIntentosFallidos(0);
                usuario.setBloqueadoHasta(null);
                usuarioRepository.save(usuario);
                log.info("Bloqueo de 24h expirado para el usuario '{}'. Cuenta desbloqueada automáticamente.", usuario.getUsuario());
            } else {
                accountNonLocked = false;
            }
        }

        boolean enabled = usuario.getEstado() != null && usuario.getEstado() == 1;

        return User.builder()
                .username(usuario.getUsuario())
                .password(usuario.getClave())
                .disabled(!enabled)
                .accountLocked(!accountNonLocked)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())))
                .build();
    }
}
