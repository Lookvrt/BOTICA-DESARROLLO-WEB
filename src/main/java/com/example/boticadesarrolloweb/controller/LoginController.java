package com.example.boticadesarrolloweb.controller;

import com.example.boticadesarrolloweb.entity.UsuarioEntity;
import com.example.boticadesarrolloweb.service.OtpService;
import com.example.boticadesarrolloweb.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador de Login y verificación OTP (doble autenticación).
 *
 * Flujo:
 * 1. GET  /login          → muestra formulario de login
 * 2. POST /login          → Spring Security valida credenciales
 * 3. POST /login/otp-redirect → genera OTP y redirige a /verificar-otp
 * 4. GET  /verificar-otp  → muestra formulario de código OTP
 * 5. POST /verificar-otp  → valida el código y redirige al dashboard
 * 6. POST /reenviar-otp   → reenvía un nuevo código al email
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class LoginController {

    private final UsuarioService usuarioService;
    private final OtpService otpService;

    // ─────────────────────────────────────────────────────────────
    // [1] Mostrar página de Login
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        Model model) {
        if (error != null) {
            model.addAttribute("errorMsg", "Usuario o contraseña incorrectos.");
        }
        if (logout != null) {
            model.addAttribute("logoutMsg", "Sesión cerrada correctamente.");
        }
        return "login";
    }

    // ─────────────────────────────────────────────────────────────
    // [3] POST interno: Spring Security llama aquí tras login exitoso
    //     Genera OTP y guarda usuario en sesión para el paso 2FA
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/login/otp-redirect")
    public String otpRedirect(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        UsuarioEntity usuario = usuarioService.buscarPorUsuario(username)
                .or(() -> usuarioService.buscarPorEmail(username))
                .orElse(null);

        if (usuario == null || usuario.getEmail() == null) {
            log.warn("Usuario sin email configurado: {}", username);
            return "redirect:/login?error=true";
        }

        // Marcar sesión como "pendiente de OTP" (no completamente autenticado)
        session.setAttribute("otp_usuario_id", usuario.getIdUsuario());
        session.setAttribute("otp_verificado", false);

        // Generar y enviar OTP al email
        otpService.generarYEnviarOtp(usuario);

        // Invalidar la autenticación de Spring Security hasta que pase el OTP
        SecurityContextHolder.clearContext();

        return "redirect:/verificar-otp";
    }

    // ─────────────────────────────────────────────────────────────
    // [4] Mostrar formulario de verificación OTP
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/verificar-otp")
    public String verOtp(HttpSession session, Model model,
                         @RequestParam(required = false) String error,
                         @RequestParam(required = false) String reenvio) {

        Integer usuarioId = (Integer) session.getAttribute("otp_usuario_id");
        if (usuarioId == null) {
            return "redirect:/login";
        }

        UsuarioEntity usuario = usuarioService.buscarPorId(usuarioId).orElse(null);
        if (usuario == null) return "redirect:/login";

        // Enmascarar email: usuario@gmail.com → us****@gmail.com
        String emailMasked = enmascararEmail(usuario.getEmail());
        model.addAttribute("emailMasked", emailMasked);

        if (error != null)   model.addAttribute("errorMsg", "Código incorrecto o expirado. Inténtalo de nuevo.");
        if (reenvio != null) model.addAttribute("reenvioMsg", "Se envió un nuevo código a tu email.");

        return "verificar-otp";
    }

    // ─────────────────────────────────────────────────────────────
    // [5] Verificar el código OTP ingresado
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/verificar-otp")
    public String verificarOtp(@RequestParam("codigo") String codigo,
                               HttpSession session,
                               RedirectAttributes redirectAttrs) {

        Integer usuarioId = (Integer) session.getAttribute("otp_usuario_id");
        if (usuarioId == null) return "redirect:/login";

        UsuarioEntity usuario = usuarioService.buscarPorId(usuarioId).orElse(null);
        if (usuario == null) return "redirect:/login";

        if (otpService.validarOtp(usuario, codigo.trim())) {
            // OTP correcto: marcar sesión como completamente verificada
            session.setAttribute("otp_verificado", true);
            session.setAttribute("usuario_autenticado_id", usuarioId);
            session.setAttribute("usuario_autenticado_rol", usuario.getRol().name());
            session.setAttribute("usuario_autenticado_nombre", usuario.getNombres());

            // Redirigir según rol
            return switch (usuario.getRol()) {
                case ADMINISTRADOR -> "redirect:/dashboard";
                case ALMACENERO    -> "redirect:/alertas";
                case VENDEDOR      -> "redirect:/venta";
            };
        }

        return "redirect:/verificar-otp?error=true";
    }

    // ─────────────────────────────────────────────────────────────
    // [6] Reenviar OTP
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/reenviar-otp")
    public String reenviarOtp(HttpSession session) {
        Integer usuarioId = (Integer) session.getAttribute("otp_usuario_id");
        if (usuarioId == null) return "redirect:/login";

        UsuarioEntity usuario = usuarioService.buscarPorId(usuarioId).orElse(null);
        if (usuario == null) return "redirect:/login";

        otpService.generarYEnviarOtp(usuario);

        return "redirect:/verificar-otp?reenvio=true";
    }

    // ─────────────────────────────────────────────────────────────
    // Helper: enmascara el email para mostrarlo en la vista
    // Ej: junior@gmail.com → ju****@gmail.com
    // ─────────────────────────────────────────────────────────────
    private String enmascararEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return email;
        String visible = email.substring(0, 2);
        String dominio = email.substring(atIndex);
        return visible + "****" + dominio;
    }
}
