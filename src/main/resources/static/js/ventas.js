// ─────────────────────────────────────────────────────────────
// Módulo Ventas: listado, modal de nueva/editar venta y anulación
// ─────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', function () {
    const modalVenta = document.getElementById('modalVenta');
    const modalAnular = document.getElementById('modalAnularVenta');

    // Apertura automática: al editar (/ventas/editar/{id}), tras un error, o con #nueva en la URL
    if (modalVenta.dataset.abrir === 'true' || location.hash === '#nueva') {
        bootstrap.Modal.getOrCreateInstance(modalVenta).show();
    }

    // Nueva Venta: si el formulario tiene una venta en edición, se recarga uno vacío
    document.getElementById('btn-nueva-venta').addEventListener('click', () => {
        if (modalVenta.dataset.edicion === 'true') {
            window.location.href = '/ventas#nueva';
        } else {
            bootstrap.Modal.getOrCreateInstance(modalVenta).show();
        }
    });

    // Anular
    document.querySelectorAll('.btn-anular-venta').forEach(button => {
        button.addEventListener('click', function () {
            document.getElementById('anular-venta-comprobante').textContent = this.dataset.comprobante;
            document.getElementById('form-anular-venta').action = `/ventas/anular/${this.dataset.id}`;
            bootstrap.Modal.getOrCreateInstance(modalAnular).show();
        });
    });

    inicializarDetalleVenta();
});

function inicializarDetalleVenta() {
    const form = document.getElementById('form-venta');
    const tbody = document.getElementById('tabla-detalle-venta');
    const filaVacia = document.getElementById('fila-sin-detalle');
    const inputBuscar = document.getElementById('buscar-producto-venta');
    const resultados = document.getElementById('resultados-producto-venta');
    const totalVenta = document.getElementById('total-venta');
    const errorDetalle = document.getElementById('error-detalle-venta');
    const selectCliente = document.getElementById('venta-idCliente');
    // Venta antigua registrada solo con su total (sin detalle de productos)
    const sinProductos = form.dataset.sinProductos === 'true';
    const totalOriginal = Number(form.dataset.totalOriginal || 0);

    const escapar = (s) => String(s ?? '').replace(/[&<>"']/g, c =>
        ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[c]));
    const filas = () => [...tbody.querySelectorAll('.fila-detalle')];

    function recalcular() {
        let total = 0;
        filas().forEach(fila => {
            const cantidad = parseInt(fila.querySelector('[data-campo="cantidad"]').value, 10) || 0;
            const subtotal = cantidad * Number(fila.dataset.precio);
            fila.querySelector('.subtotal-detalle').textContent = subtotal.toFixed(2);
            total += subtotal;
        });
        if (sinProductos && filas().length === 0) total = totalOriginal;
        totalVenta.textContent = 'S/ ' + total.toFixed(2);
        filaVacia.classList.toggle('d-none', filas().length > 0);
    }

    // Asigna los nombres detalles[i].campo que Spring necesita para armar la lista
    function reindexar() {
        filas().forEach((fila, i) => {
            fila.querySelectorAll('[data-campo]').forEach(input => {
                input.name = `detalles[${i}].${input.dataset.campo}`;
            });
        });
    }

    function agregarProducto(p) {
        const existente = filas().find(f => f.dataset.id === String(p.idProducto));
        if (existente) {
            const input = existente.querySelector('[data-campo="cantidad"]');
            if (parseInt(input.value, 10) < Number(existente.dataset.stock)) input.value = parseInt(input.value, 10) + 1;
            input.focus();
            recalcular();
            return;
        }
        if (p.stock <= 0) return;
        const fila = document.createElement('tr');
        fila.className = 'fila-detalle';
        fila.dataset.id = p.idProducto;
        fila.dataset.stock = p.stock;
        fila.dataset.precio = p.precio;
        fila.innerHTML = `
            <td class="ps-3">
                <div class="fw-semibold nombre-detalle">${escapar(p.nombre)}${p.requiereReceta
                    ? ' <span class="badge bg-warning bg-opacity-25 text-dark">Receta</span>' : ''}</div>
                <div class="small text-secondary">${escapar(p.codigo)}</div>
                <input type="hidden" data-campo="idProducto" value="${p.idProducto}">
            </td>
            <td class="text-center">${p.stock}</td>
            <td><input type="number" data-campo="cantidad" class="form-control form-control-sm" min="1" max="${p.stock}"
                       step="1" value="1" required></td>
            <td class="text-end">${Number(p.precio).toFixed(2)}</td>
            <td class="text-end fw-semibold subtotal-detalle">0.00</td>
            <td class="text-end pe-3">
                <button type="button" class="btn btn-sm btn-link text-danger p-0 btn-quitar-detalle" title="Quitar">
                    <i class="bi bi-x-lg"></i>
                </button>
            </td>`;
        tbody.insertBefore(fila, filaVacia);
        recalcular();
        fila.querySelector('[data-campo="cantidad"]').focus();
    }

    // ── Buscador ──
    let ultimos = [];
    let debounce;
    let controlador;

    const ocultarResultados = () => resultados.classList.add('d-none');

    function buscar() {
        const q = inputBuscar.value.trim();
        if (!q) {
            ocultarResultados();
            return;
        }
        if (controlador) controlador.abort();
        controlador = new AbortController();
        fetch('/ventas/api/productos?q=' + encodeURIComponent(q), {signal: controlador.signal})
            .then(r => {
                if (!r.ok) throw new Error('HTTP ' + r.status);
                return r.json();
            })
            .then(data => {
                // Al editar, un producto que ya está en la venta tiene disponibles también sus propias unidades
                ultimos = data.map(p => {
                    const fila = filas().find(f => f.dataset.id === String(p.idProducto));
                    return fila ? {...p, stock: Number(fila.dataset.stock)} : p;
                });
                resultados.innerHTML = ultimos.length === 0
                    ? '<div class="list-group-item text-secondary small">No se encontraron productos.</div>'
                    : ultimos.map(p => {
                        const enTabla = filas().some(f => f.dataset.id === String(p.idProducto));
                        const sinStock = p.stock <= 0;
                        return `<button type="button" class="list-group-item list-group-item-action d-flex justify-content-between align-items-center"
                                        data-producto="${p.idProducto}" ${sinStock ? 'disabled' : ''}>
                                    <span><span class="fw-semibold">${escapar(p.nombre)}</span>
                                          <span class="small text-secondary ms-1">${escapar(p.codigo)}</span></span>
                                    <span class="small ${sinStock ? 'text-danger' : 'text-secondary'}">
                                        ${enTabla ? '<i class="bi bi-check2 text-success me-1"></i>En la venta · ' : ''}
                                        ${sinStock ? 'Sin stock' : 'Disponible ' + p.stock} · S/ ${Number(p.precio).toFixed(2)}
                                    </span>
                                </button>`;
                    }).join('');
                resultados.classList.remove('d-none');
            })
            .catch(err => {
                if (err.name !== 'AbortError') {
                    resultados.innerHTML = '<div class="list-group-item text-danger small">No se pudo buscar productos.</div>';
                    resultados.classList.remove('d-none');
                }
            });
    }

    inputBuscar.addEventListener('input', () => {
        clearTimeout(debounce);
        debounce = setTimeout(buscar, 300);
    });

    inputBuscar.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault(); // no enviar el formulario
            const primero = resultados.querySelector('[data-producto]:not([disabled])');
            if (!resultados.classList.contains('d-none') && primero) primero.click();
        } else if (e.key === 'Escape') {
            ocultarResultados();
        }
    });

    resultados.addEventListener('click', (e) => {
        const btn = e.target.closest('[data-producto]');
        if (!btn || btn.disabled) return;
        const producto = ultimos.find(p => String(p.idProducto) === btn.dataset.producto);
        if (producto) agregarProducto(producto);
        inputBuscar.value = '';
        ocultarResultados();
    });

    document.addEventListener('click', (e) => {
        if (!resultados.contains(e.target) && e.target !== inputBuscar) ocultarResultados();
    });

    // ── Tabla ──
    tbody.addEventListener('input', recalcular);
    tbody.addEventListener('click', (e) => {
        const btn = e.target.closest('.btn-quitar-detalle');
        if (!btn) return;
        btn.closest('.fila-detalle').remove();
        recalcular();
    });

    // ── Validación antes de enviar ──
    form.addEventListener('submit', (e) => {
        const errores = [];
        if (selectCliente.dataset.requerido === 'true' && !selectCliente.value) {
            errores.push('Selecciona el cliente: las facturas requieren un cliente con RUC.');
        }
        if (filas().length === 0 && !sinProductos) errores.push('Agrega al menos un producto a la venta.');

        filas().forEach(fila => {
            const nombre = fila.querySelector('.nombre-detalle').childNodes[0].textContent.trim();
            const cantidad = parseInt(fila.querySelector('[data-campo="cantidad"]').value, 10);
            const stock = Number(fila.dataset.stock);
            if (!cantidad || cantidad < 1) {
                errores.push(`${nombre}: ingresa una cantidad válida.`);
            } else if (cantidad > stock) {
                errores.push(`${nombre}: solo hay ${stock} unidades disponibles.`);
            }
        });

        if (errores.length > 0) {
            e.preventDefault();
            errorDetalle.innerHTML = errores.map(escapar).join('<br>');
            errorDetalle.classList.remove('d-none');
            errorDetalle.scrollIntoView({behavior: 'smooth', block: 'nearest'});
            return;
        }
        errorDetalle.classList.add('d-none');
        reindexar();
    });

    recalcular();
}
