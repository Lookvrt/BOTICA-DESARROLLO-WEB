package com.example.boticadesarrolloweb.service;

import com.example.boticadesarrolloweb.entity.*;
import com.example.boticadesarrolloweb.enums.EstadoCompra;
import com.example.boticadesarrolloweb.model.Compra;
import com.example.boticadesarrolloweb.model.DetalleCompra;
import com.example.boticadesarrolloweb.model.InventarioItemDto;
import com.example.boticadesarrolloweb.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CompraService {

    private static final DateTimeFormatter FORMATO_LOTE = DateTimeFormatter.ofPattern("yyMMdd");

    private final CompraRepository compraRepository;
    private final DetalleCompraRepository detalleCompraRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final ProveedorRepository proveedorRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final LoteRepository loteRepository;
    private final AlertaService alertaService;

    public CompraService(
            CompraRepository compraRepository,
            DetalleCompraRepository detalleCompraRepository,
            DetalleVentaRepository detalleVentaRepository,
            ProveedorRepository proveedorRepository,
            UsuarioRepository usuarioRepository,
            ProductoRepository productoRepository,
            LoteRepository loteRepository,
            AlertaService alertaService) {
        this.compraRepository = compraRepository;
        this.detalleCompraRepository = detalleCompraRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.proveedorRepository = proveedorRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.loteRepository = loteRepository;
        this.alertaService = alertaService;
    }

    /** Error de validación de negocio cuyo mensaje se muestra al usuario. */
    public static class CompraException extends RuntimeException {
        public CompraException(String mensaje) {
            super(mensaje);
        }
    }

    /** Resultado del buscador de productos del modal de compra. */
    public record ProductoCompra(Integer idProducto, String codigo, String nombre, String presentacion,
                                 BigDecimal precioCompra, Integer stockActual, Integer stockMinimo) {
    }

    @Transactional(readOnly = true)
    public List<CompraEntity> listarCompras() {
        return listarCompras(null, "asc");
    }

    @Transactional(readOnly = true)
    public List<CompraEntity> listarCompras(String query, String orden) {
        String queryLimpia = (query != null) ? query.trim() : null;
        return compraRepository.buscarYOrdenarPorProveedor(queryLimpia, orden, EstadoCompra.REGISTRADA);
    }

    @Transactional(readOnly = true)
    public CompraEntity buscarPorId(Integer id) {
        return compraRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Compra no encontrada"));
    }

    public List<ProveedorEntity> listarProveedores() {
        return proveedorRepository.findAll();
    }

    public List<UsuarioEntity> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ProductoCompra> buscarProductos(String q) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        return productoRepository.buscarParaVenta(q.trim(), PageRequest.of(0, 20)).stream()
                .map(p -> new ProductoCompra(p.getIdProducto(), p.getCodigo(), p.getNombre(), p.getPresentacion(),
                        p.getPrecioCompra(), loteRepository.sumStockByProductoId(p.getIdProducto()),
                        p.getStockMinimo()))
                .toList();
    }

    /**
     * Líneas de compra precargadas para los productos en alerta indicados.
     * Cantidad sugerida = (2 × stock mínimo) − stock actual, que siempre supera el stock mínimo;
     * la cantidad mínima permitida es stock mínimo + 1.
     */
    @Transactional(readOnly = true)
    public List<DetalleCompra> detallesDesdeAlertas(Collection<Integer> idsProducto) {
        Set<Integer> ids = new HashSet<>(idsProducto);
        List<DetalleCompra> detalles = new ArrayList<>();
        for (InventarioItemDto item : alertaService.listarProductosBajoStockMinimo()) {
            ProductoEntity p = item.getProducto();
            if (!ids.contains(p.getIdProducto())) continue;
            int minimo = p.getStockMinimo() != null ? p.getStockMinimo() : 0;
            detalles.add(DetalleCompra.builder()
                    .idProducto(p.getIdProducto())
                    .codigoProducto(p.getCodigo())
                    .nombreProducto(p.getNombre())
                    .stockActual(item.getStockTotal())
                    .stockMinimo(minimo)
                    .cantidadMinima(minimo + 1)
                    .cantidad(2 * minimo - item.getStockTotal())
                    .precioUnitario(p.getPrecioCompra())
                    .build());
        }
        return detalles;
    }

    /** Vuelve a completar los datos de apoyo de la vista (nombre, stock...) tras un error de validación. */
    @Transactional(readOnly = true)
    public void completarDatosVista(Compra compra) {
        if (compra.getDetalles() == null) return;
        compra.getDetalles().removeIf(d -> d == null || d.getIdProducto() == null);
        for (DetalleCompra d : compra.getDetalles()) {
            productoRepository.findById(d.getIdProducto()).ifPresent(p -> {
                d.setCodigoProducto(p.getCodigo());
                d.setNombreProducto(p.getNombre());
                d.setStockMinimo(p.getStockMinimo());
                d.setStockActual(loteRepository.sumStockByProductoId(p.getIdProducto()));
            });
            if (d.getIdDetalleCompra() != null) {
                detalleCompraRepository.findById(d.getIdDetalleCompra())
                        .ifPresent(det -> d.setCantidadVendida(vendidas(det.getLote())));
            }
        }
    }

    @Transactional
    public CompraEntity guardarCompra(Compra compra) {
        if (compra.getIdCompra() != null) {
            return actualizarCompra(compra);
        }

        List<DetalleCompra> detalles = compra.getDetalles() == null ? List.of()
                : compra.getDetalles().stream().filter(d -> d != null && d.getIdProducto() != null).toList();
        if (detalles.isEmpty()) {
            throw new CompraException("Agrega al menos un producto a la compra.");
        }

        CompraEntity entidad = new CompraEntity();
        entidad.setEstado(EstadoCompra.REGISTRADA);
        asignarCabecera(entidad, compra);
        entidad.setTotal(BigDecimal.ZERO);
        entidad = compraRepository.save(entidad);

        Set<Integer> productosVistos = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleCompra d : detalles) {
            total = total.add(crearLinea(entidad, d, productosVistos));
        }

        entidad.setTotal(total);
        return entidad;
    }

    /** Crea el lote y el detalle de una línea nueva de compra. Devuelve su subtotal. */
    private BigDecimal crearLinea(CompraEntity compra, DetalleCompra d, Set<Integer> productosVistos) {
        ProductoEntity producto = productoRepository.findById(d.getIdProducto())
                .orElseThrow(() -> new CompraException("Uno de los productos ya no existe o fue eliminado."));
        LocalDate hoy = LocalDate.now();

        if (!productosVistos.add(producto.getIdProducto())) {
            throw new CompraException("El producto \"" + producto.getNombre() + "\" está repetido en la compra.");
        }
        if (d.getCantidadMinima() != null && d.getCantidad() < d.getCantidadMinima()) {
            throw new CompraException("La cantidad de \"" + producto.getNombre()
                    + "\" debe ser mayor a su stock mínimo (" + (d.getCantidadMinima() - 1) + ").");
        }
        if (!d.getFechaVencimiento().isAfter(hoy)) {
            throw new CompraException("La fecha de vencimiento de \"" + producto.getNombre() + "\" debe ser posterior a hoy.");
        }

        LoteEntity lote = loteRepository.save(LoteEntity.builder()
                .producto(producto)
                .numeroLote(resolverNumeroLote(producto, d.getNumeroLote(), hoy))
                .fechaVencimiento(d.getFechaVencimiento())
                .cantidadInicial(d.getCantidad())
                .stockActual(d.getCantidad())
                .precioCompra(d.getPrecioUnitario())
                .build());

        BigDecimal subtotal = d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad()));
        detalleCompraRepository.save(DetalleCompraEntity.builder()
                .compra(compra)
                .producto(producto)
                .lote(lote)
                .cantidad(d.getCantidad())
                .precioUnitario(d.getPrecioUnitario())
                .subtotal(subtotal)
                .build());
        return subtotal;
    }

    /** true si la compra es antigua (registrada antes de que las compras tuvieran productos). */
    @Transactional(readOnly = true)
    public boolean esCompraSinProductos(Integer idCompra) {
        return idCompra != null && !detalleCompraRepository.existsByCompraIdCompra(idCompra);
    }

    /** Carga una compra con sus productos para el modal de edición. */
    @Transactional(readOnly = true)
    public Compra cargarParaEditar(Integer id) {
        CompraEntity entidad = buscarPorId(id);
        if (entidad.getEstado() == EstadoCompra.ANULADA) {
            throw new CompraException("No se puede editar una compra anulada.");
        }
        List<DetalleCompra> detalles = new ArrayList<>();
        for (DetalleCompraEntity d : detalleCompraRepository.findByCompraConLote(id)) {
            ProductoEntity p = d.getProducto();
            LoteEntity lote = d.getLote();
            detalles.add(DetalleCompra.builder()
                    .idDetalleCompra(d.getIdDetalleCompra())
                    .idProducto(p.getIdProducto())
                    .idLote(lote.getIdLote())
                    .codigoProducto(p.getCodigo())
                    .nombreProducto(p.getNombre())
                    .stockActual(loteRepository.sumStockByProductoId(p.getIdProducto()))
                    .stockMinimo(p.getStockMinimo())
                    .cantidad(d.getCantidad())
                    .precioUnitario(d.getPrecioUnitario())
                    .numeroLote(lote.getNumeroLote())
                    .fechaVencimiento(lote.getFechaVencimiento())
                    .cantidadVendida(vendidas(lote))
                    .build());
        }
        return Compra.builder()
                .idCompra(entidad.getIdCompra())
                .idProveedor(entidad.getProveedor() != null ? entidad.getProveedor().getIdProveedor() : null)
                .idUsuario(entidad.getUsuario() != null ? entidad.getUsuario().getIdUsuario() : null)
                .fechaCompra(entidad.getFechaCompra())
                .tipoComprobante(entidad.getTipoComprobante())
                .numeroComprobante(entidad.getNumeroComprobante())
                .total(entidad.getTotal())
                .estado(entidad.getEstado())
                .detalles(detalles)
                .build();
    }

    private int vendidas(LoteEntity lote) {
        return Math.max(0, lote.getCantidadInicial() - lote.getStockActual());
    }

    /**
     * Edición de una compra: cabecera + productos. Ajusta el inventario:
     * - línea existente: el lote pasa a la nueva cantidad (sin bajar de lo ya vendido);
     * - línea quitada: su lote se retira del inventario (solo si no tiene ventas);
     * - línea nueva: crea un lote nuevo, igual que al registrar.
     * Las compras antiguas sin productos pueden seguir guardándose solo con su total manual.
     */
    private CompraEntity actualizarCompra(Compra compra) {
        CompraEntity entidad = buscarPorId(compra.getIdCompra());
        if (entidad.getEstado() == EstadoCompra.ANULADA) {
            throw new CompraException("No se puede editar una compra anulada.");
        }
        asignarCabecera(entidad, compra);

        Map<Integer, DetalleCompraEntity> existentes = new HashMap<>();
        detalleCompraRepository.findByCompraConLote(entidad.getIdCompra())
                .forEach(d -> existentes.put(d.getIdDetalleCompra(), d));

        List<DetalleCompra> enviados = compra.getDetalles() == null ? List.of()
                : compra.getDetalles().stream().filter(d -> d != null && d.getIdProducto() != null).toList();

        if (enviados.isEmpty()) {
            if (!existentes.isEmpty()) {
                throw new CompraException("La compra debe tener al menos un producto. Si quieres dejarla sin efecto, anúlala.");
            }
            if (compra.getTotal() == null || compra.getTotal().signum() <= 0) {
                throw new CompraException("Ingresa el monto total o agrega productos a la compra.");
            }
            entidad.setTotal(compra.getTotal()); // compra antigua sin productos
            return compraRepository.save(entidad);
        }

        // 1. Líneas quitadas: retirar su stock (solo si no se vendió nada del lote)
        Set<Integer> idsEnviados = new HashSet<>();
        enviados.forEach(d -> {
            if (d.getIdDetalleCompra() != null) idsEnviados.add(d.getIdDetalleCompra());
        });
        for (DetalleCompraEntity d : existentes.values()) {
            if (idsEnviados.contains(d.getIdDetalleCompra())) continue;
            LoteEntity lote = d.getLote();
            if (vendidas(lote) > 0) {
                throw new CompraException("No se puede quitar \"" + d.getProducto().getNombre()
                        + "\": ya se vendieron " + vendidas(lote) + " unidades del lote " + lote.getNumeroLote() + ".");
            }
            detalleCompraRepository.delete(d);
            if (detalleVentaRepository.existsByLoteIdLote(lote.getIdLote())) {
                lote.setStockActual(0);
                lote.setCantidadInicial(0);
            } else {
                loteRepository.delete(lote);
            }
        }
        detalleCompraRepository.flush();

        // 2. Líneas existentes modificadas y líneas nuevas
        Set<Integer> productosVistos = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        LocalDate hoy = LocalDate.now();

        for (DetalleCompra d : enviados) {
            if (d.getIdDetalleCompra() == null) {
                total = total.add(crearLinea(entidad, d, productosVistos));
                continue;
            }
            DetalleCompraEntity det = existentes.get(d.getIdDetalleCompra());
            if (det == null) {
                throw new CompraException("Uno de los productos no pertenece a esta compra. Recarga la página.");
            }
            ProductoEntity producto = det.getProducto(); // el producto de una línea existente no cambia
            if (!productosVistos.add(producto.getIdProducto())) {
                throw new CompraException("El producto \"" + producto.getNombre() + "\" está repetido en la compra.");
            }

            LoteEntity lote = det.getLote();
            int vendidas = vendidas(lote);
            if (d.getCantidad() < vendidas) {
                throw new CompraException("La cantidad de \"" + producto.getNombre() + "\" no puede ser menor a "
                        + vendidas + " (unidades ya vendidas de este lote).");
            }
            if (!d.getFechaVencimiento().equals(lote.getFechaVencimiento()) && !d.getFechaVencimiento().isAfter(hoy)) {
                throw new CompraException("La fecha de vencimiento de \"" + producto.getNombre() + "\" debe ser posterior a hoy.");
            }
            String nuevoLote = d.getNumeroLote() == null ? "" : d.getNumeroLote().trim();
            if (!nuevoLote.isEmpty() && !nuevoLote.equalsIgnoreCase(lote.getNumeroLote())) {
                if (loteRepository.existsByProductoIdProductoAndNumeroLoteIgnoreCase(producto.getIdProducto(), nuevoLote)) {
                    throw new CompraException("El lote \"" + nuevoLote + "\" ya existe para \"" + producto.getNombre() + "\".");
                }
                lote.setNumeroLote(nuevoLote);
            }

            lote.setCantidadInicial(d.getCantidad());
            lote.setStockActual(d.getCantidad() - vendidas);
            lote.setPrecioCompra(d.getPrecioUnitario());
            lote.setFechaVencimiento(d.getFechaVencimiento());

            BigDecimal subtotal = d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad()));
            det.setCantidad(d.getCantidad());
            det.setPrecioUnitario(d.getPrecioUnitario());
            det.setSubtotal(subtotal);
            total = total.add(subtotal);
        }

        entidad.setTotal(total);
        return compraRepository.save(entidad);
    }

    private void asignarCabecera(CompraEntity entidad, Compra compra) {
        ProveedorEntity proveedor = proveedorRepository.findById(compra.getIdProveedor())
                .orElseThrow(() -> new CompraException("Proveedor no encontrado."));
        UsuarioEntity usuario = usuarioRepository.findById(compra.getIdUsuario())
                .orElseThrow(() -> new CompraException("Usuario no encontrado."));
        entidad.setProveedor(proveedor);
        entidad.setUsuario(usuario);
        entidad.setTipoComprobante(compra.getTipoComprobante());
        entidad.setNumeroComprobante(compra.getNumeroComprobante());
    }

    /** Usa el número de lote indicado (debe ser único por producto) o genera uno: LT-yyMMdd-CODIGO[-n]. */
    private String resolverNumeroLote(ProductoEntity producto, String indicado, LocalDate hoy) {
        Integer id = producto.getIdProducto();
        if (indicado != null && !indicado.isBlank()) {
            String lote = indicado.trim();
            if (loteRepository.existsByProductoIdProductoAndNumeroLoteIgnoreCase(id, lote)) {
                throw new CompraException("El lote \"" + lote + "\" ya existe para \"" + producto.getNombre() + "\".");
            }
            return lote;
        }
        String base = "LT-" + hoy.format(FORMATO_LOTE) + "-" + producto.getCodigo();
        String lote = base;
        for (int n = 2; loteRepository.existsByProductoIdProductoAndNumeroLoteIgnoreCase(id, lote); n++) {
            lote = base + "-" + n;
        }
        return lote;
    }

    /**
     * Anula la compra y retira del inventario el stock que ingresó.
     * No se permite si ya se vendió parte de algún lote de la compra.
     */
    @Transactional
    public void anularCompra(Integer id) {
        CompraEntity compra = compraRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("La compra no existe."));

        if (compra.getEstado() == EstadoCompra.ANULADA) {
            throw new IllegalStateException("La compra ya está anulada.");
        }

        List<DetalleCompraEntity> detalles = detalleCompraRepository.findByCompraConLote(id);
        for (DetalleCompraEntity d : detalles) {
            LoteEntity lote = d.getLote();
            if (lote.getStockActual() < d.getCantidad()) {
                throw new IllegalStateException("ya se vendieron unidades del lote " + lote.getNumeroLote()
                        + " (" + d.getProducto().getNombre() + ").");
            }
        }
        for (DetalleCompraEntity d : detalles) {
            LoteEntity lote = d.getLote();
            lote.setStockActual(lote.getStockActual() - d.getCantidad());
        }

        compra.setEstado(EstadoCompra.ANULADA);
        compraRepository.save(compra);
    }
}
