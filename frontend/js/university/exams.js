async function loadExams() {
    const tbody = document.getElementById('exams-table-body');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="5" class="text-center py-4">Loading...</td></tr>';

    try {
        const response = await authFetch(`${API_BASE_URL}/${UNIVERSITY_ID}/exam`);
        if (!response.ok) throw new Error('Failed to load exams: ' + response.status);
        const exams = await response.json();
        DashboardState.exams = exams; // Store exams in state
        renderExamsTable(exams);
        updateExamStats(); // Update stats after loading exams

        // Add filter listeners
        document.querySelectorAll('.exam-filter-btn').forEach(btn => {
            btn.addEventListener('click', function () {
                const statusStr = this.innerText.toUpperCase();
                let filtered = exams;
                if (statusStr !== 'ALL STATUSES') {
                    filtered = exams.filter(e => (e.status || '').toUpperCase() === statusStr);
                }
                renderExamsTable(filtered);
            });
        });
    } catch (e) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-red-500">Error loading exams</td></tr>';
        console.error(e);
    }
}

function renderExamsTable(exams) {
    const tbody = document.getElementById('exams-table-body');
    if (!exams || exams.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-gray-500">No exams found.</td></tr>';
        return;
    }

    tbody.innerHTML = exams.map(exam => {
        const isDraft = exam.status === 'DRAFT';
        const draftWarning = isDraft ? `<div class="text-[11px] text-red-500 font-bold mt-1"><i class="fas fa-exclamation-circle mr-1"></i> Not published yet, complete exam creation!</div>` : '';
        const examNameDisplay = exam.examName || exam.sessionName || 'Unnamed Exam';

        let timeDisplay = exam.startTime || '--:--';
        if (exam.startTime && exam.durationMinutes) {
            try {
                const [h, m] = exam.startTime.split(':').map(Number);
                const start = new Date();
                start.setHours(h, m, 0);
                const end = new Date(start.getTime() + exam.durationMinutes * 60000);
                const endStr = end.toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit' });
                timeDisplay = `${exam.startTime} - ${endStr}`;
            } catch (e) { }
        }

        const currentStatus = (exam.status || 'UNKNOWN').toUpperCase();
        const sourceLabel = exam.source === 'LEGACY' ? 'Legacy' : 'University';
        const sourceColor = exam.source === 'LEGACY' ? 'bg-amber-100 text-amber-700' : 'bg-indigo-100 text-indigo-700';

        return `
        <tr class="hover:bg-gray-50 border-b border-gray-100">
            <td class="px-6 py-4 font-medium text-gray-900 border-l-4 ${isDraft ? 'border-red-500' : 'border-transparent'}">
                <div class="flex items-center gap-2">
                    ${examNameDisplay}
                    <span class="text-[10px] px-1.5 py-0.5 rounded-md font-bold uppercase transition-all ${sourceColor}">${sourceLabel}</span>
                </div>
                ${draftWarning}
            </td>
            <td class="px-6 py-4">${formatDate(exam.examDate || exam.date)}</td>
            <td class="px-6 py-4">${timeDisplay}</td>
            <td class="px-6 py-4 capitalize">${(exam.examType || exam.mode || 'Regular').toLowerCase()}</td>
            <td class="px-6 py-4 capitalize">${exam.location || 'N/A'}</td>
            <td class="px-6 py-4">
                <select onchange="updateExamStatus('${exam.sourceId}', this.value)" class="text-xs font-semibold rounded-lg px-2 py-1 outline-none cursor-pointer border ${getStatusColor(currentStatus)}">
                    <option value="UPCOMING" ${currentStatus === 'UPCOMING' ? 'selected' : ''}>UPCOMING</option>
                    <option value="DRAFT" ${currentStatus === 'DRAFT' ? 'selected' : ''}>DRAFT</option>
                    <option value="OPEN" ${currentStatus === 'OPEN' ? 'selected' : ''}>OPEN</option>
                    <option value="LIVE" ${currentStatus === 'LIVE' ? 'selected' : ''}>LIVE</option>
                    <option value="COMPLETED" ${currentStatus === 'COMPLETED' ? 'selected' : ''}>COMPLETED</option>
                    <option value="CANCELLED" ${currentStatus === 'CANCELLED' ? 'selected' : ''}>CANCELLED</option>
                </select>
            </td>
            <td class="px-6 py-4 text-right">
                <button onclick="editExam('${exam.sourceId}')" class="text-blue-600 hover:text-blue-900 mr-3 transition-colors" title="Edit Exam"><i class="fas fa-edit"></i> Edit</button>
                <button onclick="confirmDeleteExam('${exam.sourceId}')" class="text-red-600 hover:text-red-900 transition-colors" title="Delete Exam"><i class="fas fa-trash"></i></button>
            </td>
        </tr>
    `}).join('');
}

window.updateExamStatus = async function (id, status) {
    try {
        const response = await authFetch('http://localhost:8080/api/exam/' + id + '/status', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: status })
        });
        if (response.ok) {
            console.log("Exam status updated");
        } else {
            alert('Failed to update status.');
        }
    } catch (e) {
        console.error("Status update error", e);
    }
}

function getStatusColor(status) {
    switch (status?.toUpperCase()) {
        case 'OPEN': return 'bg-green-100 text-green-800';
        case 'DRAFT': return 'bg-yellow-100 text-yellow-800';
        case 'UPCOMING': return 'bg-blue-100 text-blue-800';
        case 'COMPLETED': return 'bg-gray-100 text-gray-800';
        case 'CANCELLED': return 'bg-red-100 text-red-800';
        default: return 'bg-gray-100 text-gray-800';
    }
}

// Global functions for inline onclick handlers
window.editExam = function (id) {
    if (window.loadExamIntoWizard) {
        window.loadExamIntoWizard(id);
    } else {
        alert("Edit functionality is connected to the Create Wizard.");
    }
}

// Add Delete Modal to DOM dynamically
document.addEventListener('DOMContentLoaded', () => {
    document.body.insertAdjacentHTML('beforeend', `
    <div id="delete-exam-modal" class="hidden fixed inset-0 bg-black bg-opacity-50 z-[100] flex items-center justify-center transition-opacity">
        <div class="bg-white rounded-xl shadow-2xl p-6 max-w-md w-full mx-4 transform transition-all animate-fade-in-up">
            <h3 class="text-xl font-bold text-gray-900 mb-2 border-b pb-2"><i class="fas fa-exclamation-triangle text-red-500 mr-2"></i> Important Notice</h3>
            <p class="text-gray-600 mb-4">You are about to delete this exam. This action is irreversible. All associated data will be permanently removed.</p>
            
            <div class="mb-5 bg-red-50 p-3 rounded border border-red-100 relative">
                <label class="flex items-start space-x-3 cursor-pointer">
                    <input type="checkbox" id="delete-confirm-checkbox" class="form-checkbox h-5 w-5 text-red-600 mt-0.5 rounded focus:ring-red-500 border-gray-300">
                    <span class="text-sm font-medium text-red-800">I understand the consequences and I confirm that I want to delete this exam.</span>
                </label>
            </div>

            <div class="flex justify-end gap-3 mt-4">
                <button onclick="closeDeleteModal()" class="px-4 py-2 text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors font-medium">Cancel</button>
                <button id="confirm-delete-btn" class="px-4 py-2 text-white bg-red-600 hover:bg-red-700 rounded-lg transition-colors font-medium opacity-50 cursor-not-allowed" disabled>Delete Exam</button>
            </div>
        </div>
    </div>
    `);

    document.getElementById('delete-confirm-checkbox').addEventListener('change', function () {
        const deleteBtn = document.getElementById('confirm-delete-btn');
        if (this.checked) {
            deleteBtn.disabled = false;
            deleteBtn.classList.remove('opacity-50', 'cursor-not-allowed');
        } else {
            deleteBtn.disabled = true;
            deleteBtn.classList.add('opacity-50', 'cursor-not-allowed');
        }
    });

    document.getElementById('confirm-delete-btn').addEventListener('click', async () => {
        if (!window.examToDelete) return;

        const btn = document.getElementById('confirm-delete-btn');
        const originalText = btn.innerHTML;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i>Deleting...';
        btn.disabled = true;

        try {
            const response = await authFetch(`${API_BASE_URL}/${UNIVERSITY_ID}/exam/${window.examToDelete}`, {
                method: 'DELETE'
            });

            if (response.ok) {
                closeDeleteModal();
                loadExams();
                if (typeof updateExamStats === 'function') updateExamStats();

                // Show a quick standard alert or toast
                alert("Exam successfully deleted.");
            } else {
                alert("Failed to delete the exam.");
                btn.innerHTML = originalText;
                btn.disabled = false;
            }
        } catch (error) {
            console.error("Error deleting exam:", error);
            alert("Error connecting to server.");
            btn.innerHTML = originalText;
            btn.disabled = false;
        }
    });
});

window.examToDelete = null;

window.confirmDeleteExam = function (id) {
    window.examToDelete = id;
    const modal = document.getElementById('delete-exam-modal');
    const checkbox = document.getElementById('delete-confirm-checkbox');
    const deleteBtn = document.getElementById('confirm-delete-btn');

    // Reset state
    if (checkbox) checkbox.checked = false;
    if (deleteBtn) {
        deleteBtn.disabled = true;
        deleteBtn.classList.add('opacity-50', 'cursor-not-allowed');
        deleteBtn.innerHTML = 'Delete Exam';
    }

    if (modal) modal.classList.remove('hidden');
}

window.closeDeleteModal = function () {
    window.examToDelete = null;
    const modal = document.getElementById('delete-exam-modal');
    if (modal) modal.classList.add('hidden');
}
