document.addEventListener('DOMContentLoaded', function () {
    const modalNuevo = document.getElementById('modalNuevaCompra');
    const modalEditar = document.getElementById('modalEditarCompra');
    const modalEliminar = document.getElementById('modalEliminarCompra');

    // Apertura automática si hay errores de validación
    if (modalNuevo && modalNuevo.dataset.abrir === 'true') {
        bootstrap.Modal.getOrCreateInstance(modalNuevo).show();
    }

    if (modalEditar && modalEditar.dataset.abrir === 'true') {
        bootstrap.Modal.getOrCreateInstance(modalEditar).show();
    }

    // Modal Editar
    document.querySelectorAll('.btn-editar-compra').forEach(button => {
        button.addEventListener('click', function () {
            const id = this.getAttribute('data-id');
            const proveedor = this.getAttribute('data-proveedor');
            const usuario = this.getAttribute('data-usuario');
            const tipoComprobante = this.getAttribute('data-tipocomprobante');
            const numeroComprobante = this.getAttribute('data-numerocomprobante');
            const total = this.getAttribute('data-total');

            document.getElementById('edit-idCompra').value = id || '';
            document.getElementById('edit-idProveedor').value = proveedor || '';
            document.getElementById('edit-idUsuario').value = usuario || '';
            document.getElementById('edit-tipoComprobante').value = tipoComprobante || '';
            document.getElementById('edit-numeroComprobante').value = numeroComprobante || '';
            document.getElementById('edit-total').value = total || '';

            bootstrap.Modal.getOrCreateInstance(modalEditar).show();
        });
    });

    // Modal Anular
    document.querySelectorAll('.btn-eliminar-compra').forEach(button => {
        button.addEventListener('click', function () {
            const id = this.getAttribute('data-id');
            const comprobante = this.getAttribute('data-comprobante');

            document.getElementById('delete-compra-comprobante').textContent = comprobante || `N° ${id}`;

            const formEliminar = document.getElementById('form-eliminar-compra');
            formEliminar.action = `/compras/anular/${id}`;

            bootstrap.Modal.getOrCreateInstance(modalEliminar).show();
        });
    });

    inicializarDetalleCompra();

    // Listener para el botón de ordenamiento A-Z / Z-A
    const btnToggleOrden = document.getElementById("btnToggleOrden");
    if (btnToggleOrden) {
        btnToggleOrden.addEventListener("click", function () {
            const ordenInput = document.getElementById("ordenInput");
            const form = document.getElementById("formBuscarCompras");

            if (ordenInput && form) {
                ordenInput.value = (ordenInput.value === "asc") ? "desc" : "asc";
                form.submit();
            }
        });
    }
});


// ─────────────────────────────────────────────────────────────
// Modal "Nueva Compra": buscador de productos y tabla de detalle
// ─────────────────────────────────────────────────────────────
function inicializarDetalleCompra() {
    const form = document.getElementById('form-nueva-compra');
    if (!form) return;

    const tbody = document.getElementById('tabla-detalle-compra');
    const filaVacia = document.getElementById('fila-sin-detalle');
    const inputBuscar = document.getElementById('buscar-producto-compra');
    const resultados = document.getElementById('resultados-producto-compra');
    const totalCompra = document.getElementById('total-compra');
    const errorDetalle = document.getElementById('error-detalle-compra');

    const escapar = (s) => String(s ?? '').replace(/[&<>"']/g, c =>
        ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[c]));

    // La fecha de vencimiento debe ser posterior a hoy (fecha local, no UTC)
    const manana = new Date();
    manana.setDate(manana.getDate() + 1);
    const fechaMinima = [manana.getFullYear(), String(manana.getMonth() + 1).padStart(2, '0'),
        String(manana.getDate()).padStart(2, '0')].join('-');

    const filas = () => [...tbody.querySelectorAll('.fila-detalle')];

    function recalcular() {
        let total = 0;
        filas().forEach(fila => {
            const cantidad = parseFloat(fila.querySelector('[data-campo="cantidad"]').value) || 0;
            const precio = parseFloat(fila.querySelector('[data-campo="precioUnitario"]').value) || 0;
            const subtotal = cantidad * precio;
            fila.querySelector('.subtotal-detalle').textContent = subtotal.toFixed(2);
            total += subtotal;
        });
        totalCompra.textContent = 'S/ ' + total.toFixed(2);
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
            existente.querySelector('[data-campo="cantidad"]').focus();
            return;
        }
        const bajoMinimo = p.stockActual < p.stockMinimo;
        const fila = document.createElement('tr');
        fila.className = 'fila-detalle';
        fila.dataset.id = p.idProducto;
        fila.innerHTML = `
            <td class="ps-3">
                <div class="fw-semibold">${escapar(p.nombre)}</div>
                <div class="small text-secondary">${escapar(p.codigo)}</div>
                <input type="hidden" data-campo="idProducto" value="${p.idProducto}">
            </td>
            <td class="text-center small">
                <span class="${bajoMinimo ? 'text-danger fw-semibold' : ''}">${p.stockActual}</span> / ${p.stockMinimo}
            </td>
            <td><input type="number" data-campo="cantidad" class="form-control form-control-sm" min="1" step="1" value="1" required></td>
            <td><input type="number" data-campo="precioUnitario" class="form-control form-control-sm" min="0.01" step="0.01"
                       value="${Number(p.precioCompra).toFixed(2)}" required></td>
            <td><input type="text" data-campo="numeroLote" class="form-control form-control-sm" maxlength="50" placeholder="Automático"></td>
            <td><input type="date" data-campo="fechaVencimiento" class="form-control form-control-sm" min="${fechaMinima}" required></td>
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

    function ocultarResultados() {
        resultados.classList.add('d-none');
    }

    function buscar() {
        const q = inputBuscar.value.trim();
        if (!q) {
            ocultarResultados();
            return;
        }
        if (controlador) controlador.abort();
        controlador = new AbortController();
        fetch('/compras/api/productos?q=' + encodeURIComponent(q), {signal: controlador.signal})
            .then(r => {
                if (!r.ok) throw new Error('HTTP ' + r.status);
                return r.json();
            })
            .then(data => {
                ultimos = data;
                resultados.innerHTML = data.length === 0
                    ? '<div class="list-group-item text-secondary small">No se encontraron productos.</div>'
                    : data.map(p => {
                        const enTabla = filas().some(f => f.dataset.id === String(p.idProducto));
                        return `<button type="button" class="list-group-item list-group-item-action d-flex justify-content-between align-items-center"
                                        data-producto="${p.idProducto}">
                                    <span><span class="fw-semibold">${escapar(p.nombre)}</span>
                                          <span class="small text-secondary ms-1">${escapar(p.codigo)}</span></span>
                                    <span class="small ${p.stockActual < p.stockMinimo ? 'text-danger' : 'text-secondary'}">
                                        ${enTabla ? '<i class="bi bi-check2 text-success me-1"></i>En la compra · ' : ''}Stock ${p.stockActual} / mín. ${p.stockMinimo}
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
            const primero = resultados.querySelector('[data-producto]');
            if (!resultados.classList.contains('d-none') && primero) primero.click();
        } else if (e.key === 'Escape') {
            ocultarResultados();
        }
    });

    resultados.addEventListener('click', (e) => {
        const btn = e.target.closest('[data-producto]');
        if (!btn) return;
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
        // Campos de cabecera (proveedor, usuario, comprobante): validación nativa del navegador
        const cabeceraInvalida = [...form.querySelectorAll(':invalid')].find(el => !el.closest('.fila-detalle'));
        if (cabeceraInvalida) {
            e.preventDefault();
            cabeceraInvalida.reportValidity();
            return;
        }

        const errores = [];
        if (filas().length === 0) errores.push('Agrega al menos un producto a la compra.');

        filas().forEach(fila => {
            const nombre = fila.querySelector('.fw-semibold').textContent;
            const cantidad = parseInt(fila.querySelector('[data-campo="cantidad"]').value, 10);
            const minimo = parseInt(fila.dataset.min, 10);
            const precio = parseFloat(fila.querySelector('[data-campo="precioUnitario"]').value);
            const venc = fila.querySelector('[data-campo="fechaVencimiento"]').value;

            if (!cantidad || cantidad < 1) {
                errores.push(`${nombre}: ingresa una cantidad válida.`);
            } else if (!isNaN(minimo) && cantidad < minimo) {
                errores.push(`${nombre}: la cantidad debe ser mayor a su stock mínimo (${minimo - 1}).`);
            }
            if (!precio || precio <= 0) errores.push(`${nombre}: ingresa el precio unitario.`);
            if (!venc) errores.push(`${nombre}: ingresa la fecha de vencimiento.`);
            else if (venc < fechaMinima) errores.push(`${nombre}: la fecha de vencimiento debe ser posterior a hoy.`);
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

    // Filas precargadas (desde Alertas o tras un error del servidor)
    filas().forEach(fila => fila.querySelector('[data-campo="fechaVencimiento"]').min = fechaMinima);
    recalcular();
}