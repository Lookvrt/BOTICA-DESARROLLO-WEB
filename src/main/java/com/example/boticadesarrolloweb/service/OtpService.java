package com.example.boticadesarrolloweb.service;

import com.example.boticadesarrolloweb.entity.UsuarioEntity;
import com.example.boticadesarrolloweb.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Servicio para gestión del OTP (One-Time Password) de doble autenticación y recuperación de clave.
 * Genera un código de 6 dígitos con expiración de 5 minutos y lo envía por email.
 *
 * Protecciones contra fuerza bruta (el contador vive en la BD, columna usuarios.otp_intentos):
 * - Cada código admite como máximo {@value #MAX_INTENTOS_OTP} comprobaciones, incluso con peticiones simultáneas.
 * - No se emite un OTP nuevo si el anterior se generó hace menos de 60 segundos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_DURACION_MINUTOS = 5;
    private static final int MAX_INTENTOS_OTP = 5;
    private static final Duration ESPERA_REENVIO = Duration.ofSeconds(60);

    private final JavaMailSender mailSender;
    private final UsuarioRepository usuarioRepository;

    @Value("${app.mail.from:seguridad@botica.com}")
    private String emailRemitente;

    /** Solo para desarrollo local: imprime el OTP en el log. Debe ser false en producción. */
    @Value("${app.security.otp.log-codigo:false}")
    private boolean logCodigo;

    /** Genera un código numérico criptográficamente seguro de 6 dígitos (000000 - 999999). */
    private String generarCodigoOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    /**
     * Guarda un OTP nuevo para el usuario (con su contador de intentos en cero).
     * Devuelve null, sin cambiar nada, si todavía rige el tiempo de espera desde el último código.
     */
    private String emitirOtp(UsuarioEntity usuario) {
        if (enEsperaDeReenvio(usuario)) {
            log.info("OTP no reenviado a '{}': aún rige el tiempo de espera.", usuario.getUsuario());
            return null;
        }
        String otp = generarCodigoOtp();
        LocalDateTime expiracion = LocalDateTime.now().plusMinutes(OTP_DURACION_MINUTOS);

        usuarioRepository.guardarOtp(usuario.getIdUsuario(), otp, expiracion);

        // Se refleja también en el objeto en memoria para quien lo siga usando en esta petición
        usuario.setOtpCode(otp);
        usuario.setOtpExpiracion(expiracion);
        usuario.setOtpIntentos(0);
        return otp;
    }

    /** El momento de emisión se deduce de la expiración (emisión = expiración − 5 min). */
    private boolean enEsperaDeReenvio(UsuarioEntity usuario) {
        if (usuario.getOtpExpiracion() == null) {
            return false;
        }
        LocalDateTime emitidoEn = usuario.getOtpExpiracion().minusMinutes(OTP_DURACION_MINUTOS);
        return LocalDateTime.now().isBefore(emitidoEn.plus(ESPERA_REENVIO));
    }

    /**
     * Genera un OTP de 6 dígitos, lo guarda en la BD y lo envía al email del usuario.
     *
     * @return true si se emitió y envió un código nuevo; false si se omitió por el tiempo de espera
     */
    public boolean generarYEnviarOtp(UsuarioEntity usuario) {
        String otp = emitirOtp(usuario);
        if (otp == null) {
            return false;
        }

        // Enviar email sin bloquear el flujo si falla el SMTP
        try {
            enviarEmail(usuario.getEmail(), usuario.getNombres(), otp);
            log.info("Email OTP enviado a: {}", usuario.getEmail());
        } catch (Exception e) {
            log.warn("No se pudo enviar el email OTP (verifica credenciales SMTP): {}", e.getMessage());
        }
        registrarCodigoEnLog(usuario, otp);
        return true;
    }

    /**
     * Valida el código OTP ingresado. Primero consume un intento de forma atómica en la BD y
     * después compara en tiempo constante: un mismo código no admite más de
     * {@value #MAX_INTENTOS_OTP} comprobaciones.
     */
    public boolean validarOtp(UsuarioEntity usuario, String codigoIngresado) {
        if (usuario.getOtpCode() == null || usuario.getOtpExpiracion() == null
                || !LocalDateTime.now().isBefore(usuario.getOtpExpiracion())) {
            return false;
        }

        if (usuarioRepository.consumirIntentoOtp(usuario.getIdUsuario(), MAX_INTENTOS_OTP) == 0) {
            log.warn("Intentos de OTP agotados para '{}': debe solicitar un código nuevo.", usuario.getUsuario());
            return false;
        }

        boolean coincide = codigoIngresado != null && MessageDigest.isEqual(
                usuario.getOtpCode().getBytes(StandardCharsets.UTF_8),
                codigoIngresado.getBytes(StandardCharsets.UTF_8));

        if (coincide) {
            usuarioRepository.limpiarOtp(usuario.getIdUsuario());
            usuario.setOtpCode(null);
            usuario.setOtpExpiracion(null);
            usuario.setOtpIntentos(0);
        }
        return coincide;
    }

    private void enviarEmail(String destinatario, String nombre, String otp) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(emailRemitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject("🔐 Código de verificación — Botica Ahorro y Salud");
        mensaje.setText(
                "Hola " + nombre + ",\n\n" +
                        "Tu código de verificación para acceder al sistema es:\n\n" +
                        "  ➡  " + otp + "\n\n" +
                        "Este código es válido por " + OTP_DURACION_MINUTOS + " minutos.\n" +
                        "Si no solicitaste este código, ignora este mensaje.\n\n" +
                        "— Sistema Botica Ahorro y Salud"
        );
        mailSender.send(mensaje);
    }

    /**
     * Genera un OTP para la recuperación de contraseña y lo envía al correo del usuario.
     *
     * @return true si se emitió y envió un código nuevo; false si se omitió por el tiempo de espera
     */
    public boolean generarYEnviarOtpRecuperacion(UsuarioEntity usuario) {
        String otp = emitirOtp(usuario);
        if (otp == null) {
            return false;
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(emailRemitente);
            mensaje.setTo(usuario.getEmail());
            mensaje.setSubject("🔑 Recuperación de contraseña — Botica Ahorro y Salud");
            mensaje.setText(
                    "Hola " + usuario.getNombres() + ",\n\n" +
                            "Recibimos una solicitud para restablecer la contraseña de tu cuenta.\n" +
                            "Tu código de verificación de 6 dígitos es:\n\n" +
                            "  ➡  " + otp + "\n\n" +
                            "Este código expirará en " + OTP_DURACION_MINUTOS + " minutos.\n" +
                            "Si no solicitaste este cambio, puedes ignorar este mensaje de forma segura.\n\n" +
                            "— Equipo de Seguridad Botica Ahorro y Salud"
            );
            mailSender.send(mensaje);
            log.info("Email de recuperación OTP enviado a: {}", usuario.getEmail());
        } catch (Exception e) {
            log.warn("No se pudo enviar el email de recuperación OTP (revisa credenciales SMTP): {}", e.getMessage());
        }
        registrarCodigoEnLog(usuario, otp);
        return true;
    }

    private void registrarCodigoEnLog(UsuarioEntity usuario, String otp) {
        if (logCodigo) {
            log.info("[DESARROLLO] OTP para '{}': {}", usuario.getUsuario(), otp);
        }
    }
}