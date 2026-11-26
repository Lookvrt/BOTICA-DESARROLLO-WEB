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

import java.util.Optional;

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

    /** Id ficticio para identificadores que no existen: el flujo continúa igual y no revela qué cuentas hay. */
    private static final int USUARIO_INEXISTENTE = -1;
    private static final long ESPERA_REENVIO_MS = 60_000;

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
        if ("vacio".equalsIgnoreCase(error)) {
            model.addAttribute("errorMsg", "Ingresa tu usuario o correo electrónico.");
        } else if ("codigo_invalido".equalsIgnoreCase(error)) {
            model.addAttribute("errorMsg", "El código ingresado es incorrecto o ya ha expirado.");
        } else if ("no_coinciden".equalsIgnoreCase(error)) {
            model.addAttribute("errorMsg", "Las contraseñas no coinciden. Por favor, verifica e inténtalo de nuevo.");
        } else if ("longitud".equalsIgnoreCase(error)) {
            model.addAttribute("errorMsg", "La contraseña debe tener al menos 6 caracteres.");
        } else if ("sesion_expirada".equalsIgnoreCase(error)) {
            model.addAttribute("errorMsg", "La sesión del proceso ha expirado. Por favor, inicia el proceso nuevamente.");
        } else if ("espera".equalsIgnoreCase(error)) {
            model.addAttribute("errorMsg", "Espera un minuto antes de solicitar otro código.");
        }

        // Manejo de mensajes de éxito
        if (enviado != null) {
            model.addAttribute("infoMsg", "Si la cuenta existe, enviamos un código de verificación al correo registrado.");
        }
        if (reenviado != null) {
            model.addAttribute("infoMsg", "Si la cuenta existe, se reenvió un código al correo registrado.");
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
            return "redirect:/recuperar-clave?error=vacio";
        }

        String idLimpio = identificador.trim();
        Optional<UsuarioEntity> encontrado = usuarioService.buscarPorUsuario(idLimpio)
                .or(() -> usuarioService.buscarPorEmail(idLimpio));

        // Se responde exactamente igual exista o no la cuenta (o no tenga correo),
        // para que nadie pueda averiguar qué usuarios o correos están registrados.
        int idUsuario = USUARIO_INEXISTENTE;
        if (encontrado.isPresent()) {
            UsuarioEntity usuario = encontrado.get();
            if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
                log.warn("Recuperación solicitada por un usuario sin email registrado: {}", usuario.getUsuario());
            } else {
                otpService.generarYEnviarOtpRecuperacion(usuario);
                idUsuario = usuario.getIdUsuario();
            }
        }

        session.setAttribute("recuperar_usuario_id", idUsuario);
        session.setAttribute("recuperar_paso", 2);
        session.setAttribute("recuperar_ultimo_envio", System.currentTimeMillis());

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
            // Cuenta inexistente: misma respuesta que un código incorrecto
            return "redirect:/recuperar-clave?error=codigo_invalido";
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

        // Tiempo de espera por sesión: es igual para cuentas existentes e inexistentes
        Long ultimoEnvio = (Long) session.getAttribute("recuperar_ultimo_envio");
        long ahora = System.currentTimeMillis();
        if (ultimoEnvio != null && ahora - ultimoEnvio < ESPERA_REENVIO_MS) {
            return "redirect:/recuperar-clave?error=espera";
        }
        session.setAttribute("recuperar_ultimo_envio", ahora);

        // Si la cuenta no existe no se envía nada, pero la respuesta es la misma
        usuarioService.buscarPorId(usuarioId).ifPresent(otpService::generarYEnviarOtpRecuperacion);
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
        session.removeAttribute("recuperar_ultimo_envio");
        session.removeAttribute("recuperar_paso");
        session.removeAttribute("recuperar_autorizado");
    }
}
