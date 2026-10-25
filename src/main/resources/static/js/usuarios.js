document.addEventListener('DOMContentLoaded', () => {
    // Modal Editar
    const modalEditar = new bootstrap.Modal(document.getElementById('modalEditarUsuario'));
    document.querySelectorAll('.btn-editar').forEach(btn => {
        btn.addEventListener('click', () => {
            document.getElementById('edit-idUsuario').value = btn.dataset.id;
            document.getElementById('edit-nombres').value   = btn.dataset.nombres;
            document.getElementById('edit-apellidos').value = btn.dataset.apellidos;
            document.getElementById('edit-usuario').value   = btn.dataset.usuario;
            document.getElementById('edit-email').value     = btn.dataset.email;
            document.getElementById('edit-rol').value       = btn.dataset.rol;
            modalEditar.show();
        });
    });

    // Modal Eliminar
    const modalEliminar = new bootstrap.Modal(document.getElementById('modalEliminarUsuario'));
    const formEliminar  = document.getElementById('form-eliminar');
    const deleteNombre  = document.getElementById('delete-user-nombre');

    document.querySelectorAll('.btn-eliminar').forEach(btn => {
        btn.addEventListener('click', () => {
            const id = btn.dataset.id;
            const nombre = btn.dataset.nombre;
            deleteNombre.textContent = nombre;
            formEliminar.action = '/usuarios/eliminar/' + id;
            modalEliminar.show();
        });
    });
});