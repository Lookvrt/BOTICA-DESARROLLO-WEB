document.addEventListener("DOMContentLoaded", function () {
    const ctx = document.getElementById('ventasChart');
    let chartInstance = null;

    function cargarGraficoReporte(periodo) {
        if (!ctx) return;

        fetch('/api/dashboard/ventas-grafico?periodo=' + periodo)
            .then(response => response.json())
            .then(data => {
                if (chartInstance) {
                    chartInstance.destroy();
                }

                chartInstance = new Chart(ctx, {
                    type: 'bar',
                    data: {
                        labels: data.labels,
                        datasets: [{
                            label: 'Venta (S/)',
                            data: data.data,
                            backgroundColor: 'rgba(25, 135, 84, 0.7)',
                            borderColor: '#198754',
                            borderWidth: 1,
                            borderRadius: 6
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: { display: false },
                            tooltip: {
                                callbacks: {
                                    label: function (context) {
                                        return ' Total: S/ ' + context.parsed.y.toFixed(2);
                                    }
                                }
                            }
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
            .catch(err => console.error("Error al cargar gráfico de reportes:", err));
    }

    if (ctx) {
        cargarGraficoReporte('semana');

        const reportButtons = document.querySelectorAll('#reportPeriodSelector button');
        reportButtons.forEach(btn => {
            btn.addEventListener('click', function () {
                reportButtons.forEach(b => {
                    b.classList.remove('active', 'btn-success', 'text-white');
                    b.classList.add('btn-outline-success');
                });
                this.classList.add('active', 'btn-success', 'text-white');
                this.classList.remove('btn-outline-success');

                const periodo = this.getAttribute('data-period');
                cargarGraficoReporte(periodo);
            });
        });
    }

    // Exportar PDF / Imprimir
    const btnPDF = document.getElementById('btnExportPDF');
    if (btnPDF) {
        btnPDF.addEventListener('click', function (e) {
            window.print();
        });
    }

    // Exportar a CSV
    const btnCSV = document.getElementById('btnExportCSV');
    if (btnCSV) {
        btnCSV.addEventListener('click', function () {
            exportarTablaAArchivo('reporte_ventas.csv');
        });
    }

    // Exportar a Excel
    const btnExcel = document.getElementById('btnExportExcel');
    if (btnExcel) {
        btnExcel.addEventListener('click', function () {
            exportarTablaAArchivo('reporte_ventas.xls');
        });
    }

    function exportarTablaAArchivo(filename) {
        const table = document.querySelector("table");
        if (!table) return;

        let csv = [];
        const rows = table.querySelectorAll("tr");

        for (let i = 0; i < rows.length; i++) {
            let row = [], cols = rows[i].querySelectorAll("td, th");
            for (let j = 0; j < cols.length; j++) {
                row.push('"' + cols[j].innerText.trim().replace(/\n/g, ' ') + '"');
            }
            csv.push(row.join(","));
        }

        const csvFile = new Blob([csv.join("\n")], { type: "text/csv;charset=utf-8;" });
        const downloadLink = document.createElement("a");
        downloadLink.download = filename;
        downloadLink.href = window.URL.createObjectURL(csvFile);
        downloadLink.style.display = "none";
        document.body.appendChild(downloadLink);
        downloadLink.click();
        document.body.removeChild(downloadLink);
    }
});