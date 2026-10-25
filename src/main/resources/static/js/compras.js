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
});