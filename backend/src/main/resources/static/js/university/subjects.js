// subjects.js — Manage Subjects Module
// Handles: loadSubjects(), createSubject(), renderSubjectsTable()

(function () {
    'use strict';

    // ── DOM refs ─────────────────────────────────────────────────────────────
    const tbody        = () => document.getElementById('subjects-table-body');
    const formCard     = () => document.getElementById('subject-form-card');
    const form         = () => document.getElementById('subject-form');
    const formMsg      = () => document.getElementById('subject-form-msg');
    const deptSelect   = () => document.getElementById('subject-dept');
    const filterDept   = () => document.getElementById('subject-filter-dept');

    // ── Helpers ───────────────────────────────────────────────────────────────
    function showMsg(msg, isError = false) {
        const el = formMsg();
        if (!el) return;
        el.textContent = msg;
        el.className = `mt-3 p-3 rounded-xl text-sm ${isError ? 'bg-red-50 text-red-700' : 'bg-green-50 text-green-700'}`;
        el.classList.remove('hidden');
        setTimeout(() => el.classList.add('hidden'), 4000);
    }

    async function loadDepartmentsInto(selectEl) {
        if (!selectEl) return;
        const collegeId = window.CollegeContext?.selectedCollegeId;
        // If no college selected, load all departments across the university
        const url = collegeId
            ? `${ADMIN_API_BASE_URL}/departments?collegeId=${collegeId}`
            : `${ADMIN_API_BASE_URL}/departments`;
        try {
            const res = await authFetch(url);
            const depts = res.ok ? await res.json() : [];
            let html = '<option value="">All Departments</option>';
            // Deduplicate by name when loading all depts across colleges
            const seen = new Set();
            depts.forEach(d => {
                if (!seen.has(d.name)) {
                    seen.add(d.name);
                    html += `<option value="${d.id}" data-name="${d.name}">${d.name}</option>`;
                }
            });
            selectEl.innerHTML = html;
        } catch (e) {
            selectEl.innerHTML = '<option value="">Failed to load departments</option>';
        }
    }

    // ── Load subjects table ───────────────────────────────────────────────────
    window.loadSubjects = async function () {
        const tb = tbody();
        if (!tb) return;
        tb.innerHTML = '<tr><td colspan="6" class="px-6 py-8 text-center text-gray-400"><i class="fas fa-spinner fa-spin mr-2"></i>Loading subjects...</td></tr>';

        const course = document.getElementById('subject-filter-course')?.value || '';
        const deptSelect = document.getElementById('subject-filter-dept');
        const deptName = deptSelect ? (deptSelect.options[deptSelect.selectedIndex]?.dataset?.name || '') : '';
        const sem    = document.getElementById('subject-filter-sem')?.value || '';

        // Subjects are university-wide — always filter by name, never by college-specific departmentId
        let url = `${ADMIN_API_BASE_URL}/subjects`;
        const params = [];
        if (course) params.push(`course=${encodeURIComponent(course)}`);
        if (deptName) params.push(`departmentName=${encodeURIComponent(deptName)}`);
        if (sem)    params.push(`semester=${sem}`);
        if (params.length) url += '?' + params.join('&');

        try {
            const res = await authFetch(url);
            if (!res.ok) throw new Error('Failed to load subjects');
            const subjects = await res.json();
            renderSubjectsTable(subjects);
        } catch (e) {
            tb.innerHTML = '<tr><td colspan="6" class="px-6 py-8 text-center text-red-500">Failed to load subjects. Please try again.</td></tr>';
        }
    };

    // ── Render subjects table ─────────────────────────────────────────────────
    window.renderSubjectsTable = function (subjects) {
        const tb = tbody();
        if (!tb) return;

        if (!subjects || subjects.length === 0) {
            tb.innerHTML = `<tr><td colspan="6" class="px-6 py-8 text-center text-gray-400">
                <i class="fas fa-book-open text-3xl mb-2 opacity-40"></i>
                <p>No subjects found. Try different filters or add a subject manually.</p>
            </td></tr>`;
            return;
        }

        tb.innerHTML = subjects.map(s => `
            <tr class="border-b dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
                <td class="px-6 py-3 font-mono text-xs text-gray-500">#${s.id}</td>
                <td class="px-6 py-3">
                    <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300">
                        ${s.code || 'N/A'}
                    </span>
                </td>
                <td class="px-6 py-3 font-medium text-gray-900 dark:text-white">${s.name || 'N/A'}</td>
                <td class="px-6 py-3 text-gray-600 dark:text-gray-300">${s.course || 'N/A'}</td>
                <td class="px-6 py-3 text-gray-600 dark:text-gray-300">${s.departmentEntity?.name || 'N/A'}</td>
                <td class="px-6 py-3">
                    <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-indigo-50 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-300">
                        Sem ${s.semester}
                    </span>
                </td>
            </tr>
        `).join('');
    };

    // ── Create subject ────────────────────────────────────────────────────────
    window.createSubject = async function (payload) {
        try {
            const res = await authFetch(`${ADMIN_API_BASE_URL}/subjects`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const text = await res.text();
            if (!res.ok) throw new Error(text || 'Failed to create subject');
            showMsg('Subject created successfully!');
            form()?.reset();
            loadSubjects();
        } catch (e) {
            showMsg(e.message || 'Error creating subject.', true);
        }
    };

    // ── Event wiring ──────────────────────────────────────────────────────────
    function init() {
        // Add Subject button
        const btnAdd = document.getElementById('btn-add-subject');
        if (btnAdd) {
            btnAdd.addEventListener('click', () => {
                const card = formCard();
                if (card) {
                    card.classList.toggle('hidden');
                    if (!card.classList.contains('hidden')) {
                        loadDepartmentsInto(deptSelect());
                    }
                }
            });
        }

        // Cancel button
        const btnCancel = document.getElementById('btn-cancel-subject');
        if (btnCancel) {
            btnCancel.addEventListener('click', () => {
                formCard()?.classList.add('hidden');
                form()?.reset();
            });
        }

        // Form submit
        const subjectForm = form();
        if (subjectForm) {
            subjectForm.addEventListener('submit', (e) => {
                e.preventDefault();
                const course   = document.getElementById('subject-course')?.value?.trim();
                const deptId   = document.getElementById('subject-dept')?.value;
                const semester = parseInt(document.getElementById('subject-semester')?.value);
                const name     = document.getElementById('subject-name')?.value?.trim();
                const code     = document.getElementById('subject-code')?.value?.trim().toUpperCase();

                if (!course || !deptId || !semester || !name || !code) {
                    showMsg('All fields are required.', true);
                    return;
                }

                createSubject({
                    name,
                    code,
                    semester,
                    course,
                    departmentEntity: { id: parseInt(deptId) }
                });
            });
        }

        // Search button
        const btnLoad = document.getElementById('btn-load-subjects');
        if (btnLoad) {
            btnLoad.addEventListener('click', loadSubjects);
        }

        // CSV Upload
        const btnCsv = document.getElementById('btn-subject-csv');
        if (btnCsv) {
            btnCsv.addEventListener('click', () => {
                const input = document.createElement('input');
                input.type = 'file';
                input.accept = '.csv';
                input.onchange = async (e) => {
                    const file = e.target.files[0];
                    if (!file) return;
                    const fd = new FormData();
                    fd.append('file', file);
                    try {
                        const token = localStorage.getItem('token');
                        const res = await fetch(`${ADMIN_API_BASE_URL}/subjects/upload`, { 
                            method: 'POST', 
                            headers: token ? { 'Authorization': `Bearer ${token}` } : {},
                            body: fd 
                        });
                        const msg = await res.text();
                        alert(res.ok ? msg : 'Upload failed: ' + msg);
                        if (res.ok) loadSubjects();
                    } catch (err) {
                        alert('Upload error: ' + err.message);
                    }
                };
                input.click();
            });
        }

        // Reload department dropdowns when college context changes
        document.addEventListener('collegeContextChanged', () => {
            // Reload form dept dropdown if form is open
            const card = formCard();
            if (card && !card.classList.contains('hidden')) {
                loadDepartmentsInto(deptSelect());
            }

            // Reload filter dept dropdown
            loadDepartmentsInto(filterDept());

            // Reload staff dept filter too
            const staffDeptFilter = document.getElementById('staff-dept-filter');
            if (staffDeptFilter) {
                loadStaffDepartmentsInto(staffDeptFilter);
            }

            // Reload student dept filter across dashboard
            const studentDeptFilter = document.getElementById('filter-department');
            if (studentDeptFilter) {
                loadStaffDepartmentsInto(studentDeptFilter);
            }

            // Reload exam creation form department dropdown
            const examDeptSelect = document.getElementById('exam-department');
            if (examDeptSelect) {
                loadStaffDepartmentsInto(examDeptSelect);
            }
        });

        // Always load departments for filter (university-wide) on startup
        loadDepartmentsInto(filterDept());
        
        // Immediate load if context already set before init
        if (window.CollegeContext?.selectedCollegeId) {
            loadDepartmentsInto(filterDept());
            const staffDeptFilter = document.getElementById('staff-dept-filter');
            if (staffDeptFilter) loadStaffDepartmentsInto(staffDeptFilter);
            const studentDeptFilter = document.getElementById('filter-department');
            if (studentDeptFilter) loadStaffDepartmentsInto(studentDeptFilter);
            const examDeptSelect = document.getElementById('exam-department');
            if (examDeptSelect) loadStaffDepartmentsInto(examDeptSelect);
        }
    }

    // ── Staff dept filter helper ──────────────────────────────────────────────
    async function loadStaffDepartmentsInto(selectEl) {
        if (!selectEl) return;
        const collegeId = window.CollegeContext?.selectedCollegeId;
        let html = '<option value="">All Departments</option>';
        if (collegeId) {
            try {
                const res = await authFetch(`${ADMIN_API_BASE_URL}/departments?collegeId=${collegeId}`);
                const depts = res.ok ? await res.json() : [];
                depts.forEach(d => {
                    html += `<option value="${d.name}">${d.name}</option>`;
                });
            } catch (e) { /* silent */ }
        }
        selectEl.innerHTML = html;
    }

    // Run after DOM ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
