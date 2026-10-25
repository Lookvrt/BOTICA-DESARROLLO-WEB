function initDashboardCharts() {
    const rawData = window.DASHBOARD_DATA || {};

    const semanaLabels = (rawData.semana && rawData.semana.labels && rawData.semana.labels.length > 0)
        ? rawData.semana.labels : ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];
    const semanaData = (rawData.semana && rawData.semana.data && rawData.semana.data.length > 0)
        ? rawData.semana.data : [0, 0, 0, 0, 0, 0, 0];

    const hoyLabels = (rawData.hoy && rawData.hoy.labels && rawData.hoy.labels.length > 0)
        ? rawData.hoy.labels : ['08:00', '10:00', '12:00', '14:00', '16:00', '18:00', '20:00', '22:00'];
    const hoyData = (rawData.hoy && rawData.hoy.data && rawData.hoy.data.length > 0)
        ? rawData.hoy.data : [0, 0, 0, 0, 0, 0, 0, 0];

    const mesLabels = (rawData.mes && rawData.mes.labels && rawData.mes.labels.length > 0)
        ? rawData.mes.labels : ['Sem 1', 'Sem 2', 'Sem 3', 'Sem 4'];
    const mesData = (rawData.mes && rawData.mes.data && rawData.mes.data.length > 0)
        ? rawData.mes.data : [0, 0, 0, 0];

    // 1. Gráfico Semanal (Línea)
    const ctxWeekly = document.getElementById('weeklySalesChart');
    if (ctxWeekly && typeof Chart !== 'undefined') {
        new Chart(ctxWeekly, {
            type: 'line',
            data: {
                labels: semanaLabels,
                datasets: [{
                    label: 'Ventas Semanales (S/)',
                    data: semanaData,
                    borderColor: '#0dcaf0',
                    backgroundColor: 'rgba(13, 202, 240, 0.15)',
                    fill: true,
                    tension: 0.35,
                    borderWidth: 2,
                    pointRadius: 4,
                    pointBackgroundColor: '#0dcaf0'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: { callback: v => 'S/ ' + v }
                    }
                }
            }
        });
    }

    // 2. Gráfico Hoy (Línea con Puntos)
    const ctxDaily = document.getElementById('dailySalesChart');
    if (ctxDaily && typeof Chart !== 'undefined') {
        new Chart(ctxDaily, {
            type: 'line',
            data: {
                labels: hoyLabels,
                datasets: [{
                    label: 'Ventas de Hoy (S/)',
                    data: hoyData,
                    borderColor: '#198754',
                    backgroundColor: 'rgba(25, 135, 84, 0.15)',
                    fill: true,
                    tension: 0.2,
                    borderWidth: 2,
                    pointRadius: 5,
                    pointHoverRadius: 7,
                    pointBackgroundColor: '#198754'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: { callback: v => 'S/ ' + v }
                    }
                }
            }
        });
    }

    // 3. Gráfico Mes (Barras)
    const ctxMonthly = document.getElementById('monthlySalesChart');
    if (ctxMonthly && typeof Chart !== 'undefined') {
        new Chart(ctxMonthly, {
            type: 'bar',
            data: {
                labels: mesLabels,
                datasets: [{
                    label: 'Ventas del Mes (S/)',
                    data: mesData,
                    backgroundColor: 'rgba(13, 110, 253, 0.65)',
                    borderColor: '#0d6efd',
                    borderWidth: 1,
                    borderRadius: 4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: { callback: v => 'S/ ' + v }
                    }
                }
            }
        });
    }
}

// Control de ejecución: si el DOM ya cargó, ejecuta de inmediato
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initDashboardCharts);
} else {
    initDashboardCharts();
}