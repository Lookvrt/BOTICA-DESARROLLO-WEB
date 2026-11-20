# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Inventory, lot-traceability and sales web app for the pharmacy "Botica Ahorro y Salud". Server-rendered Spring Boot MVC app (Java 21, Spring Boot 4.x, Spring Data JPA, Spring Security, Thymeleaf + Bootstrap 5, Lombok, MySQL). Code, UI text, logs and commit messages are in **Spanish**; keep that convention.

## Commands

Use the Maven wrapper (`.\mvnw.cmd` on Windows, `./mvnw` elsewhere):

```bash
./mvnw spring-boot:run                       # run at http://localhost:8080
./mvnw clean package -DskipTests             # build the JAR (what the Dockerfile does)
./mvnw test                                  # all tests
./mvnw test -Dtest=LoginAttemptServiceTest   # one test class
./mvnw test -Dtest=LoginAttemptServiceTest#methodName   # one test method
```

There is no linter/formatter configured.

## Local configuration

- `application.properties` sets `spring.profiles.active=local`, so `src/main/resources/application-local.properties` (git-ignored) is loaded. Create it from `application-local.properties.example`; it holds the DB password, Mailtrap password and Google OAuth client id/secret. In production (Render, via `Dockerfile`) these come from env vars (`DB_PASSWORD`, `MAIL_PASSWORD`, `GOOGLE_CLIENT_ID`, ...).
- There is **no local database**: the default datasource is a shared Aiven MySQL instance used by the whole team, with `spring.jpa.hibernate.ddl-auto=update`. Entity changes alter the shared schema on startup — be deliberate with column renames/removals.
- `BoticaDesarrolloWebApplicationTests` (`@SpringBootTest`) needs that DB reachable with valid credentials; `LoginAttemptServiceTest` is a pure Mockito unit test and runs offline.
- `DataInitializer` (a `CommandLineRunner`) seeds the `admin` / `admin123` ADMINISTRADOR user plus sample categories, products and lots if absent.
- 2FA is toggled by `app.security.2fa.enabled`. OTP codes go to Mailtrap's sandbox and are also logged.

## Architecture

Layered package structure under `com.example.boticadesarrolloweb`: `controller` → `service` → `repository` → `entity`.

- **`entity/` vs `model/`**: `*Entity` classes are the JPA mappings (Lombok `@Builder`). `model/` holds form/DTO objects with Bean Validation annotations, bound in controllers and converted to entities there or in services. Status flags like `estado` / `requiereReceta` are `Integer` 0/1, not booleans.
- **Stock lives in lots**: a `ProductoEntity` has no stock column; stock is the sum of `LoteEntity.stockActual` (see `LoteService.calcularStockTotal`). Expiry and low-stock alerts (`AlertaService`, `/alertas`) are computed from lots vs `producto.stockMinimo`.
- **Views**: controllers return Thymeleaf template names. Every page composes `fragment/head.html` (`common-head(title)`) and `fragment/sidebar.html` (`sidebar(activeKey)`); the sidebar uses `sec:authorize` to show menu items per role. Page-specific JS lives in `static/js/<page>.js` and calls a few JSON endpoints (`@ResponseBody` methods such as `/inventario/api/producto/{id}`, `/api/reportes/ventas-diarias`). Report exports (Excel/PDF/CSV) are in `ReporteExportService` using Apache POI, OpenPDF and Commons CSV.

### Security (`security/` package)

- **Authorization is URL-based in `SecurityConfig`**, by role (`Rol` enum: `ADMINISTRADOR`, `VENDEDOR`, `ALMACENERO`). Adding a new module means adding its paths there; anything unlisted falls to `anyRequest().authenticated()`. Access denied redirects to `/login?denied=true`.
- **Form login flow**: form fields are `usuario` / `clave`. On success Spring forwards to `POST /login/otp-redirect` (`LoginController`), which either finishes login directly (2FA off) or generates an OTP via `OtpService` and redirects to `/verificar-otp`. After OTP validation the controller rebuilds the `SecurityContext` and redirects by role (ADMINISTRADOR → `/dashboard`, ALMACENERO → `/alertas`, VENDEDOR → `/venta`).
- **Session attributes** used by views: `usuario_autenticado_id`, `usuario_autenticado_nombre`, `usuario_autenticado_rol`, plus `otp_usuario_id` / `otp_verificado` during 2FA. `GlobalSessionControllerAdvice` repopulates them for remember-me logins.
- **Google OAuth2**: `CustomOAuth2UserService` only accepts Google accounts whose email matches an active `UsuarioEntity`; `OAuth2LoginSuccessHandler` skips OTP and redirects by role; failures go through `OAuth2LoginFailureHandler`.
- **Brute-force lockout** (`LoginAttemptService`, `CustomAuthenticationFailureHandler`): 5 failed attempts lock the account for 24h (persisted on `UsuarioEntity.intentosFallidos` / `bloqueadoHasta`); failures for unknown usernames are tracked per IP in memory (localhost exempt). Failure reasons are passed to the login page as `?error=` codes (`bad_credentials`, `locked_now`, `locked`, `ip_locked`, `disabled`, `oauth2_*`) that `login.html` maps to messages. Admins can unlock users at `POST /usuarios/desbloquear/{id}`.
- Password recovery (`RecuperarClaveController`) is a separate public OTP-by-email flow under `/recuperar-clave/**`.

## Conventions

- Commit messages follow Conventional Commits in Spanish, e.g. `fix(auth): ...`, `feat(security): ...`, often referencing QA/security ticket IDs like `PS-02` / `HZ-01`.
