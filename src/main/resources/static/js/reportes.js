document.addEventListener("DOMContentLoaded", function() {
    const ctx = document.getElementById('ventasChart');

    if (ctx) {
        new Chart(ctx.getContext('2d'), {
            type: 'bar',
            data: {
                labels: ['30/09', '01/10'],
                datasets: [{
                    label: 'Ventas por día',
                    data: [680, 545],
                    backgroundColor: [
                        'rgba(105, 240, 174, 0.8)',
                        'rgba(130, 177, 255, 0.8)'
                    ],
                    borderWidth: 0,
                    borderRadius: 4,
                    barPercentage: 0.4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: false
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        max: 800,
                        ticks: {
                            stepSize: 200,
                            color: '#6c757d'
                        },
                        grid: {
                            color: '#e9ecef'
                        }
                    },
                    x: {
                        grid: {
                            display: false
                        },
                        ticks: {
                            color: '#6c757d'
                        }
                    }
                }
            }
        });
    }
});