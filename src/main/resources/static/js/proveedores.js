document.addEventListener('DOMContentLoaded', function () {
    const modalNuevo = document.getElementById('modalProveedor');
    const modalEditar = document.getElementById('editarProveedor');

    if (modalNuevo && modalNuevo.dataset.abrir === 'true') {
        bootstrap.Modal.getOrCreateInstance(modalNuevo).show();
    }

    if (modalEditar && modalEditar.dataset.abrir === 'true') {
        bootstrap.Modal.getOrCreateInstance(modalEditar).show();
    }
});
