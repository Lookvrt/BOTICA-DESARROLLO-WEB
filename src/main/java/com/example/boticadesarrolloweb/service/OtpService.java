package com.example.boticadesarrolloweb.service;

import com.example.boticadesarrolloweb.entity.UsuarioEntity;
import com.example.boticadesarrolloweb.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * Servicio para gestión del OTP (One-Time Password) de doble autenticación.
 * Genera un código de 6 dígitos, lo guarda en BD con expiración de 5 minutos
 * y lo envía al email del usuario vía Gmail SMTP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final JavaMailSender mailSender;
    private final UsuarioRepository usuarioRepository;

    @Value("${app.mail.from:seguridad@botica.com}")
    private String emailRemitente;

    private static final int OTP_DURACION_MINUTOS = 5;

    /**
     * Genera un OTP de 6 dígitos, lo guarda en la BD y lo envía al email del usuario.
     *
     * @param usuario entidad del usuario autenticado
     */
    public void generarYEnviarOtp(UsuarioEntity usuario) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        LocalDateTime expiracion = LocalDateTime.now().plusMinutes(OTP_DURACION_MINUTOS);

        // Guardar OTP en BD
        usuario.setOtpCode(otp);
        usuario.setOtpExpiracion(expiracion);
        usuarioRepository.save(usuario);

        // Enviar email de forma segura (sin bloquear el flujo si falla el SMTP)
        try {
            enviarEmail(usuario.getEmail(), usuario.getNombres(), otp);
            log.info("📧 Email OTP enviado con éxito a: {}", usuario.getEmail());
        } catch (Exception e) {
            log.warn("⚠️ No se pudo enviar el email OTP (verifica credenciales SMTP en application.properties): {}", e.getMessage());
        }

        // Mostrar código en consola para desarrollo y pruebas del docente/equipo
        log.info("=================================================");
        log.info("🔐 [DESARROLLO] CÓDIGO OTP PARA '{}': {}", usuario.getUsuario(), otp);
        log.info("=================================================");
    }

    /**
     * Valida el código OTP ingresado por el usuario.
     *
     * @param usuario entidad del usuario
     * @param codigoIngresado código que el usuario escribió en el formulario
     * @return true si el código es correcto y no ha expirado
     */
    public boolean validarOtp(UsuarioEntity usuario, String codigoIngresado) {
        if (usuario.getOtpCode() == null || usuario.getOtpExpiracion() == null) {
            return false;
        }

        boolean codigoCorrecto = usuario.getOtpCode().equals(codigoIngresado);
        boolean noExpirado = LocalDateTime.now().isBefore(usuario.getOtpExpiracion());

        if (codigoCorrecto && noExpirado) {
            // Limpiar OTP tras uso exitoso
            usuario.setOtpCode(null);
            usuario.setOtpExpiracion(null);
            usuarioRepository.save(usuario);
            return true;
        }

        return false;
    }

    /**
     * Envía el email con el código OTP al usuario.
     */
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
}
