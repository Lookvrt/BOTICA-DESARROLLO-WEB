package com.example.boticadesarrolloweb.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * SOLO PARA DESARROLLO LOCAL.
 * Autentica automáticamente cada petición como un ADMINISTRADOR ficticio,
 * omitiendo login y OTP. Se registra únicamente si
 * app.security.dev-bypass=true y solo actúa en peticiones desde localhost.
 */
public class DevAuthBypassFilter extends OncePerRequestFilter {

    // Se usa el usuario real "admin" (creado por DataInitializer) para que las operaciones
    // que guardan el usuario (p. ej. ventas) funcionen. GlobalSessionControllerAdvice
    // completa id, nombre y rol en la sesión a partir de este nombre.
    static final String DEV_USER = "admin";

    private final HttpSessionSecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if (esLocalhost(request.getRemoteAddr())
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            var auth = new UsernamePasswordAuthenticationToken(
                    DEV_USER, null, List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);

            // Guardarlo en la sesión como un login real; si no, cada petición cuenta como un login
            // nuevo y el límite de 1 sesión por usuario (maximumSessions(1)) expira la sesión anterior.
            contextRepository.saveContext(context, request, response);

            HttpSession session = request.getSession(true);
            session.setAttribute("otp_verificado", true);
        }

        chain.doFilter(request, response);
    }

    private boolean esLocalhost(String ip) {
        return "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip);
    }
}
