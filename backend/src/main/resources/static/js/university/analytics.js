function loadAnalytics() {
    renderCharts();
}

function renderCharts() {
    const ctx1 = document.getElementById('registrationsChart')?.getContext('2d');
    const ctx2 = document.getElementById('examDistributionChart')?.getContext('2d');

    if (ctx1) {
        new Chart(ctx1, {
            type: 'line',
            data: {
                labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
                datasets: [{
                    label: 'Registrations',
                    data: [12, 19, 3, 5, 2, 3], // Mock data for now
                    borderColor: 'rgb(75, 192, 192)',
                    tension: 0.1
                }]
            },
            options: { responsive: true }
        });
    }

    if (ctx2) {
        new Chart(ctx2, {
            type: 'doughnut',
            data: {
                labels: ['Approved', 'Pending', 'Rejected'],
                datasets: [{
                    label: 'Status',
                    data: [
                        DashboardState.stats.approvedRegistrations,
                        DashboardState.stats.pendingApprovals,
                        5 // rejected placeholder
                    ],
                    backgroundColor: [
                        'rgb(34, 197, 94)',
                        'rgb(234, 179, 8)', // Yellow
                        'rgb(239, 68, 68)'
                    ],
                    hoverOffset: 4
                }]
            },
            options: { responsive: true }
        });
    }
}
