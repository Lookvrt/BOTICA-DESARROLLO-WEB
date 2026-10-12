document.addEventListener('DOMContentLoaded', function () {
    const canvas = document.getElementById('salesChart');

    if (canvas) {
        const ctx = canvas.getContext('2d');

        new Chart(ctx, {
            type: 'bar',
            data: {
                labels: ['Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab', 'Dom'],
                datasets: [{
                    data: [820, 950, 1100, 1340, 1180, 1620, 1245],
                    backgroundColor: [
                        '#6ee7b7', '#6ee7b7', '#6ee7b7', '#6ee7b7', '#6ee7b7', '#60a5fa', '#6ee7b7'
                    ],
                    borderRadius: 4
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
                        max: 2000
                    }
                }
            }
        });
    }
});