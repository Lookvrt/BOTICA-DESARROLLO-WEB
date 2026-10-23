document.addEventListener('DOMContentLoaded', function () {
    const modalNuevo = document.getElementById('nuevaCompra');
    const modalEditar = document.getElementById('editarCompra');

    if (modalNuevo && modalNuevo.dataset.abrir === 'true') {
        bootstrap.Modal.getOrCreateInstance(modalNuevo).show();
    }

    if (modalEditar && modalEditar.dataset.abrir === 'true') {
        bootstrap.Modal.getOrCreateInstance(modalEditar).show();
    }
});
