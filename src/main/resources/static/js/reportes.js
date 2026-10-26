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