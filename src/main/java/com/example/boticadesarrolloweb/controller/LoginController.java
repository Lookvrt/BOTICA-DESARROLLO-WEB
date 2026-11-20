package com.example.boticadesarrolloweb.controller;

import com.example.boticadesarrolloweb.entity.UsuarioEntity;
import com.example.boticadesarrolloweb.enums.Rol;
import com.example.boticadesarrolloweb.security.LoginAttemptService;
import com.example.boticadesarrolloweb.service.OtpService;
import com.example.boticadesarrolloweb.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controlador de Login y verificación OTP (doble autenticación opcional).
 *
 * Flujo:
 * 1. GET /login → muestra formulario de login
 * 2. POST /login → Spring Security valida credenciales
 * 3. POST /login/otp-redirect → Si 2FA está deshabilitado entra directo al
 * dashboard; si no, genera OTP
 * 4. GET /verificar-otp → muestra formulario de código OTP (si 2FA está activo)
 * 5. POST /verificar-otp → valida el código y redirige al dashboard
 * 6. POST /reenviar-otp → reenvía un nuevo código al email
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class LoginController {

    private final UsuarioService usuarioService;
    private final OtpService otpService;
    private final LoginAttemptService loginAttemptService;

    @Value("${app.security.2fa.enabled:false}")
    private boolean twoFactorAuthEnabled;

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("🔐 Estado de 2FA configurado en LoginController: twoFactorAuthEnabled = {}", twoFactorAuthEnabled);
    }

    // ─────────────────────────────────────────────────────────────
    // [1] Petición a la raíz "/"
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/")
    public String raiz(Authentication authentication) {
        if (estaAutenticado(authentication)) {
            return redireccionarPorRol(authentication);
        }
        return "redirect:/login";
    }

    // ─────────────────────────────────────────────────────────────
    // [2] Petición a "/login"
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        @RequestParam(required = false) String denied,
                        @RequestParam(required = false) Integer restantes,
                        Authentication authentication,
                        Model model) {

        // Solo redirigir si el usuario está autenticado Y NO vienen parámetros de error, logout o denegación
        if (error == null && logout == null && denied == null && estaAutenticado(authentication)) {
            return redireccionarPorRol(authentication);
        }

        if (denied != null) {
            model.addAttribute("errorMsg", "Su cuenta no tiene permisos para acceder a esta sección del sistema.");
        }
        if (error != null && !error.isBlank() && !"false".equalsIgnoreCase(error)) {
            String mensaje = switch (error.toLowerCase()) {
                case "oauth2_unregistered" ->
                        "Su correo de Google no está registrado en el sistema. Solicite al administrador que cree su cuenta.";
                case "oauth2_disabled" ->
                        "Su cuenta se encuentra inactiva. Contacte al administrador.";
                case "oauth2_email" ->
                        "No se pudo verificar el correo de su cuenta de Google.";
                case "oauth2_failed", "oauth2" ->
                        "No se pudo iniciar sesión con Google. Inténtelo de nuevo.";
                case "locked_now" ->
                        "Has alcanzado el límite de 5 intentos fallidos consecutivos. Tu cuenta ha sido bloqueada temporalmente por 24 horas por seguridad.";
                case "locked" ->
                        "Tu cuenta se encuentra bloqueada por 24 horas debido a múltiples intentos fallidos de inicio de sesión. Inténtalo más tarde o contacta al administrador.";
                case "ip_locked" ->
                        "Acceso bloqueado temporalmente por 24 horas debido a múltiples intentos fallidos desde este dispositivo.";
                case "disabled" ->
                        "Su cuenta se encuentra inactiva. Contacte al administrador.";
                case "bad_credentials", "true" -> {
                    // Intentos 1 y 2 (restantes = 4 o 3): solo mostrar "Usuario o contraseña incorrectos."
                    // Intentos 3 y 4 (restantes = 2 o 1): advertir los intentos restantes antes del bloqueo
                    if (restantes != null && restantes > 0 && restantes <= 2) {
                        yield "Usuario o contraseña incorrectos. Te queda" + (restantes == 1 ? " 1 intento" : "n " + restantes + " intentos") + " antes de que tu cuenta sea bloqueada por 24 horas.";
                    }
                    yield "Usuario o contraseña incorrectos.";
                }
                default -> null;
            };
            if (mensaje != null) {
                model.addAttribute("errorMsg", mensaje);
            }
        }
        if (logout != null) {
            model.addAttribute("logoutMsg", "Sesión cerrada correctamente.");
        }
        return "login";
    }

    private boolean estaAutenticado(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private String redireccionarPorRol(Authentication authentication) {
        if (authentication == null) return "redirect:/login";

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_ADMINISTRADOR".equals(role)) {
                return "redirect:/dashboard";
            } else if ("ROLE_ALMACENERO".equals(role)) {
                return "redirect:/compras";
            } else if ("ROLE_VENDEDOR".equals(role)) {
                return "redirect:/venta";
            }
        }
        return "redirect:/login";
    }

    // ─────────────────────────────────────────────────────────────
    // [3] POST interno: Spring Security llama aquí tras login exitoso
    // Si 2FA está activo genera OTP; si está inactivo da paso directo.
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/login/otp-redirect")
    public String otpRedirect(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        UsuarioEntity usuario = usuarioService.buscarPorUsuario(username)
                .or(() -> usuarioService.buscarPorEmail(username))
                .orElse(null);

        if (usuario == null) {
            log.warn("Usuario no encontrado en login redirect: {}", username);
            return "redirect:/login?error=true";
        }

        // Reiniciar intentos fallidos tras login exitoso
        loginAttemptService.registrarExito(usuario.getUsuario(), null);

        // Si la doble autenticación (2FA) está desactivada: entrar directo sin OTP
        if (!twoFactorAuthEnabled) {
            session.setAttribute("otp_verificado", true);
            session.setAttribute("usuario_autenticado_id", usuario.getIdUsuario());
            session.setAttribute("usuario_autenticado_rol", usuario.getRol().name());
            session.setAttribute("usuario_autenticado_nombre", usuario.getNombres());

            log.info("Acceso directo sin 2FA para '{}' (Rol: {})", usuario.getUsuario(), usuario.getRol());

            return switch (usuario.getRol()) {
                case ADMINISTRADOR -> "redirect:/dashboard";
                case ALMACENERO -> "redirect:/alertas";
                case VENDEDOR -> "redirect:/venta";
            };
        }

        if (usuario.getEmail() == null) {
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
        session.removeAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);

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
        if (usuario == null) {
            return "redirect:/login";
        }

        // Enmascarar email: usuario@gmail.com → us****@gmail.com
        String emailMasked = enmascararEmail(usuario.getEmail());
        model.addAttribute("emailMasked", emailMasked);

        if (error != null) {
            model.addAttribute("errorMsg", "Código incorrecto o expirado. Inténtalo de nuevo.");
        }
        if (reenvio != null) {
            model.addAttribute("reenvioMsg", "Se envió un nuevo código a tu email.");
        }

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
        if (usuarioId == null) {
            return "redirect:/login";
        }

        UsuarioEntity usuario = usuarioService.buscarPorId(usuarioId).orElse(null);
        if (usuario == null) {
            return "redirect:/login";
        }

        if (otpService.validarOtp(usuario, codigo.trim())) {
            // Reiniciar intentos fallidos tras validar OTP exitosamente
            loginAttemptService.registrarExito(usuario.getUsuario(), null);

            // OTP correcto: marcar sesión como completamente verificada
            session.setAttribute("otp_verificado", true);
            session.setAttribute("usuario_autenticado_id", usuarioId);
            session.setAttribute("usuario_autenticado_rol", usuario.getRol().name());
            session.setAttribute("usuario_autenticado_nombre", usuario.getNombres());

            // Restaurar contexto de Spring Security para autorizaciones y roles
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())
            );
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(usuario.getUsuario(), null, authorities);

            SecurityContext sc = SecurityContextHolder.createEmptyContext();
            sc.setAuthentication(authToken);
            SecurityContextHolder.setContext(sc);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);

            // Redirigir según rol
            return switch (usuario.getRol()) {
                case ADMINISTRADOR -> "redirect:/dashboard";
                case ALMACENERO -> "redirect:/alertas";
                case VENDEDOR -> "redirect:/venta";
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
        if (usuarioId == null) {
            return "redirect:/login";
        }

        UsuarioEntity usuario = usuarioService.buscarPorId(usuarioId).orElse(null);
        if (usuario == null) {
            return "redirect:/login";
        }

        otpService.generarYEnviarOtp(usuario);

        return "redirect:/verificar-otp?reenvio=true";
    }

    // ─────────────────────────────────────────────────────────────
    // Helper: enmascara el email para mostrarlo en la vista
    // Ej: junior@gmail.com → ju****@gmail.com
    // ─────────────────────────────────────────────────────────────
    private String enmascararEmail(String email) {
        if (email == null) return "";
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) {
            return email;
        }
        String visible = email.substring(0, 2);
        String dominio = email.substring(atIndex);
        return visible + "****" + dominio;
    }
}
