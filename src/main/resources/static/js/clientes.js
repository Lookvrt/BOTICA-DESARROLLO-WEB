document.addEventListener('DOMContentLoaded', function () {
    const modalCliente = document.getElementById('modalCliente');
    const modalEliminar = document.getElementById('modalEliminarCliente');

    // Apertura automática: al editar (/clientes/editar/{id}), tras un error, o con #nuevo en la URL
    if (modalCliente.dataset.abrir === 'true' || location.hash === '#nuevo') {
        bootstrap.Modal.getOrCreateInstance(modalCliente).show();
    }

    // Nuevo Cliente: si el formulario tiene un cliente en edición, se recarga uno vacío
    document.getElementById('btn-nuevo-cliente').addEventListener('click', () => {
        if (modalCliente.dataset.edicion === 'true') {
            window.location.href = '/clientes#nuevo';
        } else {
            bootstrap.Modal.getOrCreateInstance(modalCliente).show();
        }
    });

    // Eliminar
    document.querySelectorAll('.btn-eliminar-cliente').forEach(button => {
        button.addEventListener('click', function () {
            document.getElementById('delete-cliente-nombre').textContent = this.dataset.nombre;
            document.getElementById('form-eliminar-cliente').action = `/clientes/eliminar/${this.dataset.id}`;
            bootstrap.Modal.getOrCreateInstance(modalEliminar).show();
        });
    });

    // Orden A-Z / Z-A
    document.getElementById('btnToggleOrden').addEventListener('click', function () {
        const ordenInput = document.getElementById('ordenInput');
        ordenInput.value = ordenInput.value === 'asc' ? 'desc' : 'asc';
        document.getElementById('formBuscarClientes').submit();
    });

    // ── Validación del RUC (SUNAT) mientras se escribe ──
    const FACTORES = [5, 4, 3, 2, 7, 6, 5, 4, 3, 2];
    const PREFIJOS = ['10', '15', '16', '17', '20'];

    function errorRuc(ruc) {
        if (!/^\d{11}$/.test(ruc)) return 'El RUC debe tener exactamente 11 dígitos.';
        if (!PREFIJOS.includes(ruc.slice(0, 2))) return 'El RUC debe empezar con 10, 15, 16, 17 o 20.';
        const suma = FACTORES.reduce((acc, f, i) => acc + f * Number(ruc[i]), 0);
        let digito = 11 - (suma % 11);
        if (digito === 10) digito = 0;
        if (digito === 11) digito = 1;
        return digito === Number(ruc[10]) ? null : 'El dígito verificador no corresponde: el RUC no es válido según SUNAT.';
    }

    const inputRuc = document.getElementById('ruc');
    const errorRucDiv = document.getElementById('ruc-error');

    inputRuc.addEventListener('input', () => {
        inputRuc.value = inputRuc.value.replace(/\D/g, '').slice(0, 11);
        if (inputRuc.value.length === 11) {
            const err = errorRuc(inputRuc.value);
            inputRuc.classList.toggle('is-invalid', !!err);
            inputRuc.classList.toggle('is-valid', !err);
            if (err) errorRucDiv.textContent = err;
        } else {
            inputRuc.classList.remove('is-invalid', 'is-valid');
        }
    });

    document.getElementById('form-cliente').addEventListener('submit', (e) => {
        const err = errorRuc(inputRuc.value.trim());
        const razon = document.getElementById('razonSocial');
        if (err) {
            e.preventDefault();
            inputRuc.classList.add('is-invalid');
            errorRucDiv.textContent = err;
            inputRuc.focus();
        } else if (!razon.value.trim()) {
            e.preventDefault();
            razon.classList.add('is-invalid');
            razon.focus();
        }
    });
});
