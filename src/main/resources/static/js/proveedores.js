document.addEventListener('DOMContentLoaded', function () {
    const modalNuevo = document.getElementById('modalNuevoProveedor');
    const modalEditar = document.getElementById('modalEditarProveedor');
    const modalEliminar = document.getElementById('modalEliminarProveedor');

    // Apertura automática si hay errores de validación desde el backend
    if (modalNuevo && modalNuevo.dataset.abrir === 'true') {
        bootstrap.Modal.getOrCreateInstance(modalNuevo).show();
    }

    if (modalEditar && modalEditar.dataset.abrir === 'true') {
        bootstrap.Modal.getOrCreateInstance(modalEditar).show();
    }

    // Eventos para botones de Editar
    document.querySelectorAll('.btn-editar-proveedor').forEach(button => {
        button.addEventListener('click', function () {
            const id = this.getAttribute('data-id');
            const razonSocial = this.getAttribute('data-razonsocial');
            const ruc = this.getAttribute('data-ruc');
            const correo = this.getAttribute('data-correo');
            const telefono = this.getAttribute('data-telefono');
            const direccion = this.getAttribute('data-direccion');
            const estado = this.getAttribute('data-estado');

            document.getElementById('edit-idProveedor').value = id || '';
            document.getElementById('edit-razonSocial').value = razonSocial || '';
            document.getElementById('edit-ruc').value = ruc || '';
            document.getElementById('edit-correo').value = correo || '';
            document.getElementById('edit-telefono').value = telefono || '';
            document.getElementById('edit-direccion').value = direccion || '';
            document.getElementById('edit-estado').value = estado !== null ? estado : '1';

            bootstrap.Modal.getOrCreateInstance(modalEditar).show();
        });
    });

    // Eventos para botones de Eliminar
    document.querySelectorAll('.btn-eliminar-proveedor').forEach(button => {
        button.addEventListener('click', function () {
            const id = this.getAttribute('data-id');
            const nombre = this.getAttribute('data-nombre');

            document.getElementById('delete-proveedor-nombre').textContent = nombre || `PRV-${id}`;

            const formEliminar = document.getElementById('form-eliminar-proveedor');
            formEliminar.action = `/proveedores/eliminar/${id}`;

            bootstrap.Modal.getOrCreateInstance(modalEliminar).show();
        });
    });
});