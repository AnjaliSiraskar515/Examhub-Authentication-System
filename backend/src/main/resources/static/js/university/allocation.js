const allocationState = {
    selectedExamId: null,
    colleges: [],
    headSupervisors: [],
    currentPage: 0,
    currentCollegeId: null
};

document.addEventListener('DOMContentLoaded', () => {
    const targetLink = document.querySelector('.nav-link[data-target="allocation-section"]');
    if (targetLink) {
        targetLink.addEventListener('click', () => {
            loadAllocationExams();
        });
    }

    const selectExam = document.getElementById('allocation-exam-select');
    if (selectExam) {
        selectExam.addEventListener('change', (e) => {
            allocationState.selectedExamId = e.target.value;
            if (allocationState.selectedExamId) {
                document.getElementById('btn-map-college').classList.remove('hidden');
                document.getElementById('btn-auto-map').classList.remove('hidden');
                document.getElementById('allocation-mappings-container').classList.remove('hidden');
                loadMappingsForExam(allocationState.selectedExamId);
            } else {
                document.getElementById('btn-map-college').classList.add('hidden');
                document.getElementById('btn-auto-map').classList.add('hidden');
                document.getElementById('allocation-mappings-container').classList.add('hidden');
            }
        });
    }

    const mapForm = document.getElementById('map-college-form');
    if (mapForm) {
        mapForm.addEventListener('submit', handleMapCollegeSubmit);
    }

    const mapCollegeSelect = document.getElementById('map-college-select');
    if (mapCollegeSelect) {
        mapCollegeSelect.addEventListener('change', (e) => {
            loadHeadSupervisors(e.target.value);
        });
    }
});

async function loadAllocationExams() {
    try {
        const resp = await authFetch('/api/university/exams');
        if (!resp.ok) throw new Error('Failed to fetch exams: ' + resp.status);
        const exams = await resp.json();
        const select = document.getElementById('allocation-exam-select');
        select.innerHTML = '<option value="">— Select Exam —</option>';
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        (exams || []).filter(ex => {
            if (ex.status === 'COMPLETED') return false;
            if (ex.examDate) {
                let dateStr = Array.isArray(ex.examDate) ? ex.examDate.join('-') : ex.examDate;
                const parts = dateStr.split('-');
                if (parts.length >= 3) {
                    const eDate = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
                    if (eDate < today) return false;
                }
            }
            return true;
        }).forEach(ex => {
            let dateStr = '';
            if (ex.examDate) {
                dateStr = Array.isArray(ex.examDate) ? ex.examDate.join('-') : ex.examDate;
            }
            select.innerHTML += `<option value="${ex.id}">${ex.sessionName} (${ex.course || ''} - Sem ${ex.semester || ''}) [${dateStr}]</option>`;
        });
    } catch (e) {
        console.error('Failed to load allocation exams:', e);
    }
}

async function loadMappingsForExam(examId) {
    try {
        const tbody = document.getElementById('allocation-mappings-body');
        tbody.innerHTML = '<tr><td colspan="5" class="px-6 py-4 text-center">Loading...</td></tr>';
        
        const resp = await authFetch(`/api/university/allocation/mappings/exam/${examId}`);
        if (!resp.ok) throw new Error('Failed to fetch mappings: ' + resp.status);
        const mappings = await resp.json();
        
        if (!mappings || mappings.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="px-6 py-4 text-center text-gray-500">No colleges mapped yet.</td></tr>';
            return;
        }

        tbody.innerHTML = mappings.map(m => {
            let statusBadge = '';
            let emoji = '';
            if (m.status === 'PENDING') {
                statusBadge = 'bg-yellow-100 text-yellow-800';
                emoji = '🟡';
            } else if (m.status === 'HALLS_CONFIGURED') {
                statusBadge = 'bg-blue-100 text-blue-800';
                emoji = '🔵';
            } else {
                statusBadge = 'bg-green-100 text-green-800';
                emoji = '🟢';
            }

            return `
            <tr class="border-b dark:border-gray-700">
                <td class="px-6 py-4 font-medium">${m.collegeName}</td>
                <td class="px-6 py-4">${m.headSupervisorName}</td>
                <td class="px-6 py-4">${m.totalCapacity || 'Not Configured'}</td>
                <td class="px-6 py-4">
                    <span class="px-2 py-1.5 rounded-full text-xs font-bold shadow-sm inline-flex items-center gap-1.5 ${statusBadge}">
                        ${emoji} ${m.status.replace('_', ' ')}
                    </span>
                </td>
                <td class="px-6 py-4 text-right">
                    ${m.status === 'HALLS_CONFIGURED' ? 
                        `<button onclick="generateSeats(${m.examId}, ${m.collegeId})" class="bg-blue-600 hover:bg-blue-700 text-white px-3 py-1.5 rounded shadow text-sm transition-all"><i class="fas fa-cogs mr-1"></i> Generate Seats</button>` 
                        : m.status === 'SEATS_GENERATED' ? 
                        `<button onclick="viewSeatingChart(${m.examId}, ${m.collegeId})" class="bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 px-3 py-1.5 rounded shadow text-sm transition-all"><i class="fas fa-list-ol mr-1"></i> View Chart</button>`
                        : '<span class="text-xs text-gray-400 italic">Waiting for Halls...</span>'
                    }
                </td>
            </tr>
            `;
        }).join('');
    } catch (e) {
        console.error('Failed to load mappings:', e);
        document.getElementById('allocation-mappings-body').innerHTML = '<tr><td colspan="5" class="px-6 py-4 text-center text-red-500">Error loading data</td></tr>';
    }
}

async function openMapCollegeModal() {
    if (!allocationState.selectedExamId) return;
    
    try {
        const resp = await authFetch('/api/admin/colleges');
        if (!resp.ok) throw new Error('Failed to fetch colleges');
        const colleges = await resp.json();
        const select = document.getElementById('map-college-select');
        select.innerHTML = '<option value="">— Select College —</option>';
        (colleges || []).forEach(c => {
            select.innerHTML += `<option value="${c.id}">${c.name}</option>`;
        });
    } catch (e) {
        console.error('Failed to load colleges', e);
    }
    
    document.getElementById('map-supervisor-select').innerHTML = '<option value="">— Select Head Supervisor —</option>';
    document.getElementById('map-supervisor-select').disabled = true;
    document.getElementById('map-college-modal').classList.remove('hidden');
}

function closeMapCollegeModal() {
    document.getElementById('map-college-modal').classList.add('hidden');
    document.getElementById('map-college-form').reset();
}

async function loadHeadSupervisors(collegeId) {
    const supSelect = document.getElementById('map-supervisor-select');
    if (!collegeId) {
        supSelect.innerHTML = '<option value="">— Select Head Supervisor —</option>';
        supSelect.disabled = true;
        return;
    }
    
    try {
        const resp = await authFetch(`/api/admin/supervisors?collegeId=${collegeId}`);
        if (!resp.ok) throw new Error('Failed to fetch supervisors');
        const data = await resp.json();
        const heads = (data || []).filter(u => u.supervisorType && u.supervisorType.trim().toUpperCase() === 'HEAD' && u.status && u.status.trim().toLowerCase() === 'active');
        
        supSelect.innerHTML = '<option value="">— Select Head Supervisor —</option>';
        heads.forEach(h => {
            supSelect.innerHTML += `<option value="${h.userId}">${h.name} (${h.email})</option>`;
        });
        supSelect.disabled = heads.length === 0;
        if (heads.length === 0) {
            supSelect.innerHTML = '<option value="">— No Active Head Supervisors Found —</option>';
        }
    } catch (e) {
        console.error('Failed to load heads', e);
    }
}

async function handleMapCollegeSubmit(e) {
    e.preventDefault();
    const collegeId = document.getElementById('map-college-select').value;
    const supervisorId = document.getElementById('map-supervisor-select').value;
    
    if (!collegeId || !supervisorId || !allocationState.selectedExamId) return;
    
    try {
        const resp = await authFetch('/api/university/allocation/mappings', {
            method: 'POST',
            body: JSON.stringify({
                examId: parseInt(allocationState.selectedExamId),
                collegeId: parseInt(collegeId),
                headSupervisorId: parseInt(supervisorId)
            })
        });
        if (!resp.ok) {
            const err = await resp.json().catch(() => ({}));
            throw new Error(err.error || err.message || 'Failed to map college');
        }
        closeMapCollegeModal();
        loadMappingsForExam(allocationState.selectedExamId);
    } catch (err) {
        alert(err.message || 'Failed to map college');
    }
}

async function generateSeats(examId, collegeId) {
    if (!confirm('Are you sure you want to generate seats? This will clear any existing non-attended seat data for this college.')) return;
    
    try {
        const resp = await authFetch(`/api/university/allocation/generate-seats?examId=${examId}&collegeId=${collegeId}`, {
            method: 'POST'
        });
        if (!resp.ok) {
            const err = await resp.json().catch(() => ({}));
            throw new Error(err.error || err.message || 'Failed to generate seats');
        }
        alert('Seats generated successfully!');
        loadMappingsForExam(examId);
    } catch (e) {
        alert(e.message || 'Failed to generate seats');
    }
}

async function viewSeatingChart(examId, collegeId, page = 0) {
    allocationState.currentCollegeId = collegeId;
    allocationState.currentPage = page;
    
    try {
        const resp = await authFetch(`/api/university/allocation/seats?examId=${examId}&collegeId=${collegeId}&page=${page}&size=50`);
        if (!resp.ok) throw new Error('Failed to fetch seating chart');
        const response = await resp.json();
        const seats = response.content || [];
        const totalPages = response.totalPages || 1;
        
        const container = document.getElementById('seating-chart-container');
        const tbody = document.getElementById('seating-chart-body');
        
        if (seats.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="px-6 py-4 text-center">No seats found.</td></tr>';
        } else {
            tbody.innerHTML = seats.map(s => `
                <tr class="border-b dark:border-gray-700">
                    <td class="px-6 py-3 font-mono font-semibold">${s.rollNumber}</td>
                    <td class="px-6 py-3">${s.prn}</td>
                    <td class="px-6 py-3">${s.studentName}</td>
                    <td class="px-6 py-3">${s.hallName}</td>
                    <td class="px-6 py-3 font-semibold text-blue-600 dark:text-blue-400">${s.seatNumber}</td>
                </tr>
            `).join('');
        }
        
        document.getElementById('seat-page-info').textContent = `Page ${page + 1} of ${totalPages}`;
        const prevBtn = document.getElementById('seat-prev-page');
        const nextBtn = document.getElementById('seat-next-page');
        
        prevBtn.disabled = page <= 0;
        nextBtn.disabled = page >= totalPages - 1;
        
        prevBtn.onclick = () => viewSeatingChart(examId, collegeId, page - 1);
        nextBtn.onclick = () => viewSeatingChart(examId, collegeId, page + 1);
        
        container.classList.remove('hidden');
        container.scrollIntoView({ behavior: 'smooth' });
    } catch (e) {
        console.error('Failed to load seating chart', e);
        alert(e.message || 'Failed to load seating chart');
    }
}

window.closeSeatingChart = function() {
    document.getElementById('seating-chart-container').classList.add('hidden');
}

window.openMapCollegeModal = openMapCollegeModal;
window.closeMapCollegeModal = closeMapCollegeModal;
window.generateSeats = generateSeats;
window.viewSeatingChart = viewSeatingChart;
window.autoMapColleges = autoMapColleges;

async function autoMapColleges() {
    if (!allocationState.selectedExamId) return;

    if (!confirm('This will automatically assign Head Supervisors to all colleges for this exam. Proceed?')) {
        return;
    }

    const btn = document.getElementById('btn-auto-map');
    const originalText = btn.innerHTML;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Mapping...';
    btn.disabled = true;

    try {
        const resp = await authFetch(`/api/university/allocation/auto-map/${allocationState.selectedExamId}`, {
            method: 'POST'
        });
        const result = await resp.json();

        if (!resp.ok) throw new Error(result.error || 'Auto-map failed');

        let msg = result.message + '\n\nDetails:\n';
        if (result.details && result.details.length > 0) {
            result.details.forEach(d => {
                msg += `- ${d.college}: ${d.status}\n`;
            });
        }
        
        alert(msg);
        
        loadMappingsForExam(allocationState.selectedExamId);
    } catch (e) {
        console.error('Auto-map failed:', e);
        alert('Failed to auto-map colleges: ' + e.message);
    } finally {
        btn.innerHTML = originalText;
        btn.disabled = false;
    }
}
