async function loadRegistrations() {
    const tbody = document.getElementById('registrations-table-body');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4">Loading...</td></tr>';

    try {
        const response = await fetch(`${API_BASE_URL}/${UNIVERSITY_ID}/registrations`);
        const data = await response.json();
        DashboardState.registrations = data;
        renderRegistrationsTable(data);
    } catch (e) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-red-500">Error loading registrations</td></tr>';
    }
}

function renderRegistrationsTable(regs) {
    const tbody = document.getElementById('registrations-table-body');
    if (regs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-gray-500">No registrations found</td></tr>';
        return;
    }

    tbody.innerHTML = regs.map(reg => `
        <tr class="hover:bg-gray-50 border-b border-gray-100">
            <td class="px-6 py-4 font-medium text-gray-900">${reg.fullName}</td>
            <td class="px-6 py-4 text-gray-500">${reg.prn}</td>
            <td class="px-6 py-4">
                <div class="text-sm font-medium text-gray-900">${getExamName(reg.examId)}</div>
                <div class="text-xs text-gray-500">${reg.course}</div>
            </td>
            <td class="px-6 py-4">${reg.institutionName || 'N/A'}</td>
            <td class="px-6 py-4">
                <span class="px-2 py-1 text-xs rounded-full ${getRegStatusColor(reg.registrationStatus)}">
                    ${reg.registrationStatus}
                </span>
            </td>
            <td class="px-6 py-4 text-right">
                ${renderRegActions(reg)}
            </td>
        </tr>
    `).join('');
}

function getExamName(examId) {
    // Basic lookup from cached exams
    const exam = DashboardState.exams.find(e => e.id === examId);
    return exam ? exam.examName : `Exam #${examId}`;
}

function getRegStatusColor(status) {
    switch (status) {
        case 'APPROVED': return 'bg-green-100 text-green-800';
        case 'REJECTED': return 'bg-red-100 text-red-800';
        default: return 'bg-yellow-100 text-yellow-800';
    }
}

function renderRegActions(reg) {
    if (reg.registrationStatus !== 'PENDING') return '';
    return `
        <button onclick="approveRegistration(${reg.id})" class="text-green-600 hover:text-green-900 mr-3" title="Approve">
            <i class="fas fa-check"></i>
        </button>
        <button onclick="rejectRegistration(${reg.id})" class="text-red-600 hover:text-red-900" title="Reject">
            <i class="fas fa-times"></i>
        </button>
    `;
}

async function approveRegistration(id) {
    if (!confirm('Approve this registration?')) return;
    await updateRegStatus(id, 'approve');
}

async function rejectRegistration(id) {
    if (!confirm('Reject this registration?')) return;
    await updateRegStatus(id, 'reject');
}

async function updateRegStatus(id, action) {
    try {
        const response = await fetch(`${API_BASE_URL}/registrations/${id}/${action}`, {
            method: 'POST'
        });

        if (response.ok) {
            loadRegistrations();
            loadStats();
        } else {
            alert('Action failed');
        }
    } catch (e) {
        console.error(e);
        alert('Network error');
    }
}
