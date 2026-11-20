package com.example.boticadesarrolloweb.security;

import com.example.boticadesarrolloweb.entity.UsuarioEntity;
import com.example.boticadesarrolloweb.enums.Rol;
import com.example.boticadesarrolloweb.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private LoginAttemptService loginAttemptService;
    private UserDetailsServiceImpl userDetailsService;

    private UsuarioEntity testUser;

    @BeforeEach
    void setUp() {
        loginAttemptService = new LoginAttemptService(usuarioRepository);
        userDetailsService = new UserDetailsServiceImpl(usuarioRepository);

        testUser = UsuarioEntity.builder()
                .idUsuario(1)
                .usuario("admin")
                .email("admin@botica.com")
                .clave("encodedPass")
                .nombres("Admin")
                .apellidos("User")
                .rol(Rol.ADMINISTRADOR)
                .estado(1)
                .intentosFallidos(0)
                .bloqueadoHasta(null)
                .build();
    }

    @Test
    @DisplayName("Debe registrar intentos fallidos y mostrar intentos restantes")
    void registrarFallo_primerosIntentos() {
        when(usuarioRepository.findByUsuario("admin")).thenReturn(Optional.of(testUser));
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // Intento 1
        var res1 = loginAttemptService.registrarFallo("admin", "192.168.1.10");
        assertEquals(LoginAttemptService.EstadoIntento.INTENTO_REGISTRADO, res1.getEstado());
        assertEquals(4, res1.getIntentosRestantes());
        assertEquals(1, testUser.getIntentosFallidos());
        assertFalse(testUser.isCuentaBloqueada());

        // Intento 2
        var res2 = loginAttemptService.registrarFallo("admin", "192.168.1.10");
        assertEquals(3, res2.getIntentosRestantes());
        assertEquals(2, testUser.getIntentosFallidos());
    }

    @Test
    @DisplayName("Al 5to intento fallido consecutivo debe bloquear la cuenta por 24 horas (HZ-01 / PS-02)")
    void registrarFallo_bloqueoAlQuintoIntento() {
        testUser.setIntentosFallidos(4);
        when(usuarioRepository.findByUsuario("admin")).thenReturn(Optional.of(testUser));
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var res5 = loginAttemptService.registrarFallo("admin", "192.168.1.10");

        assertEquals(LoginAttemptService.EstadoIntento.BLOQUEADO_AHORA, res5.getEstado());
        assertEquals(5, testUser.getIntentosFallidos());
        assertNotNull(testUser.getBloqueadoHasta());
        assertTrue(testUser.isCuentaBloqueada());
        assertTrue(testUser.getBloqueadoHasta().isAfter(LocalDateTime.now().plusHours(23)));
    }

    @Test
    @DisplayName("Si la cuenta ya está bloqueada, debe indicar que sigue bloqueada")
    void registrarFallo_cuentaYaBloqueada() {
        testUser.setIntentosFallidos(5);
        testUser.setBloqueadoHasta(LocalDateTime.now().plusHours(24));
        when(usuarioRepository.findByUsuario("admin")).thenReturn(Optional.of(testUser));

        var res = loginAttemptService.registrarFallo("admin", "192.168.1.10");
        assertEquals(LoginAttemptService.EstadoIntento.YA_BLOQUEADO, res.getEstado());
    }

    @Test
    @DisplayName("Login exitoso reinicia el contador de intentos fallidos a cero")
    void registrarExito_reiniciaContador() {
        testUser.setIntentosFallidos(3);
        when(usuarioRepository.findByUsuario("admin")).thenReturn(Optional.of(testUser));

        loginAttemptService.registrarExito("admin", "192.168.1.10");

        assertEquals(0, testUser.getIntentosFallidos());
        assertNull(testUser.getBloqueadoHasta());
        verify(usuarioRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("UserDetailsServiceImpl reporta cuenta bloqueada cuando está en período de 24h")
    void userDetailsService_cuentaBloqueada() {
        testUser.setIntentosFallidos(5);
        testUser.setBloqueadoHasta(LocalDateTime.now().plusHours(24));
        when(usuarioRepository.findByUsuario("admin")).thenReturn(Optional.of(testUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("admin");

        assertFalse(userDetails.isAccountNonLocked(), "La cuenta debe reportarse como bloqueada en Spring Security");
    }

    @Test
    @DisplayName("UserDetailsServiceImpl desbloquea automáticamente la cuenta si ya expiraron las 24 horas")
    void userDetailsService_autoDesbloqueoDespuesDe24Horas() {
        testUser.setIntentosFallidos(5);
        // El bloqueo expiró hace 1 minuto
        testUser.setBloqueadoHasta(LocalDateTime.now().minusMinutes(1));
        when(usuarioRepository.findByUsuario("admin")).thenReturn(Optional.of(testUser));
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDetails userDetails = userDetailsService.loadUserByUsername("admin");

        assertTrue(userDetails.isAccountNonLocked(), "La cuenta debe desbloquearse automáticamente tras expirar las 24 horas");
        assertEquals(0, testUser.getIntentosFallidos());
        assertNull(testUser.getBloqueadoHasta());
        verify(usuarioRepository, times(1)).save(testUser);
    }
}

