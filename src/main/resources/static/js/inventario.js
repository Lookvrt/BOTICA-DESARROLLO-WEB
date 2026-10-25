// Generador de código sugerido e.g. MED010
function generarCodigoSugerido() {
    const aleatorio = Math.floor(100 + Math.random() * 900);
    document.getElementById('nuevoCodigo').value = 'MED' + aleatorio;
}

document.addEventListener('DOMContentLoaded', function () {
    // Asignar eventos a los botones de la tabla usando data-attributes
    document.querySelectorAll('.btn-ver-lotes').forEach(function (btn) {
        btn.addEventListener('click', function () {
            const id = this.getAttribute('data-id');
            const nombre = this.getAttribute('data-nombre');
            abrirModalLotes(id, nombre);
        });
    });

    document.querySelectorAll('.btn-nuevo-lote').forEach(function (btn) {
        btn.addEventListener('click', function () {
            const id = this.getAttribute('data-id');
            const nombre = this.getAttribute('data-nombre');
            const precio = this.getAttribute('data-precio');
            abrirModalNuevoLote(id, nombre, precio);
        });
    });

    document.querySelectorAll('.btn-editar-prod').forEach(function (btn) {
        btn.addEventListener('click', function () {
            const id = this.getAttribute('data-id');
            abrirModalEditar(id);
        });
    });

    document.querySelectorAll('.btn-eliminar-prod').forEach(function (btn) {
        btn.addEventListener('click', function () {
            const id = this.getAttribute('data-id');
            const nombre = this.getAttribute('data-nombre');
            confirmarEliminar(id, nombre);
        });
    });
});

// Modal Editar Producto
function abrirModalEditar(idProducto) {
    fetch('/inventario/api/producto/' + idProducto)
        .then(res => res.json())
        .then(p => {
            document.getElementById('editIdProducto').value = p.idProducto;
            document.getElementById('editCodigo').value = p.codigo || '';
            document.getElementById('editNombre').value = p.nombre || '';
            document.getElementById('editPrincipio').value = p.principioActivo || '';
            document.getElementById('editConcentracion').value = p.concentracion || '';
            document.getElementById('editPresentacion').value = p.presentacion || '';
            document.getElementById('editCategoria').value = p.idCategoria || '';
            document.getElementById('editPrecioCompra').value = p.precioCompra || '0.00';
            document.getElementById('editPrecioVenta').value = p.precioVenta || '0.00';
            document.getElementById('editStockMinimo').value = p.stockMinimo || '5';
            document.getElementById('editReceta').checked = (p.requiereReceta === 1);

            const modal = new bootstrap.Modal(document.getElementById('modalEditarProducto'));
            modal.show();
        })
        .catch(err => {
            console.error("Error al cargar producto:", err);
            alert("No se pudieron cargar los datos del producto.");
        });
}

// Modal Agregar Lote
function abrirModalNuevoLote(idProducto, nombreProducto, precioCompra) {
    document.getElementById('loteIdProducto').value = idProducto;
    document.getElementById('loteProductoNombre').innerText = nombreProducto;
    document.getElementById('lotePrecioCompra').value = precioCompra || '0.00';
    document.getElementById('loteNumero').value = '';
    document.getElementById('loteCantidad').value = '';
    document.getElementById('loteFechaFabricacion').value = new Date().toISOString().split('T')[0];

    // Sugerir fecha de vencimiento a 2 años por defecto
    const hoy = new Date();
    hoy.setFullYear(hoy.getFullYear() + 2);
    document.getElementById('loteFechaVencimiento').value = hoy.toISOString().split('T')[0];

    const modal = new bootstrap.Modal(document.getElementById('modalAgregarLote'));
    modal.show();
}

// Modal Ver Lotes
function abrirModalLotes(idProducto, nombreProducto) {
    document.getElementById('verLotesProductoNombre').innerText = nombreProducto;
    const tbody = document.getElementById('tablaLotesBody');
    tbody.innerHTML = '<tr><td colspan="6" class="text-center py-3 text-muted"><i class="bi bi-hourglass-split me-1"></i> Cargando lotes...</td></tr>';

    const modal = new bootstrap.Modal(document.getElementById('modalVerLotes'));
    modal.show();

    fetch('/inventario/api/producto/' + idProducto + '/lotes')
        .then(res => res.json())
        .then(lotes => {
            if (!lotes || lotes.length === 0) {
                tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted"><i class="bi bi-inbox fs-4 d-block mb-1"></i> Este producto aún no tiene lotes registrados.</td></tr>';
                return;
            }

            const hoy = new Date();
            tbody.innerHTML = '';
            lotes.forEach(l => {
                const fechaVenc = l.fechaVencimiento ? new Date(l.fechaVencimiento) : null;
                let estadoBadge = '<span class="badge bg-success bg-opacity-10 text-success">Vigente</span>';

                if (fechaVenc) {
                    const diffTime = fechaVenc - hoy;
                    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
                    if (diffDays < 0) {
                        estadoBadge = '<span class="badge bg-danger text-white">Vencido</span>';
                    } else if (diffDays <= 45) {
                        estadoBadge = '<span class="badge bg-warning text-dark">Próx. a vencer (' + diffDays + ' d)</span>';
                    }
                }

                const fila = `
                        <tr>
                            <td class="fw-bold">${l.numeroLote || '-'}</td>
                            <td class="text-muted small">${l.fechaFabricacion || '-'}</td>
                            <td class="fw-semibold">${l.fechaVencimiento || '-'}</td>
                            <td class="text-center fw-bold fs-6">${l.stockActual}</td>
                            <td class="text-end">S/. ${(l.precioCompra || 0).toFixed(2)}</td>
                            <td class="text-center">${estadoBadge}</td>
                        </tr>
                    `;
                tbody.innerHTML += fila;
            });
        })
        .catch(err => {
            console.error("Error al cargar lotes:", err);
            tbody.innerHTML = '<tr><td colspan="6" class="text-center py-3 text-danger">Error al cargar lotes.</td></tr>';
        });
}

// Modal Eliminar Producto
function confirmarEliminar(idProducto, nombreProducto) {
    document.getElementById('eliminarProductoNombre').innerText = nombreProducto;
    document.getElementById('formEliminar').action = '/inventario/eliminar/' + idProducto;
    const modal = new bootstrap.Modal(document.getElementById('modalEliminarProducto'));
    modal.show();
}