package com.example.boticadesarrolloweb.controller;

import com.example.boticadesarrolloweb.entity.CategoriaEntity;
import com.example.boticadesarrolloweb.entity.LoteEntity;
import com.example.boticadesarrolloweb.entity.ProductoEntity;
import com.example.boticadesarrolloweb.model.InventarioItemDto;
import com.example.boticadesarrolloweb.service.CategoriaService;
import com.example.boticadesarrolloweb.service.LoteService;
import com.example.boticadesarrolloweb.service.ProductoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final LoteService loteService;

    // ─────────────────────────────────────────────────────────────
    // [1] Listado principal de Inventario con filtros y métricas
    // ─────────────────────────────────────────────────────────────
    @GetMapping
    public String inventario(@RequestParam(name = "q", required = false) String query,
                             @RequestParam(name = "categoria", required = false) Integer idCategoria,
                             @RequestParam(name = "stock", required = false) String filtroStock,
                             Model model) {

        List<InventarioItemDto> items = productoService.listarInventario(query, idCategoria, filtroStock);
        List<CategoriaEntity> categorias = categoriaService.listarActivas();

        // Calcular métricas para las tarjetas de resumen
        List<InventarioItemDto> todos = productoService.listarInventario(null, null, null);
        long enStock = todos.stream().filter(i -> "NORMAL".equals(i.getEstadoStock())).count();
        long criticos = todos.stream().filter(i -> "CRITICO".equals(i.getEstadoStock())).count();
        long agotados = todos.stream().filter(i -> "AGOTADO".equals(i.getEstadoStock())).count();
        long porVencer = todos.stream().filter(InventarioItemDto::isProximoAVencer).count();

        model.addAttribute("items", items);
        model.addAttribute("categorias", categorias);
        model.addAttribute("totalProductos", todos.size());
        model.addAttribute("totalEnStock", enStock);
        model.addAttribute("totalCriticos", criticos);
        model.addAttribute("totalAgotados", agotados);
        model.addAttribute("totalPorVencer", porVencer);

        // Mantener parámetros de filtro en la vista
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("categoriaSeleccionada", idCategoria != null ? idCategoria : 0);
        model.addAttribute("stockSeleccionado", filtroStock != null ? filtroStock : "TODOS");

        return "inventario";
    }

    // ─────────────────────────────────────────────────────────────
    // [2] Guardar o Editar Producto
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/guardar")
    public String guardar(@RequestParam(required = false) Integer idProducto,
                          @RequestParam String codigo,
                          @RequestParam String nombre,
                          @RequestParam(required = false) String descripcion,
                          @RequestParam(required = false) String principioActivo,
                          @RequestParam(required = false) String concentracion,
                          @RequestParam(required = false) String presentacion,
                          @RequestParam Integer idCategoria,
                          @RequestParam(defaultValue = "0.00") BigDecimal precioCompra,
                          @RequestParam BigDecimal precioVenta,
                          @RequestParam(defaultValue = "5") Integer stockMinimo,
                          @RequestParam(defaultValue = "0") Integer requiereReceta,
                          @RequestParam(required = false) String numeroLote,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaVencimiento,
                          @RequestParam(required = false) Integer stockInicial,
                          RedirectAttributes redirectAttributes) {

        try {
            CategoriaEntity categoria = categoriaService.buscarPorId(idCategoria)
                    .orElseThrow(() -> new IllegalArgumentException("Categoría no válida"));

            boolean esNuevo = (idProducto == null || idProducto == 0);

            if (esNuevo) {
                // Validar código único
                if (productoService.existeCodigo(codigo.trim())) {
                    redirectAttributes.addFlashAttribute("mensajeError",
                            "El código '" + codigo.trim() + "' ya está registrado en otro producto.");
                    return "redirect:/inventario";
                }

                ProductoEntity nuevo = ProductoEntity.builder()
                        .codigo(codigo.trim().toUpperCase())
                        .nombre(nombre.trim())
                        .descripcion(descripcion)
                        .principioActivo(principioActivo)
                        .concentracion(concentracion)
                        .presentacion(presentacion)
                        .categoria(categoria)
                        .precioCompra(precioCompra)
                        .precioVenta(precioVenta)
                        .stockMinimo(stockMinimo)
                        .requiereReceta(requiereReceta)
                        .estado(1)
                        .build();

                productoService.crear(nuevo, numeroLote, fechaVencimiento, stockInicial);
                redirectAttributes.addFlashAttribute("mensajeExito",
                        "Producto '" + nuevo.getNombre() + "' registrado correctamente con código " + nuevo.getCodigo() + ".");

            } else {
                // Validar si el código colisiona con otro producto
                ProductoEntity actual = productoService.buscarPorId(idProducto)
                        .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

                if (!actual.getCodigo().equalsIgnoreCase(codigo.trim()) && productoService.existeCodigo(codigo.trim())) {
                    redirectAttributes.addFlashAttribute("mensajeError",
                            "El código '" + codigo.trim() + "' ya pertenece a otro producto.");
                    return "redirect:/inventario";
                }

                actual.setCodigo(codigo.trim().toUpperCase());
                actual.setNombre(nombre.trim());
                actual.setDescripcion(descripcion);
                actual.setPrincipioActivo(principioActivo);
                actual.setConcentracion(concentracion);
                actual.setPresentacion(presentacion);
                actual.setCategoria(categoria);
                actual.setPrecioCompra(precioCompra);
                actual.setPrecioVenta(precioVenta);
                actual.setStockMinimo(stockMinimo);
                actual.setRequiereReceta(requiereReceta);

                productoService.actualizar(actual);
                redirectAttributes.addFlashAttribute("mensajeExito",
                        "Producto '" + actual.getNombre() + "' actualizado correctamente.");
            }

        } catch (Exception e) {
            log.error("Error al guardar producto: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("mensajeError",
                    "Error al procesar la solicitud: " + e.getMessage());
        }

        return "redirect:/inventario";
    }

    // ─────────────────────────────────────────────────────────────
    // [3] Agregar nuevo lote a un producto existente
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/lote/guardar")
    public String guardarLote(@RequestParam Integer idProducto,
                              @RequestParam String numeroLote,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFabricacion,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaVencimiento,
                              @RequestParam Integer cantidad,
                              @RequestParam(defaultValue = "0.00") BigDecimal precioCompra,
                              RedirectAttributes redirectAttributes) {

        try {
            ProductoEntity producto = productoService.buscarPorId(idProducto)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

            if (fechaVencimiento.isBefore(LocalDate.now())) {
                redirectAttributes.addFlashAttribute("mensajeError",
                        "La fecha de vencimiento no puede ser anterior al día de hoy.");
                return "redirect:/inventario";
            }

            LoteEntity lote = LoteEntity.builder()
                    .producto(producto)
                    .numeroLote(numeroLote.trim().toUpperCase())
                    .fechaFabricacion(fechaFabricacion != null ? fechaFabricacion : LocalDate.now())
                    .fechaVencimiento(fechaVencimiento)
                    .cantidadInicial(cantidad)
                    .stockActual(cantidad)
                    .precioCompra(precioCompra.compareTo(BigDecimal.ZERO) > 0 ? precioCompra : producto.getPrecioCompra())
                    .build();

            loteService.guardar(lote);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Lote " + lote.getNumeroLote() + " registrado con " + cantidad + " unidades para " + producto.getNombre() + ".");

        } catch (Exception e) {
            log.error("Error al registrar lote: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("mensajeError", "Error al registrar el lote: " + e.getMessage());
        }

        return "redirect:/inventario";
    }

    // ─────────────────────────────────────────────────────────────
    // [4] Desactivar / Eliminar Producto (Soft Delete)
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable("id") Integer idProducto,
                           RedirectAttributes redirectAttributes) {
        try {
            ProductoEntity p = productoService.buscarPorId(idProducto).orElse(null);
            if (p != null) {
                productoService.eliminar(idProducto);
                redirectAttributes.addFlashAttribute("mensajeExito",
                        "Producto '" + p.getNombre() + "' retirado del inventario.");
            }
        } catch (Exception e) {
            log.error("Error al eliminar producto {}: {}", idProducto, e.getMessage());
            redirectAttributes.addFlashAttribute("mensajeError", "No se pudo eliminar el producto.");
        }
        return "redirect:/inventario";
    }

    // ─────────────────────────────────────────────────────────────
    // [5] Endpoint JSON: Obtener datos de producto para editar
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/api/producto/{id}")
    @ResponseBody
    public ResponseEntity<?> obtenerProductoJson(@PathVariable("id") Integer id) {
        return productoService.buscarPorId(id)
                .map(p -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("idProducto", p.getIdProducto());
                    data.put("codigo", p.getCodigo());
                    data.put("nombre", p.getNombre());
                    data.put("descripcion", p.getDescripcion());
                    data.put("principioActivo", p.getPrincipioActivo());
                    data.put("concentracion", p.getConcentracion());
                    data.put("presentacion", p.getPresentacion());
                    data.put("idCategoria", p.getCategoria() != null ? p.getCategoria().getIdCategoria() : null);
                    data.put("precioCompra", p.getPrecioCompra());
                    data.put("precioVenta", p.getPrecioVenta());
                    data.put("stockMinimo", p.getStockMinimo());
                    data.put("requiereReceta", p.getRequiereReceta());
                    return ResponseEntity.ok(data);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ─────────────────────────────────────────────────────────────
    // [6] Endpoint JSON: Obtener lotes de un producto
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/api/producto/{id}/lotes")
    @ResponseBody
    public ResponseEntity<?> obtenerLotesJson(@PathVariable("id") Integer id) {
        List<LoteEntity> lotes = loteService.listarPorProducto(id);
        List<Map<String, Object>> lotesDto = lotes.stream().map(l -> {
            Map<String, Object> item = new HashMap<>();
            item.put("idLote", l.getIdLote());
            item.put("numeroLote", l.getNumeroLote());
            item.put("fechaFabricacion", l.getFechaFabricacion());
            item.put("fechaVencimiento", l.getFechaVencimiento());
            item.put("cantidadInicial", l.getCantidadInicial());
            item.put("stockActual", l.getStockActual());
            item.put("precioCompra", l.getPrecioCompra());
            return item;
        }).toList();

        return ResponseEntity.ok(lotesDto);
    }
}
