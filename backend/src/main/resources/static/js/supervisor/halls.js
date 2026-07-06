const hallsState = {
    selectedExamId: null,
    currentCollegeId: null,
    mappings: []
};

document.addEventListener('DOMContentLoaded', () => {
    const navHallsBtn = document.getElementById('navHalls');
    if (navHallsBtn) {
        navHallsBtn.addEventListener('click', () => {
            loadAssignedExams();
        });
    }

    const examSelect = document.getElementById('halls-exam-select');
    if (examSelect) {
        examSelect.addEventListener('change', (e) => {
            hallsState.selectedExamId = e.target.value;
            if (hallsState.selectedExamId) {
                // Find matching mapping for collegeId
                const mapping = hallsState.mappings.find(m => m.examId == hallsState.selectedExamId);
                if (mapping) {
                    hallsState.currentCollegeId = mapping.collegeId;
                    loadConfiguredHalls(hallsState.selectedExamId, mapping.collegeId);
                }
            } else {
                document.getElementById('halls-list-container').classList.add('hidden');
            }
        });
    }

    const form = document.getElementById('create-hall-form');
    if (form) {
        form.addEventListener('submit', handleCreateHall);
    }
});

async function loadAssignedExams() {
    try {
        const response = await fetch((window.GLOBAL_API_BASE || '') + '/api/supervisor/halls/mappings', {
            headers: authHeaders()
        });
        if (!response.ok) throw new Error('Failed to load assigned exams');
        const mappings = await response.json();

        // Fetch exams to determine dynamic live status
        let liveExamIds = new Set();
        try {
            const examsRes = await fetch((window.GLOBAL_API_BASE || '') + '/api/supervisor/exams', { headers: authHeaders() });
            const exams = await examsRes.json();
            
            exams.forEach(ex => {
                let st = (ex.status || '').toUpperCase();
                try {
                     const todayObj = new Date();
                     const examDateObj = new Date(ex.date);
                     const isToday = examDateObj.toDateString() === todayObj.toDateString();
                     let isLive = st === 'LIVE' || st === 'ONGOING';
                     let isCompleted = st === 'COMPLETED' || (!isLive && examDateObj < todayObj && !isToday);

                     if (isToday && ex.startTime && ex.startTime !== 'TBD') {
                        const parseTime = (tStr) => {
                           if (!tStr || tStr === 'TBD') return null;
                           let match = tStr.trim().match(/(\d{1,2}):(\d{2})/);
                           if (!match) return null;
                           let h = parseInt(match[1]);
                           let m = parseInt(match[2]);
                           if (tStr.toUpperCase().includes('PM') && h < 12) h += 12;
                           if (tStr.toUpperCase().includes('AM') && h === 12) h = 0;
                           return h * 60 + m;
                        };
                        const startMins = parseTime(ex.startTime);
                        const endMins = (ex.endTime && ex.endTime !== 'TBD') ? parseTime(ex.endTime) : (startMins + (ex.duration || 120));
                        const nowMins = todayObj.getHours() * 60 + todayObj.getMinutes();
                        if (startMins !== null && endMins !== null) {
                           if (!isLive && !isCompleted) {
                              if (nowMins >= startMins && nowMins <= endMins) {
                                 st = 'LIVE';
                              } else if (nowMins > endMins) {
                                 st = 'COMPLETED';
                              }
                           }
                        }
                     }
                } catch(err) {}
                
                if (st === 'LIVE' || st === 'ONGOING') {
                    liveExamIds.add(ex.id);
                }
            });
        } catch(err) {
            console.warn("Could not fetch exams for dynamic status filtering", err);
        }

        hallsState.mappings = mappings.filter(m => !liveExamIds.has(m.examId)) || [];

        const select = document.getElementById('halls-exam-select');
        select.innerHTML = '<option value="">— Select Exam —</option>';
        hallsState.mappings.forEach(m => {
            select.innerHTML += `<option value="${m.examId}">${m.examName}</option>`;
        });
    } catch (e) {
        console.error(e);
        showToast(e.message, 'error');
    }
}

async function loadConfiguredHalls(examId, collegeId) {
    try {
        const response = await fetch(`/api/supervisor/halls/?examId=${examId}&collegeId=${collegeId}`, {
            headers: authHeaders()
        });
        if (!response.ok) throw new Error('Failed to load configured halls');
        const halls = await response.json();

        const container = document.getElementById('halls-list-container');
        const tbody = document.getElementById('halls-list-body');
        
        container.classList.remove('hidden');

        if (!halls || halls.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="px-5 py-4 text-center text-gray-500">No halls configured yet.</td></tr>';
            return;
        }

        tbody.innerHTML = halls.map(h => `
            <tr>
                <td class="px-5 py-4 font-semibold text-gray-800">${h.hallName}</td>
                <td class="px-5 py-4 font-mono text-indigo-600">${h.hallPrefix}</td>
                <td class="px-5 py-4">${h.capacity} students</td>
                <td class="px-5 py-4">${h.examSupervisorName || 'Not Assigned'}</td>
                <td class="px-5 py-4 text-right">
                    <button onclick="deleteHall(${h.id})" class="text-red-500 hover:text-red-700 bg-red-50 hover:bg-red-100 rounded p-2 transition-colors" title="Delete Hall">
                        <i class="fas fa-trash-alt"></i>
                    </button>
                </td>
            </tr>
        `).join('');
    } catch (e) {
        console.error(e);
        showToast(e.message, 'error');
    }
}

async function openCreateHallModal() {
    if (!hallsState.selectedExamId || !hallsState.currentCollegeId) {
        showToast('Please select an exam first', 'error');
        return;
    }

    // Load eligible supervisors: same university, different department (cross-dept invigilation rule)
    try {
        const response = await fetch(`/api/supervisor/halls/exam-supervisors?examId=${hallsState.selectedExamId}`, {
            headers: authHeaders()
        });
        const supervisors = await response.json();
        
        // Server already filters: same-university, active, EXAM-type, excludes same-dept as the exam
        const eligible = Array.isArray(supervisors) ? supervisors : [];
        
        const supSelect = document.getElementById('hall-supervisor');
        supSelect.innerHTML = '<option value="">— Select Exam Supervisor (Optional) —</option>';
        eligible.forEach(s => {
            // Show college + department so Head Supervisor has full context
            const meta = [s.college, s.department].filter(Boolean).join(' · ');
            supSelect.innerHTML += `<option value="${s.userId}">${s.name} (${meta})</option>`;
        });

        document.getElementById('create-hall-form').reset();
        document.getElementById('create-hall-modal').classList.remove('hidden');
    } catch (e) {
        console.error('Failed to load supervisors', e);
        showToast('Error loading supervisors for assignment', 'error');
    }
}

function closeCreateHallModal() {
    document.getElementById('create-hall-modal').classList.add('hidden');
}

async function handleCreateHall(e) {
    e.preventDefault();
    if (!hallsState.selectedExamId || !hallsState.currentCollegeId) return;

    const hallName = document.getElementById('hall-name').value;
    const hallPrefix = document.getElementById('hall-prefix').value;
    const capacity = parseInt(document.getElementById('hall-capacity').value);
    const supervisorId = document.getElementById('hall-supervisor').value;

    try {
        const response = await fetch((window.GLOBAL_API_BASE || '') + '/api/supervisor/halls/', {
            method: 'POST',
            headers: authHeaders({'Content-Type': 'application/json'}),
            body: JSON.stringify({
                examId: parseInt(hallsState.selectedExamId),
                collegeId: hallsState.currentCollegeId,
                hallName: hallName,
                hallPrefix: hallPrefix,
                capacity: capacity,
                examSupervisorId: supervisorId ? parseInt(supervisorId) : null
            })
        });

        if (!response.ok) {
            const errData = await response.json();
            throw new Error(errData.error || 'Failed to create hall');
        }

        showToast('Hall created successfully', 'success');
        closeCreateHallModal();
        loadConfiguredHalls(hallsState.selectedExamId, hallsState.currentCollegeId);
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// Ensure authHeaders is globally available or mock it if needed
if (typeof window.authHeaders !== 'function') {
    window.authHeaders = function(extra = {}) {
        const token = localStorage.getItem('jwtToken');
        return {
            'Authorization': 'Bearer ' + token,
            ...extra
        };
    };
}

window.openCreateHallModal = openCreateHallModal;
window.closeCreateHallModal = closeCreateHallModal;

window.deleteHall = async function(hallId) {
    if (!confirm('Are you sure you want to delete this hall? All capacities and configurations for this hall will be removed.')) return;
    try {
        const response = await fetch(`/api/supervisor/halls/${hallId}`, {
            method: 'DELETE',
            headers: authHeaders()
        });
        if (!response.ok) {
            const err = await response.json();
            throw new Error(err.error || 'Failed to delete hall');
        }
        showToast('Hall deleted successfully', 'success');
        loadConfiguredHalls(hallsState.selectedExamId, hallsState.currentCollegeId);
    } catch (e) {
        console.error(e);
        showToast(e.message, 'error');
    }
};
