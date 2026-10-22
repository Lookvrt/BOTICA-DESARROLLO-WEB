package com.example.boticadesarrolloweb.security;

import com.example.boticadesarrolloweb.entity.UsuarioEntity;
import com.example.boticadesarrolloweb.enums.Rol;
import com.example.boticadesarrolloweb.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Inicializador de datos de prueba para asegurar que siempre haya
 * un usuario Administrador disponible para iniciar sesión y probar el 2FA.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!usuarioRepository.existsByUsuario("admin")) {
            UsuarioEntity admin = UsuarioEntity.builder()
                    .usuario("admin")
                    .clave(passwordEncoder.encode("admin123"))
                    .nombres("Junior")
                    .apellidos("Administrador")
                    .email("junior.admin@botica.com")
                    .rol(Rol.ADMINISTRADOR)
                    .estado(1)
                    .build();

            usuarioRepository.save(admin);
            log.info("==================================================================");
            log.info("👤 Usuario de prueba creado: admin / clave: admin123 (ADMINISTRADOR)");
            log.info("==================================================================");
        }
    }
}
