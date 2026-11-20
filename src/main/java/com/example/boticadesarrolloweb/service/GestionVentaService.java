package com.example.boticadesarrolloweb.service;

import com.example.boticadesarrolloweb.entity.*;
import com.example.boticadesarrolloweb.enums.EstadoVenta;
import com.example.boticadesarrolloweb.model.DetalleVenta;
import com.example.boticadesarrolloweb.model.Venta;
import com.example.boticadesarrolloweb.repository.*;
import com.example.boticadesarrolloweb.service.PuntoVentaService.VentaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Módulo Ventas: listado de todas las ventas (Punto de Venta y facturas) y
 * registro / edición / anulación de facturas a clientes con RUC.
 * El stock se descuenta en orden FEFO reutilizando {@link PuntoVentaService#descontarStockFefo}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GestionVentaService {

    public static final String TIPO_FACTURA = "FACTURA";
    public static final String SERIE_FACTURA = "F001-";

    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final LoteRepository loteRepository;
    private final PuntoVentaService puntoVentaService;

    @Transactional(readOnly = true)
    public List<VentaEntity> listar(String q) {
        return ventaRepository.buscarParaListado(q != null ? q.trim() : null);
    }

    /** Carga una venta con una línea por producto para el modal de edición. */
    @Transactional(readOnly = true)
    public Venta cargarParaEditar(Integer idVenta) {
        VentaEntity venta = buscar(idVenta);
        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new VentaException("No se puede editar una venta anulada.");
        }
        Map<Integer, DetalleVenta> porProducto = new LinkedHashMap<>();
        for (DetalleVentaEntity d : detalleVentaRepository.findByVentaConLote(idVenta)) {
            ProductoEntity p = d.getProducto();
            DetalleVenta linea = porProducto.computeIfAbsent(p.getIdProducto(), id -> DetalleVenta.builder()
                    .idProducto(id)
                    .codigoProducto(p.getCodigo())
                    .nombreProducto(p.getNombre())
                    .precioUnitario(d.getPrecioUnitario())
                    .cantidad(0)
                    .build());
            linea.setCantidad(linea.getCantidad() + d.getCantidad());
        }
        LocalDate hoy = LocalDate.now();
        porProducto.values().forEach(l -> {
            l.setSubtotal(l.getPrecioUnitario().multiply(BigDecimal.valueOf(l.getCantidad())));
            // Al editar, lo que ya tiene esta venta también se puede volver a vender
            l.setStockDisponible(loteRepository.sumStockVendible(l.getIdProducto(), hoy) + l.getCantidad());
        });

        return Venta.builder()
                .idVenta(venta.getIdVenta())
                .idCliente(venta.getCliente() != null ? venta.getCliente().getIdCliente() : null)
                .metodoPago(venta.getMetodoPago())
                .fechaVenta(venta.getFechaVenta())
                .tipoComprobante(venta.getTipoComprobante())
                .numeroComprobante(venta.getNumeroComprobante())
                .vendedor(venta.getUsuario().getNombres() + " " + venta.getUsuario().getApellidos())
                .total(venta.getTotal())
                .estado(venta.getEstado())
                .detalles(new ArrayList<>(porProducto.values()))
                .build();
    }

    /** Vuelve a completar los datos de apoyo de la vista tras un error de validación. */
    @Transactional(readOnly = true)
    public void completarDatosVista(Venta form) {
        Map<Integer, Integer> enEstaVenta = new HashMap<>();
        Map<Integer, BigDecimal> preciosOriginales = new HashMap<>();
        if (form.getIdVenta() != null) {
            ventaRepository.findById(form.getIdVenta()).ifPresent(v -> {
                form.setTipoComprobante(v.getTipoComprobante());
                form.setNumeroComprobante(v.getNumeroComprobante());
                form.setFechaVenta(v.getFechaVenta());
                form.setVendedor(v.getUsuario().getNombres() + " " + v.getUsuario().getApellidos());
            });
            for (DetalleVentaEntity d : detalleVentaRepository.findByVentaConLote(form.getIdVenta())) {
                enEstaVenta.merge(d.getProducto().getIdProducto(), d.getCantidad(), Integer::sum);
                preciosOriginales.putIfAbsent(d.getProducto().getIdProducto(), d.getPrecioUnitario());
            }
        }
        if (form.getDetalles() == null) return;
        form.getDetalles().removeIf(d -> d == null || d.getIdProducto() == null);
        LocalDate hoy = LocalDate.now();
        for (DetalleVenta d : form.getDetalles()) {
            productoRepository.findById(d.getIdProducto()).ifPresent(p -> {
                d.setCodigoProducto(p.getCodigo());
                d.setNombreProducto(p.getNombre());
                d.setPrecioUnitario(preciosOriginales.getOrDefault(p.getIdProducto(), p.getPrecioVenta()));
                d.setStockDisponible(loteRepository.sumStockVendible(p.getIdProducto(), hoy)
                        + enEstaVenta.getOrDefault(p.getIdProducto(), 0));
            });
        }
    }

    /** true si la venta existe y no tiene detalle de productos (ventas antiguas registradas solo con su total). */
    @Transactional(readOnly = true)
    public boolean esVentaSinProductos(Integer idVenta) {
        return idVenta != null && detalleVentaRepository.findByVentaConLote(idVenta).isEmpty();
    }

    /** Registra una factura nueva o guarda los cambios de una venta existente. */
    @Transactional
    public VentaEntity guardar(Venta form, Integer idUsuario) {
        // Venta antigua sin productos: si no se agregan productos, solo se actualiza la cabecera y conserva su total
        if (form.getIdVenta() != null && sinLineas(form) && esVentaSinProductos(form.getIdVenta())) {
            VentaEntity venta = buscar(form.getIdVenta());
            if (venta.getEstado() == EstadoVenta.ANULADA) {
                throw new VentaException("No se puede editar una venta anulada.");
            }
            asignarClienteYPago(venta, form);
            return venta;
        }

        Map<Integer, Integer> cantidades = agruparLineas(form);

        if (form.getIdVenta() == null) {
            UsuarioEntity usuario = usuarioRepository.findById(idUsuario)
                    .orElseThrow(() -> new VentaException("No se pudo identificar al usuario que realiza la venta."));
            VentaEntity venta = ventaRepository.save(VentaEntity.builder()
                    .usuario(usuario)
                    .cliente(clienteObligatorio(form.getIdCliente()))
                    .tipoComprobante(TIPO_FACTURA)
                    .numeroComprobante(siguienteNumeroFactura())
                    .metodoPago(form.getMetodoPago())
                    .estado(EstadoVenta.REGISTRADA)
                    .build());
            venta.setTotal(venderLineas(venta, cantidades, Map.of()));
            log.info("Factura {} registrada: total S/ {}", venta.getNumeroComprobante(), venta.getTotal());
            return venta;
        }

        VentaEntity venta = buscar(form.getIdVenta());
        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new VentaException("No se puede editar una venta anulada.");
        }
        asignarClienteYPago(venta, form);

        // Devolver al inventario lo vendido y volver a descontar con las nuevas cantidades.
        // Los productos que ya estaban en la venta conservan su precio original.
        Map<Integer, BigDecimal> preciosOriginales = new HashMap<>();
        for (DetalleVentaEntity d : detalleVentaRepository.findByVentaConLote(venta.getIdVenta())) {
            preciosOriginales.putIfAbsent(d.getProducto().getIdProducto(), d.getPrecioUnitario());
            d.getLote().setStockActual(d.getLote().getStockActual() + d.getCantidad());
            detalleVentaRepository.delete(d);
        }
        detalleVentaRepository.flush();

        venta.setTotal(venderLineas(venta, cantidades, preciosOriginales));
        log.info("Venta {} actualizada: total S/ {}", venta.getNumeroComprobante(), venta.getTotal());
        return venta;
    }

    /** Anula la venta y devuelve sus unidades a los lotes de donde salieron. */
    @Transactional
    public void anular(Integer idVenta) {
        VentaEntity venta = buscar(idVenta);
        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new VentaException("La venta ya está anulada.");
        }
        for (DetalleVentaEntity d : detalleVentaRepository.findByVentaConLote(idVenta)) {
            d.getLote().setStockActual(d.getLote().getStockActual() + d.getCantidad());
        }
        venta.setEstado(EstadoVenta.ANULADA);
        log.info("Venta {} anulada; stock devuelto a sus lotes.", venta.getNumeroComprobante());
    }

    // ─────────────────────────────────────────────────────────────

    private VentaEntity buscar(Integer idVenta) {
        return ventaRepository.findById(idVenta).orElseThrow(() -> new VentaException("La venta no existe."));
    }

    private ClienteEntity clienteObligatorio(Integer idCliente) {
        if (idCliente == null) {
            throw new VentaException("Selecciona el cliente: las facturas requieren un cliente con RUC.");
        }
        ClienteEntity cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new VentaException("El cliente seleccionado no existe."));
        if (cliente.getEstado() == null || cliente.getEstado() != 1) {
            throw new VentaException("El cliente " + cliente.getRazonSocial() + " está inactivo.");
        }
        return cliente;
    }

    /** Facturas: cliente obligatorio. Boletas del Punto de Venta: cliente opcional. */
    private void asignarClienteYPago(VentaEntity venta, Venta form) {
        venta.setCliente(TIPO_FACTURA.equals(venta.getTipoComprobante())
                ? clienteObligatorio(form.getIdCliente())
                : (form.getIdCliente() != null ? clienteObligatorio(form.getIdCliente()) : null));
        venta.setMetodoPago(form.getMetodoPago());
    }

    private boolean sinLineas(Venta form) {
        return form.getDetalles() == null || form.getDetalles().stream().noneMatch(d -> d != null && d.getIdProducto() != null);
    }

    private Map<Integer, Integer> agruparLineas(Venta form) {
        Map<Integer, Integer> cantidades = new LinkedHashMap<>();
        if (form.getDetalles() != null) {
            for (DetalleVenta d : form.getDetalles()) {
                if (d == null || d.getIdProducto() == null) continue;
                if (d.getCantidad() == null || d.getCantidad() <= 0) {
                    throw new VentaException("Hay una cantidad inválida en la venta.");
                }
                cantidades.merge(d.getIdProducto(), d.getCantidad(), Integer::sum);
            }
        }
        if (cantidades.isEmpty()) {
            throw new VentaException("Agrega al menos un producto a la venta.");
        }
        return cantidades;
    }

    private BigDecimal venderLineas(VentaEntity venta, Map<Integer, Integer> cantidades,
                                    Map<Integer, BigDecimal> preciosOriginales) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Integer, Integer> e : cantidades.entrySet()) {
            ProductoEntity producto = productoRepository.findById(e.getKey())
                    .orElseThrow(() -> new VentaException("Uno de los productos ya no está disponible."));
            BigDecimal precio = preciosOriginales.getOrDefault(producto.getIdProducto(), producto.getPrecioVenta());
            total = total.add(puntoVentaService.descontarStockFefo(venta, producto, e.getValue(), precio));
        }
        return total;
    }

    /** Correlativo de la serie de facturas: F001-00000001, F001-00000002, ... */
    private String siguienteNumeroFactura() {
        String ultimo = ventaRepository.ultimoNumeroDeSerie(SERIE_FACTURA);
        int siguiente = 1;
        if (ultimo != null) {
            try {
                siguiente = Integer.parseInt(ultimo.substring(SERIE_FACTURA.length())) + 1;
            } catch (NumberFormatException ignored) {
                // número con otro formato: se reinicia la cuenta
            }
        }
        return String.format("%s%08d", SERIE_FACTURA, siguiente);
    }
}
