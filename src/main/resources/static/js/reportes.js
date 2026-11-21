document.addEventListener("DOMContentLoaded", function () {
    let chartInstance = null;
    const canvas = document.getElementById('ventasChart');
    const periodButtons = document.querySelectorAll('#reportPeriodSelector button');

    function cargarGraficoReporte(periodo = 'semana') {
        if (!canvas) return;

        fetch(`/api/reportes/ventas-diarias?periodo=${periodo}`)
            .then(response => {
                if (!response.ok) throw new Error("Error en la respuesta de la API");
                return response.json();
            })
            .then(data => {
                const labels = data.labels || [];
                const values = data.data || [];

                // Destruir instancia previa si existe
                if (chartInstance) {
                    chartInstance.destroy();
                }

                // Crear nueva gráfica
                chartInstance = new Chart(canvas, {
                    type: 'line',
                    data: {
                        labels: labels,
                        datasets: [{
                            label: 'Ventas (S/)',
                            data: values,
                            borderColor: '#198754',
                            backgroundColor: 'rgba(25, 135, 84, 0.15)',
                            fill: true,
                            tension: 0.3,
                            borderWidth: 2,
                            pointRadius: 4,
                            pointBackgroundColor: '#198754'
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: { display: false }
                        },
                        scales: {
                            y: {
                                beginAtZero: true,
                                ticks: {
                                    callback: function (value) {
                                        return 'S/ ' + value;
                                    }
                                }
                            }
                        }
                    }
                });
            })
            .catch(error => console.error("Error al cargar gráfico de reportes:", error));
    }

    // Evento de clic en los botones: Hoy, Semana, Mes
    periodButtons.forEach(btn => {
        btn.addEventListener('click', function () {
            periodButtons.forEach(b => {
                b.classList.remove('btn-success', 'text-white', 'active');
                b.classList.add('btn-outline-success');
            });

            this.classList.remove('btn-outline-success');
            this.classList.add('btn-success', 'text-white', 'active');

            const periodo = this.getAttribute('data-period');
            cargarGraficoReporte(periodo);
        });
    });

    // Carga inicial por defecto (Semana)
    cargarGraficoReporte('semana');
});

// --- Configuración personalizada de exportación (Excel, PDF, CSV) ---
(function initExportConfig() {
    const inputInicio = document.getElementById('exportFechaInicio');
    const inputFin = document.getElementById('exportFechaFin');
    const selectEstado = document.getElementById('exportEstado');
    const colChecks = document.querySelectorAll('.export-col-check');
    const badgeResumen = document.getElementById('badgeResumenExport');

    const linkExcel = document.getElementById('linkExportExcel');
    const linkPDF = document.getElementById('linkExportPDF');
    const linkCSV = document.getElementById('linkExportCSV');

    if (!linkExcel || !inputInicio) return;

    const formatearISO = (date) => date.toISOString().split('T')[0];

    // Atajos rápidos de fecha
    document.querySelectorAll('#presetsFechaExport button').forEach(btn => {
        btn.addEventListener('click', function () {
            document.querySelectorAll('#presetsFechaExport button').forEach(b => b.classList.remove('active'));
            this.classList.add('active');

            const hoy = new Date();
            const preset = this.dataset.preset;

            if (preset === 'todo') {
                inputInicio.value = '';
                inputFin.value = '';
            } else if (preset === 'hoy') {
                inputInicio.value = formatearISO(hoy);
                inputFin.value = formatearISO(hoy);
            } else if (preset === 'semana') {
                const hace7 = new Date();
                hace7.setDate(hoy.getDate() - 6);
                inputInicio.value = formatearISO(hace7);
                inputFin.value = formatearISO(hoy);
            } else if (preset === 'mes') {
                const inicioMes = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
                inputInicio.value = formatearISO(inicioMes);
                inputFin.value = formatearISO(hoy);
            }
        });
    });

    // Marcar / desmarcar todas las columnas
    const btnToggleCols = document.getElementById('btnToggleCols');
    if (btnToggleCols) {
        btnToggleCols.addEventListener('click', () => {
            const todasMarcadas = Array.from(colChecks).every(c => c.checked);
            colChecks.forEach(c => c.checked = !todasMarcadas);
            if (todasMarcadas && colChecks.length > 0) colChecks[0].checked = true; // Mínimo 1
            btnToggleCols.textContent = todasMarcadas ? 'Marcar todas' : 'Dejar solo ID';
        });
    }

    function actualizarEnlacesExportacion() {
        const params = new URLSearchParams();
        if (inputInicio.value) params.set('inicio', inputInicio.value);
        if (inputFin.value) params.set('fin', inputFin.value);
        params.set('estado', selectEstado.value);

        const seleccionadas = Array.from(colChecks).filter(c => c.checked).map(c => c.value);
        if (seleccionadas.length === 0) {
            colChecks[0].checked = true;
            seleccionadas.push(colChecks[0].value);
        }
        seleccionadas.forEach(col => params.append('cols', col));

        const query = '?' + params.toString();
        linkExcel.href = '/reporte/exportar/excel' + query;
        linkPDF.href = '/reporte/exportar/pdf' + query;
        linkCSV.href = '/reporte/exportar/csv' + query;

        // Actualizar etiqueta visual
        const textoEstado = selectEstado.value === 'REGISTRADA' ? 'Registradas'
            : (selectEstado.value === 'ANULADA' ? 'Anuladas' : 'Todas');
        const textoFecha = (inputInicio.value || inputFin.value)
            ? `${inputInicio.value || '...'} a ${inputFin.value || '...'}`
            : 'Todo el historial';
        badgeResumen.textContent = `${textoEstado} · ${textoFecha} · ${seleccionadas.length} cols`;
    }

    document.getElementById('btnAplicarExportConfig')?.addEventListener('click', () => {
        actualizarEnlacesExportacion();
        const modalEl = document.getElementById('modalConfigExport');
        bootstrap.Modal.getInstance(modalEl)?.hide();
    });

    document.getElementById('btnResetExportConfig')?.addEventListener('click', () => {
        inputInicio.value = '';
        inputFin.value = '';
        selectEstado.value = 'REGISTRADA';
        const defaults = ['id', 'fecha', 'comprobante', 'metodoPago', 'total', 'estado'];
        colChecks.forEach(c => c.checked = defaults.includes(c.value));
        document.querySelectorAll('#presetsFechaExport button').forEach(b => {
            b.classList.toggle('active', b.dataset.preset === 'todo');
        });
        actualizarEnlacesExportacion();
    });

    // Inicializar URLs con los valores por defecto al cargar
    actualizarEnlacesExportacion();
})();