export const Charts = {
    renderRegistrationChart(canvasId) {
        const ctx = document.getElementById(canvasId).getContext('2d');

        // Gradient for Line Chart
        const gradient = ctx.createLinearGradient(0, 0, 0, 400);
        gradient.addColorStop(0, 'rgba(79, 70, 229, 0.4)'); // Indigo
        gradient.addColorStop(1, 'rgba(79, 70, 229, 0)');

        return new Chart(ctx, {
            type: 'line',
            data: {
                labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
                datasets: [{
                    label: 'Registrations',
                    data: [12, 19, 3, 5, 2, 30],
                    borderColor: '#4f46e5', // Indigo-600
                    backgroundColor: gradient,
                    borderWidth: 2,
                    pointBackgroundColor: '#ffffff',
                    pointBorderColor: '#4f46e5',
                    pointRadius: 4,
                    pointHoverRadius: 6,
                    fill: true,
                    tension: 0.4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        backgroundColor: 'rgba(17, 24, 39, 0.9)',
                        titleColor: '#fff',
                        bodyColor: '#fff',
                        padding: 10,
                        cornerRadius: 8,
                        displayColors: false
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        grid: {
                            color: 'rgba(156, 163, 175, 0.1)', // Gray-400 with opacity
                            drawBorder: false
                        },
                        ticks: { color: '#9ca3af' } // Gray-400
                    },
                    x: {
                        grid: { display: false },
                        ticks: { color: '#9ca3af' }
                    }
                }
            }
        });
    },

    renderStatusChart(canvasId) {
        const ctx = document.getElementById(canvasId).getContext('2d');

        return new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: ['Approved', 'Pending', 'Rejected'],
                datasets: [{
                    data: [12, 5, 2], // Mock Data
                    backgroundColor: [
                        '#10b981', // Emerald-500
                        '#f59e0b', // Amber-500
                        '#ef4444'  // Red-500
                    ],
                    borderWidth: 0,
                    hoverOffset: 4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '75%',
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            usePointStyle: true,
                            color: '#9ca3af',
                            padding: 20,
                            font: { size: 12 }
                        }
                    }
                }
            }
        });
    }
};
