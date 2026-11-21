package com.example.boticadesarrolloweb.controller;

import com.example.boticadesarrolloweb.enums.MetodoPago;
import com.example.boticadesarrolloweb.model.InventarioItemDto;
import com.example.boticadesarrolloweb.model.PuntoVentaDto.ProductoVenta;
import com.example.boticadesarrolloweb.model.PuntoVentaDto.VentaRequest;
import com.example.boticadesarrolloweb.model.PuntoVentaDto.VentaResponse;
import com.example.boticadesarrolloweb.repository.UsuarioRepository;
import com.example.boticadesarrolloweb.service.PuntoVentaService;
import com.example.boticadesarrolloweb.service.PuntoVentaService.VentaException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.boticadesarrolloweb.service.ProductoService;

import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class VentaController {

    private final PuntoVentaService puntoVentaService;
    private final UsuarioRepository usuarioRepository;
    private final ProductoService productoService;

    @GetMapping("/venta")
    public String mostrarPuntoVenta(Model model) {
        List<InventarioItemDto> productosDisponibles = productoService.listarInventario(null, null, null)
                .stream()
                .filter(item -> item.getStockTotal() != null && item.getStockTotal() > 0)
                .toList();

        model.addAttribute("productos", productosDisponibles);
        model.addAttribute("metodosPago", MetodoPago.values());
        return "venta";
    }

    @GetMapping("/venta/api/productos")
    @ResponseBody
    public List<ProductoVenta> buscarProductos(@RequestParam(name = "q", required = false) String q) {
        return puntoVentaService.buscarProductos(q);
    }

    @PostMapping("/venta/api/registrar")
    @ResponseBody
    public ResponseEntity<?> registrar(@RequestBody VentaRequest request,
                                       HttpSession session,
                                       Authentication authentication) {
        Integer idUsuario = resolverUsuario(session, authentication);
        if (idUsuario == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo identificar al usuario de la sesión."));
        }
        try {
            VentaResponse respuesta = puntoVentaService.registrarVenta(request, idUsuario);
            return ResponseEntity.ok(respuesta);
        } catch (VentaException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error inesperado al registrar venta", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "No se pudo registrar la venta. Intente nuevamente."));
        }
    }

    /** ID del usuario logueado: primero de la sesión; si no está (p. ej. remember-me), por su nombre de usuario. */
    private Integer resolverUsuario(HttpSession session, Authentication authentication) {
        Object id = session.getAttribute("usuario_autenticado_id");
        if (id instanceof Integer idUsuario) {
            return idUsuario;
        }
        if (authentication == null) {
            return null;
        }
        String nombre = authentication.getName();
        return usuarioRepository.findByUsuario(nombre)
                .or(() -> usuarioRepository.findByEmail(nombre))
                .map(u -> u.getIdUsuario())
                .orElse(null);
    }
}
