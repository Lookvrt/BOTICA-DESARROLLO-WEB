package com.example.boticadesarrolloweb.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 1. Recursos estáticos y rutas públicas
                        .requestMatchers("/", "/login", "/login/otp-redirect", "/verificar-otp", "/reenviar-otp",
                                "/recuperar-clave", "/recuperar-clave/**",
                                "/oauth2/**", "/login/oauth2/**", "/error",
                                "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()

                        // 2. Módulos EXCLUSIVOS de ADMINISTRADOR
                        .requestMatchers("/dashboard", "/api/dashboard/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/usuarios", "/usuarios/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/reporte", "/reporte/**", "/api/reportes/**").hasRole("ADMINISTRADOR")

                        // 3. Módulos compartidos
                        .requestMatchers("/venta", "/venta/**").hasAnyRole("ADMINISTRADOR", "VENDEDOR")
                        .requestMatchers("/inventario", "/inventario/**").hasAnyRole("ADMINISTRADOR", "ALMACENERO")
                        .requestMatchers("/compras", "/compras/**").hasAnyRole("ADMINISTRADOR", "ALMACENERO")
                        .requestMatchers("/proveedores", "/proveedores/**").hasAnyRole("ADMINISTRADOR", "ALMACENERO")
                        .requestMatchers("/alertas", "/alertas/**").hasAnyRole("ADMINISTRADOR", "ALMACENERO")

                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/login?denied=true")
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("usuario")
                        .passwordParameter("clave")
                        .successForwardUrl("/login/otp-redirect")
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("botica-secreta-remember-me-key-2026")
                        .tokenValiditySeconds(86400 * 7)
                        .rememberMeParameter("remember-me")
                        .userDetailsService(userDetailsService)
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureUrl("/login?error=oauth2")
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .maximumSessions(1)
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}