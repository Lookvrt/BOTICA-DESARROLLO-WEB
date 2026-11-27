# 💊 Botica Ahorro y Salud - Sistema de Inventario y Ventas

Sistema web para el control de inventarios, trazabilidad de lotes y gestión de ventas de la botica **Ahorro y Salud**.

---

## 🌟 Características Destacadas

* **📊 Dashboard Principal:** Indicadores clave del negocio en tiempo real.
* **🚨 Alertas de Vencimiento y Stock:** Notificaciones automáticas de productos por vencer y stock bajo.
* **📦 Control por Lotes:** Seguimiento detallado de fechas de vencimiento y existencias.
* **🧾 Punto de Venta:** Registro rápido de ventas con múltiples métodos de pago (Efectivo, Tarjeta, Yape, Plin).

---

## 🛠️ Tech Stack

* **Backend:** Java 21, Spring Boot, Spring Data JPA, Lombok
* **Frontend:** HTML5, Bootstrap 5, Thymeleaf, JavaScript
* **Base de Datos:** MySQL

---

## 📁 Estructura del Proyecto

```text
src/main/
├── java/com/example/boticadesarrolloweb/
│   ├── controller/     # Controladores Spring MVC y Endpoints HTTP
│   ├── entity/         # Entidades JPA (@Entity) mapeadas a la base de datos
│   ├── enums/          # Enumeraciones del sistema (Rol, MetodoPago, Estados)
│   ├── model/          # DTOs y modelos de formulario con validaciones (@NotBlank, @NotNull)
│   ├── repository/     # Interfaces de acceso a datos (Spring Data JPA)
│   └── service/        # Lógica de negocio del sistema
│
└── resources/
    ├── static/         # Recursos estáticos (css, images, js)
    └── templates/      # Vistas HTML y componentes reutilizables (fragment/)
```

---

## 🚀 Despliegue en Render (Producción en la Nube)

1. En tu dashboard de [Render.com](https://render.com), presiona **New + → Web Service**.
2. Conecta el repositorio: `Lookvrt/BOTICA-DESARROLLO-WEB` (o tu fork).
3. Render detectará automáticamente el **Dockerfile**.
4. En **Environment Variables**, añade las siguientes variables:
   * `DB_PASSWORD` = `<tu_contraseña_aiven>`
   * `MAIL_PASSWORD` = `<tu_contraseña_mailtrap>`
5. Presiona **Deploy Web Service**. Render generará tu URL HTTPS pública automáticamente.

---

## 💻 Ejecución Local (Para el Equipo)

### Requisitos Previos
* **JDK 21** o superior.
* Conexión a Internet (la base de datos está centralizada en Aiven MySQL).

### Instrucciones de Ejecución

1. **Clonar el repositorio:**
   ```bash
   git clone git@github.com:Lookvrt/BOTICA-DESARROLLO-WEB.git
   cd BOTICA-DESARROLLO-WEB
   ```

2. **Configurar credenciales locales:**
   Copia la plantilla y coloca las contraseñas del equipo:
   ```bash
   cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
   ```

3. **Ejecutar el proyecto:**
   ```bash
   ./mvnw spring-boot:run
   ```
   *(En Windows: `.\mvnw.cmd spring-boot:run`)*

4. **Acceso Web:**
   * URL: `http://localhost:8080`
   * Usuario inicial: `admin`
   * Contraseña: `admin123`
   * Código OTP 2FA: revisa tu bandeja de Mailtrap o los logs de la terminal.