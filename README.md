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

* **Backend:** Java 25, Spring Boot, Spring Data JPA, Lombok
* **Frontend:** HTML5, Bootstrap 5, Thymeleaf, JavaScript
* **Base de Datos:** MySQL (XAMPP)

---

## 📁 Estructura del Proyecto

```text
src/main/
├── java/com/example/boticadesarrolloweb/
│   ├── controller/     # Controladores Spring MVC
│   ├── model/          # Entidades JPA (@Entity)
│   ├── repository/     # Interfaces de acceso a datos
│   └── service/        # Lógica de negocio
│
└── resources/
    ├── static/         # Recursos (css, images, js)
    └── templates/      # Vistas HTML y componentes reutilizables (fragment/)
```

---

## 🚀 Despliegue Local

### Requisitos Previos

* **JDK 25** o superior.
* **XAMPP** (con servicios Apache y MySQL activos).

### Instrucciones de Ejecución

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/tu-usuario/BOTICA-DESARROLLO-WEB.git
   cd BOTICA-DESARROLLO-WEB
   ```

2. **Crear la Base de Datos:**
   Crear un schema vacío en MySQL/phpMyAdmin (Spring Data JPA generará las tablas automáticamente):
   ```sql
   CREATE DATABASE botica_db;
   ```

3. **Verificar Configuración:**
   Confirmar credenciales de base de datos en `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/botica_db?useSSL=false&serverTimezone=UTC
   spring.datasource.username=root
   spring.datasource.password=
   ```

4. **Compilar y Correr:**
   Ejecutar la compilación limpia y desplegar el servidor:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

5. **Acceso Web:**
   Navegar a `http://localhost:8080` desde el explorador.