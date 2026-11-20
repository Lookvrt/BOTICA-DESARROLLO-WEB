package com.example.boticadesarrolloweb.security;

import com.example.boticadesarrolloweb.entity.UsuarioEntity;
import com.example.boticadesarrolloweb.repository.UsuarioRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de control de intentos de inicio de sesión y protección contra
 * ataques de fuerza bruta (Hallazgo HZ-01 / Prueba PS-02).
 *
 * Reglas:
 * - Límite de 5 intentos fallidos consecutivos.
 * - Bloqueo de cuenta / acceso por 24 horas al alcanzar el 5to intento.
 * - Desbloqueo automático al expirar las 24 horas.
 * - Reinicio del contador de intentos al autenticarse exitosamente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    public static final int MAX_INTENTOS = 5;
    public static final int DURACION_BLOQUEO_HORAS = 24;

    private final UsuarioRepository usuarioRepository;

    // Control en memoria por dirección IP para ataques distribuidos / usuarios inexistentes
    private final Map<String, IpRecord> ipAttempts = new ConcurrentHashMap<>();

    public enum EstadoIntento {
        INTENTO_REGISTRADO,
        BLOQUEADO_AHORA,
        YA_BLOQUEADO,
        IP_BLOQUEADA
    }

    @Getter
    public static class ResultadoLogin {
        private final EstadoIntento estado;
        private final Integer intentosRestantes;

        public ResultadoLogin(EstadoIntento estado, Integer intentosRestantes) {
            this.estado = estado;
            this.intentosRestantes = intentosRestantes;
        }

        public EstadoIntento getEstado() {
            return estado;
        }

        public Integer getIntentosRestantes() {
            return intentosRestantes;
        }
    }

    private static class IpRecord {
        int attempts = 0;
        Instant blockedUntil = null;
    }

    /**
     * Registra un intento fallido tanto para la cuenta de usuario como para la IP.
     */
    @Transactional
    public ResultadoLogin registrarFallo(String usernameOrEmail, String ip) {
        String clientIp = normalizarIp(ip);

        // 1. Verificar si la IP ya está bloqueada
        if (isIpBloqueada(clientIp)) {
            return new ResultadoLogin(EstadoIntento.IP_BLOQUEADA, 0);
        }

        // 2. Buscar si corresponde a un usuario registrado
        if (usernameOrEmail != null && !usernameOrEmail.isBlank()) {
            Optional<UsuarioEntity> optUsuario = usuarioRepository.findByUsuario(usernameOrEmail.trim())
                    .or(() -> usuarioRepository.findByEmail(usernameOrEmail.trim()));

            if (optUsuario.isPresent()) {
                UsuarioEntity usuario = optUsuario.get();

                // Si la cuenta ya estaba bloqueada
                if (usuario.isCuentaBloqueada()) {
                    log.warn("Intento de acceso en cuenta ya bloqueada: {}", usuario.getUsuario());
                    return new ResultadoLogin(EstadoIntento.YA_BLOQUEADO, 0);
                }

                // Incrementar contador de intentos fallidos
                int intentos = (usuario.getIntentosFallidos() == null ? 0 : usuario.getIntentosFallidos()) + 1;
                usuario.setIntentosFallidos(intentos);

                if (intentos >= MAX_INTENTOS) {
                    usuario.setBloqueadoHasta(LocalDateTime.now().plusHours(DURACION_BLOQUEO_HORAS));
                    usuarioRepository.save(usuario);
                    log.warn("🚨 Cuenta '{}' BLOQUEADA por {} horas tras {} intentos fallidos consecutivos.",
                            usuario.getUsuario(), DURACION_BLOQUEO_HORAS, intentos);
                    return new ResultadoLogin(EstadoIntento.BLOQUEADO_AHORA, 0);
                } else {
                    usuarioRepository.save(usuario);
                    int restantes = MAX_INTENTOS - intentos;
                    log.info("Intento fallido {}/{} para usuario '{}'. Restantes: {}",
                            intentos, MAX_INTENTOS, usuario.getUsuario(), restantes);
                    return new ResultadoLogin(EstadoIntento.INTENTO_REGISTRADO, restantes);
                }
            }
        }

        // 3. Si el usuario no existe en la BD, controlar por IP (excepto localhost)
        if (!isLocalhost(clientIp)) {
            IpRecord record = ipAttempts.computeIfAbsent(clientIp, k -> new IpRecord());
            record.attempts++;

            if (record.attempts >= MAX_INTENTOS) {
                record.blockedUntil = Instant.now().plus(DURACION_BLOQUEO_HORAS, ChronoUnit.HOURS);
                log.warn("🚨 IP '{}' BLOQUEADA por {} horas tras {} intentos fallidos.",
                        clientIp, DURACION_BLOQUEO_HORAS, record.attempts);
                return new ResultadoLogin(EstadoIntento.IP_BLOQUEADA, 0);
            } else {
                int restantes = MAX_INTENTOS - record.attempts;
                return new ResultadoLogin(EstadoIntento.INTENTO_REGISTRADO, restantes);
            }
        }

        return new ResultadoLogin(EstadoIntento.INTENTO_REGISTRADO, null);
    }

    /**
     * Se invoca tras una autenticación exitosa para reiniciar los contadores.
     */
    @Transactional
    public void registrarExito(String usernameOrEmail, String ip) {
        String clientIp = normalizarIp(ip);
        ipAttempts.remove(clientIp);

        if (usernameOrEmail != null && !usernameOrEmail.isBlank()) {
            usuarioRepository.findByUsuario(usernameOrEmail.trim())
                    .or(() -> usuarioRepository.findByEmail(usernameOrEmail.trim()))
                    .ifPresent(usuario -> {
                        if ((usuario.getIntentosFallidos() != null && usuario.getIntentosFallidos() > 0)
                                || usuario.getBloqueadoHasta() != null) {
                            usuario.setIntentosFallidos(0);
                            usuario.setBloqueadoHasta(null);
                            usuarioRepository.save(usuario);
                            log.info("Contador de intentos fallidos reiniciado para '{}'.", usuario.getUsuario());
                        }
                    });
        }
    }

    /**
     * Desbloquea manualmente a un usuario (por parte del administrador).
     */
    @Transactional
    public void desbloquearUsuario(Integer idUsuario) {
        usuarioRepository.findById(idUsuario).ifPresent(usuario -> {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
            usuarioRepository.save(usuario);
            log.info("Usuario '{}' desbloqueado manualmente por el administrador.", usuario.getUsuario());
        });
    }

    public boolean isIpBloqueada(String ip) {
        if (ip == null || isLocalhost(ip)) return false;
        IpRecord record = ipAttempts.get(normalizarIp(ip));
        if (record == null || record.blockedUntil == null) return false;

        if (Instant.now().isAfter(record.blockedUntil)) {
            // El bloqueo ya expiró
            ipAttempts.remove(normalizarIp(ip));
            return false;
        }
        return true;
    }

    public void desbloquearIp(String ip) {
        if (ip != null) {
            ipAttempts.remove(normalizarIp(ip));
        }
    }

    private boolean isLocalhost(String ip) {
        return ip == null || "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip);
    }

    private String normalizarIp(String ip) {
        if (ip == null) return "unknown";
        return ip.trim();
    }
}

