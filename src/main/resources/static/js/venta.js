// ─────────────────────────────────────────────────────────────
// Punto de Venta: búsqueda de productos, carrito y cobro
// ─────────────────────────────────────────────────────────────
(function () {
    const carrito = new Map(); // idProducto -> { producto, cantidad }
    let ultimosResultados = [];
    let buscando = null;
    let mensajeVacio = 'Escribe el nombre, código o principio activo de un producto.';

    const $ = (id) => document.getElementById(id);
    const formBuscar = $('form-buscar');
    const inputBuscar = $('input-buscar');
    const tablaResultados = $('tabla-resultados');
    const tablaCarrito = $('tabla-carrito');
    const totalPagar = $('total-pagar');
    const metodoPago = $('metodo-pago');
    const bloqueEfectivo = $('bloque-efectivo');
    const montoRecibido = $('monto-recibido');
    const vuelto = $('vuelto');
    const btnCobrar = $('btn-cobrar');
    const btnCancelar = $('btn-cancelar');
    const alerta = $('alerta-venta');

    const soles = (n) => 'S/ ' + Number(n).toFixed(2);
    const escapar = (s) => String(s ?? '').replace(/[&<>"']/g, c =>
        ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[c]));

    function mostrarAlerta(mensaje, tipo) {
        alerta.className = 'alert alert-' + tipo;
        alerta.textContent = mensaje;
        clearTimeout(mostrarAlerta.t);
        mostrarAlerta.t = setTimeout(() => alerta.className = 'alert d-none', 6000);
    }

    // ── Búsqueda ──────────────────────────────────────────────
    function buscar() {
        const q = inputBuscar.value.trim();
        if (!q) {
            ultimosResultados = [];
            renderResultados('Escribe el nombre, código o principio activo de un producto.');
            return;
        }
        if (buscando) buscando.abort();
        buscando = new AbortController();
        fetch('/venta/api/productos?q=' + encodeURIComponent(q), {signal: buscando.signal})
            .then(r => {
                if (!r.ok) throw new Error('HTTP ' + r.status);
                return r.json();
            })
            .then(data => {
                ultimosResultados = data;
                renderResultados('No se encontraron productos para "' + q + '".');
            })
            .catch(err => {
                if (err.name !== 'AbortError') mostrarAlerta('No se pudo buscar productos. ¿Sigue activa tu sesión?', 'danger');
            });
    }

    function renderResultados(mensaje) {
        mensajeVacio = mensaje;
        if (ultimosResultados.length === 0) {
            tablaResultados.innerHTML = `<tr><td colspan="4" class="text-center text-secondary py-4">${escapar(mensajeVacio)}</td></tr>`;
            return;
        }
        tablaResultados.innerHTML = ultimosResultados.map(p => {
            const enCarrito = carrito.get(p.idProducto)?.cantidad || 0;
            const restante = p.stock - enCarrito;
            const receta = p.requiereReceta
                ? '<span class="badge bg-warning bg-opacity-25 text-dark ms-1">Receta</span>' : '';
            const detalle = p.presentacion ? `<div class="small text-secondary fw-normal">${escapar(p.codigo)} · ${escapar(p.presentacion)}</div>` : '';
            return `<tr>
                <td class="fw-bold">${escapar(p.nombre)}${receta}${detalle}</td>
                <td class="text-center ${p.stock === 0 ? 'text-danger' : ''}">${p.stock}</td>
                <td class="text-center">${Number(p.precio).toFixed(2)}</td>
                <td class="text-end">
                    <button class="btn btn-sm bg-success bg-opacity-50 text-dark fw-medium border-0 px-3 rounded-2"
                            data-agregar="${p.idProducto}" ${restante <= 0 ? 'disabled' : ''}>
                        ${p.stock === 0 ? 'Sin stock' : '+ Añadir'}
                    </button>
                </td>
            </tr>`;
        }).join('');
    }

    formBuscar.addEventListener('submit', (e) => {
        e.preventDefault();
        buscar();
    });

    let debounce;
    inputBuscar.addEventListener('input', () => {
        clearTimeout(debounce);
        debounce = setTimeout(buscar, 300);
    });

    tablaResultados.addEventListener('click', (e) => {
        const btn = e.target.closest('[data-agregar]');
        if (!btn) return;
        const producto = ultimosResultados.find(p => p.idProducto === Number(btn.dataset.agregar));
        if (producto) agregar(producto);
    });

    // ── Carrito ───────────────────────────────────────────────
    function agregar(producto) {
        const item = carrito.get(producto.idProducto);
        const actual = item ? item.cantidad : 0;
        if (actual + 1 > producto.stock) {
            mostrarAlerta('No hay más stock disponible de ' + producto.nombre + '.', 'warning');
            return;
        }
        carrito.set(producto.idProducto, {producto, cantidad: actual + 1});
        renderCarrito();
    }

    function cambiarCantidad(id, cantidad) {
        const item = carrito.get(id);
        if (!item) return;
        if (cantidad <= 0) {
            carrito.delete(id);
        } else if (cantidad > item.producto.stock) {
            mostrarAlerta('Solo hay ' + item.producto.stock + ' unidades de ' + item.producto.nombre + '.', 'warning');
            item.cantidad = item.producto.stock;
        } else {
            item.cantidad = cantidad;
        }
        renderCarrito();
    }

    function calcularTotal() {
        let total = 0;
        carrito.forEach(({producto, cantidad}) => total += Number(producto.precio) * cantidad);
        return Math.round(total * 100) / 100;
    }

    function renderCarrito() {
        if (carrito.size === 0) {
            tablaCarrito.innerHTML = '<tr><td colspan="4" class="text-center text-secondary py-4">El carrito está vacío.</td></tr>';
        } else {
            tablaCarrito.innerHTML = Array.from(carrito.values()).map(({producto, cantidad}) => `<tr>
                <td class="fw-bold">${escapar(producto.nombre)}</td>
                <td class="text-center" style="width: 90px;">
                    <input type="number" class="form-control form-control-sm text-center" min="1" max="${producto.stock}"
                           value="${cantidad}" data-cantidad="${producto.idProducto}">
                </td>
                <td class="text-end">${(Number(producto.precio) * cantidad).toFixed(2)}</td>
                <td class="text-end">
                    <button class="btn btn-sm btn-link text-danger p-0" data-quitar="${producto.idProducto}" title="Quitar">
                        <i class="bi bi-x-lg"></i>
                    </button>
                </td>
            </tr>`).join('');
        }
        totalPagar.textContent = soles(calcularTotal());
        actualizarVuelto();
        renderResultados(mensajeVacio); // actualiza botones "+ Añadir" según lo que ya está en el carrito
    }

    tablaCarrito.addEventListener('change', (e) => {
        const input = e.target.closest('[data-cantidad]');
        if (input) cambiarCantidad(Number(input.dataset.cantidad), parseInt(input.value, 10) || 0);
    });

    tablaCarrito.addEventListener('click', (e) => {
        const btn = e.target.closest('[data-quitar]');
        if (btn) cambiarCantidad(Number(btn.dataset.quitar), 0);
    });

    // ── Pago ──────────────────────────────────────────────────
    function esEfectivo() {
        return metodoPago.value === 'EFECTIVO';
    }

    function actualizarVuelto() {
        bloqueEfectivo.classList.toggle('d-none', !esEfectivo());
        const recibido = parseFloat(montoRecibido.value);
        const diferencia = isNaN(recibido) ? 0 : recibido - calcularTotal();
        vuelto.textContent = soles(Math.max(diferencia, 0));
        vuelto.classList.toggle('text-danger', !isNaN(recibido) && diferencia < 0);
    }

    metodoPago.addEventListener('change', actualizarVuelto);
    montoRecibido.addEventListener('input', actualizarVuelto);

    function limpiar() {
        carrito.clear();
        montoRecibido.value = '';
        renderCarrito();
    }

    btnCancelar.addEventListener('click', () => {
        if (carrito.size === 0 || confirm('¿Vaciar el carrito y cancelar la venta?')) limpiar();
    });

    btnCobrar.addEventListener('click', () => {
        if (carrito.size === 0) {
            mostrarAlerta('Agrega al menos un producto al carrito.', 'warning');
            return;
        }
        const total = calcularTotal();
        const recibido = parseFloat(montoRecibido.value);
        if (esEfectivo() && !isNaN(recibido) && recibido < total) {
            mostrarAlerta('El monto recibido es menor al total a pagar.', 'warning');
            return;
        }

        const csrf = $('csrf-token');
        const headers = {'Content-Type': 'application/json'};
        if (csrf) headers[csrf.dataset.header] = csrf.value;

        const body = {
            metodoPago: metodoPago.value,
            items: Array.from(carrito.values()).map(({producto, cantidad}) => ({idProducto: producto.idProducto, cantidad}))
        };

        btnCobrar.disabled = true;
        fetch('/venta/api/registrar', {method: 'POST', headers, body: JSON.stringify(body)})
            .then(r => r.json().catch(() => ({})).then(data => ({ok: r.ok, data})))
            .then(({ok, data}) => {
                if (!ok) {
                    mostrarAlerta(data.error || 'No se pudo registrar la venta.', 'danger');
                    buscar(); // refrescar stock mostrado
                    return;
                }
                $('ok-comprobante').textContent = data.numeroComprobante;
                $('ok-total').textContent = soles(data.total);
                $('ok-metodo').textContent = data.metodoPago;
                const mostrarVuelto = esEfectivo() && !isNaN(recibido);
                $('ok-vuelto-linea').classList.toggle('d-none', !mostrarVuelto);
                if (mostrarVuelto) $('ok-vuelto').textContent = soles(recibido - Number(data.total));
                bootstrap.Modal.getOrCreateInstance($('modal-venta-ok')).show();
                limpiar();
                buscar(); // refrescar stock mostrado
            })
            .catch(() => mostrarAlerta('Error de conexión al registrar la venta.', 'danger'))
            .finally(() => btnCobrar.disabled = false);
    });

    renderCarrito();
})();
