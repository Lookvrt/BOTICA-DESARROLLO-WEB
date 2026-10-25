package com.example.boticadesarrolloweb.controller;

import com.example.boticadesarrolloweb.entity.UsuarioEntity;
import com.example.boticadesarrolloweb.service.OtpService;
import com.example.boticadesarrolloweb.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador para la recuperación de contraseña de usuarios.
 * Flujo en 3 pasos:
 *  1. Solicitud con email o nombre de usuario.
 *  2. Verificación de código OTP de 6 dígitos enviado al correo.
 *  3. Establecimiento de la nueva contraseña y cifrado con BCrypt.
 */
@Slf4j
@Controller
@RequestMapping("/recuperar-clave")
@RequiredArgsConstructor
public class RecuperarClaveController {

    private final UsuarioService usuarioService;
    private final OtpService otpService;

    // ─────────────────────────────────────────────────────────────
    // [1] Página principal de recuperación (maneja los 3 pasos)
    // ─────────────────────────────────────────────────────────────
    @GetMapping
    public String index(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String enviado,
                        @RequestParam(required = false) String reenviado,
                        HttpSession session,
                        Model model) {

        Integer paso = (Integer) session.getAttribute("recuperar_paso");
        if (paso == null) {
            paso = 1;
        }

        // Manejo de mensajes de error
        if ("notfound".equalsIgnoreCase(error)) {
            model.addAttribute("errorMsg", "No existe ninguna cuenta registrada con ese usuario o correo.");
        } else if ("noemail".equalsIgnoreCase(error)) {
            model.addAttribute("errorMsg", "Este usuario no tiene un correo electrónico configurado. Contacta al administrador.");
        } else if ("codigo_invalido".equalsIgnoreCase(error)) {
            model.addAttribute("errorMsg", "El código ingresado es incorrecto o ya ha expirado.");
        } else if ("no_coinciden".equalsIgnoreCase(error)) {
            model.addAttribute("errorMsg", "Las contraseñas no coinciden. Por favor, verifica e inténtalo de nuevo.");
        } else if ("longitud".equalsIgnoreCase(error)) {
            model.addAttribute("errorMsg", "La contraseña debe tener al menos 6 caracteres.");
        } else if ("sesion_expirada".equalsIgnoreCase(error)) {
            model.addAttribute("errorMsg", "La sesión del proceso ha expirado. Por favor, inicia el proceso nuevamente.");
        }

        // Manejo de mensajes de éxito
        if (enviado != null) {
            model.addAttribute("infoMsg", "Se ha enviado un código de verificación a tu correo registrado.");
        }
        if (reenviado != null) {
            model.addAttribute("infoMsg", "Se reenvió un nuevo código a tu correo.");
        }

        // Enmascarar el correo para mostrarlo en el paso 2 (ej: j***r@correo.com)
        String email = (String) session.getAttribute("recuperar_email");
        if (email != null) {
            model.addAttribute("emailEnmascarado", enmascararEmail(email));
        }

        model.addAttribute("paso", paso);
        return "recuperar-clave";
    }

    // ─────────────────────────────────────────────────────────────
    // [2] Paso 1: Enviar solicitud con usuario o email
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/solicitar")
    public String solicitar(@RequestParam("identificador") String identificador,
                            HttpSession session) {

        if (identificador == null || identificador.trim().isEmpty()) {
            return "redirect:/recuperar-clave?error=notfound";
        }

        String idLimpio = identificador.trim();
        UsuarioEntity usuario = usuarioService.buscarPorUsuario(idLimpio)
                .or(() -> usuarioService.buscarPorEmail(idLimpio))
                .orElse(null);

        if (usuario == null) {
            log.warn("Intento de recuperación para usuario/email inexistente: {}", idLimpio);
            return "redirect:/recuperar-clave?error=notfound";
        }

        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            log.warn("Usuario sin email registrado para recuperación: {}", usuario.getUsuario());
            return "redirect:/recuperar-clave?error=noemail";
        }

        // Generar OTP y enviar correo
        otpService.generarYEnviarOtpRecuperacion(usuario);

        // Guardar estado en sesión
        session.setAttribute("recuperar_usuario_id", usuario.getIdUsuario());
        session.setAttribute("recuperar_email", usuario.getEmail());
        session.setAttribute("recuperar_paso", 2);

        return "redirect:/recuperar-clave?enviado=true";
    }

    // ─────────────────────────────────────────────────────────────
    // [3] Paso 2: Validar código OTP
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/verificar")
    public String verificar(@RequestParam("codigo") String codigo,
                            HttpSession session) {

        Integer usuarioId = (Integer) session.getAttribute("recuperar_usuario_id");
        if (usuarioId == null) {
            return "redirect:/recuperar-clave?error=sesion_expirada";
        }

        UsuarioEntity usuario = usuarioService.buscarPorId(usuarioId).orElse(null);
        if (usuario == null) {
            limpiarSesion(session);
            return "redirect:/recuperar-clave?error=sesion_expirada";
        }

        boolean valido = otpService.validarOtp(usuario, codigo != null ? codigo.trim() : "");
        if (!valido) {
            return "redirect:/recuperar-clave?error=codigo_invalido";
        }

        session.setAttribute("recuperar_paso", 3);
        session.setAttribute("recuperar_autorizado", true);
        return "redirect:/recuperar-clave";
    }

    // ─────────────────────────────────────────────────────────────
    // [4] Reenviar código OTP
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/reenviar")
    public String reenviar(HttpSession session) {
        Integer usuarioId = (Integer) session.getAttribute("recuperar_usuario_id");
        if (usuarioId == null) {
            return "redirect:/recuperar-clave?error=sesion_expirada";
        }

        UsuarioEntity usuario = usuarioService.buscarPorId(usuarioId).orElse(null);
        if (usuario == null) {
            limpiarSesion(session);
            return "redirect:/recuperar-clave?error=sesion_expirada";
        }

        otpService.generarYEnviarOtpRecuperacion(usuario);
        return "redirect:/recuperar-clave?reenviado=true";
    }

    // ─────────────────────────────────────────────────────────────
    // [5] Paso 3: Guardar la nueva contraseña
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/cambiar")
    public String cambiar(@RequestParam("nuevaClave") String nuevaClave,
                          @RequestParam("confirmarClave") String confirmarClave,
                          HttpSession session) {

        Boolean autorizado = (Boolean) session.getAttribute("recuperar_autorizado");
        Integer usuarioId = (Integer) session.getAttribute("recuperar_usuario_id");

        if (autorizado == null || !autorizado || usuarioId == null) {
            limpiarSesion(session);
            return "redirect:/recuperar-clave?error=sesion_expirada";
        }

        if (nuevaClave == null || nuevaClave.length() < 6) {
            return "redirect:/recuperar-clave?error=longitud";
        }

        if (!nuevaClave.equals(confirmarClave)) {
            return "redirect:/recuperar-clave?error=no_coinciden";
        }

        // Actualizar contraseña con hash BCrypt
        usuarioService.cambiarClave(usuarioId, nuevaClave);
        log.info("Contraseña actualizada exitosamente para usuario con ID: {}", usuarioId);

        limpiarSesion(session);
        return "redirect:/login?recuperado=true";
    }

    // ─────────────────────────────────────────────────────────────
    // [6] Cancelar y volver al Login
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/cancelar")
    public String cancelar(HttpSession session) {
        limpiarSesion(session);
        return "redirect:/login";
    }

    private void limpiarSesion(HttpSession session) {
        session.removeAttribute("recuperar_usuario_id");
        session.removeAttribute("recuperar_email");
        session.removeAttribute("recuperar_paso");
        session.removeAttribute("recuperar_autorizado");
    }

    private String enmascararEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf("@");
        String name = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (name.length() <= 2) {
            return name.charAt(0) + "***" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
    }
}
