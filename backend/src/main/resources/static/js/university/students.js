// students.js - Logic for Managing Students
// Uses global CollegeContext (collegeContext.js) for filtering.
// No college dropdown in the modal — context bar drives everything.

let studentsList = []; // Memory cache

// ─── Listen for College Context Changes ──────────────────────────────────────
document.addEventListener('collegeContextChanged', () => {
    // Only reload if the students section is currently visible
    const section = document.getElementById('students-section');
    if (section && !section.classList.contains('hidden')) {
        window.loadStudents();
    }
});

// 1. Initial Load Function
window.loadStudents = async function () {
    const tbody = document.getElementById('students-table-body');
    if (!tbody) return;

    // Guard: require college selection
    if (!window.CollegeContext || !window.CollegeContext.selectedCollegeId) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="px-6 py-12 text-center">
                    <div class="flex flex-col items-center gap-3 text-gray-400 dark:text-gray-500">
                        <i class="fas fa-building text-4xl opacity-40"></i>
                        <p class="font-semibold text-base">Please select a college to view students</p>
                        <p class="text-sm opacity-70">Use the college selector in the header above</p>
                    </div>
                </td>
            </tr>`;
        const totalEnrollment = document.getElementById('total-enrollment');
        if (totalEnrollment) totalEnrollment.innerText = '—';
        injectStudentModal();
        return;
    }

    try {
        tbody.innerHTML = `<tr><td colspan="7" class="px-6 py-4 text-center text-gray-500"><i class="fas fa-spinner fa-spin mr-2"></i>Loading students...</td></tr>`;

        // Build query: always pass collegeId from context
        let query = new URLSearchParams();
        query.append('collegeId', window.CollegeContext.selectedCollegeId);

        const fYr = document.getElementById('filter-year')?.value || '';
        const fDep = document.getElementById('filter-department')?.value || '';
        if (fYr && fYr !== 'All Years') query.append('year', fYr);
        if (fDep && fDep !== 'All Departments') query.append('department', fDep);

        const url = `${ADMIN_API_BASE_URL}/students?${query.toString()}`;
        const response = await authFetch(url);

        if (!response.ok) throw new Error('Failed to fetch students');

        studentsList = await response.json();

        const totalEnrollment = document.getElementById('total-enrollment');
        if (totalEnrollment) totalEnrollment.innerText = studentsList.length;

        // Calculate Fees Overdue (students where feesPaid is false)
        const feesOverdueEl = document.getElementById('fees-overdue');
        if (feesOverdueEl) {
            const overdueCount = studentsList.filter(s => s.feesPaid === false || !s.feesPaid).length;
            feesOverdueEl.innerText = overdueCount;
        }

        // Calculate New Applications / Pending (students who are not eligible or status is pending)
        const newAppsEl = document.getElementById('new-applications');
        if (newAppsEl) {
            const pendingCount = studentsList.filter(s => s.isEligible === false || s.status === 'pending').length;
            newAppsEl.innerText = pendingCount;
        }

        renderStudentsTable();
        injectStudentModal();

    } catch (e) {
        console.error('Error loading students:', e);
        if (tbody) {
            tbody.innerHTML = `<tr><td colspan="7" class="px-6 py-4 text-center text-red-500"><i class="fas fa-exclamation-triangle mr-2"></i>Failed to load students. Backend may be offline.</td></tr>`;
        }
    }
};

// Hook up event listeners for year/department filters
document.addEventListener('DOMContentLoaded', () => {
    ['filter-year', 'filter-department'].forEach(id => {
        document.getElementById(id)?.addEventListener('change', window.loadStudents);
    });
});

// 2. Render Table
function renderStudentsTable() {
    const tbody = document.getElementById('students-table-body');
    if (!tbody) return;

    if (studentsList.length === 0) {
        const collegeName = window.CollegeContext?.selectedCollegeName || 'selected college';
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="px-6 py-12 text-center">
                    <div class="flex flex-col items-center gap-3 text-gray-400 dark:text-gray-500">
                        <i class="fas fa-user-graduate text-4xl opacity-40"></i>
                        <p class="font-semibold text-base">No students found for <span class="text-blue-500">${collegeName}</span></p>
                        <p class="text-sm opacity-70">Try adjusting year/department filters or upload student records via CSV</p>
                    </div>
                </td>
            </tr>`;
        return;
    }

    const html = studentsList.map(student => {
        const isEligible = student.isEligible !== false;
        const statusBadge = isEligible
            ? `<span class="px-2 py-1 bg-green-100 text-green-700 text-xs font-bold rounded-md uppercase tracking-wider">Active</span>`
            : `<span class="px-2 py-1 bg-red-100 text-red-700 text-xs font-bold rounded-md uppercase tracking-wider">Blocked</span>`;

        const collegeName = (student.college && student.college.name)
            ? student.college.name
            : (student.collegeName || 'Unknown');

        let yearDisplay = 'N/A';
        if (student.semester) {
            // Use regex to strip non-digits globally, ensuring we can parse "Sem Semester 8" or "08" into 8 correctly
            const semStr = String(student.semester).replace(/\D/g, '');
            const semNum = parseInt(semStr, 10);
            if (!isNaN(semNum)) {
                if (semNum === 1 || semNum === 2) yearDisplay = 'First Year';
                else if (semNum === 3 || semNum === 4) yearDisplay = 'Second Year';
                else if (semNum === 5 || semNum === 6) yearDisplay = 'Third Year';
                else if (semNum === 7 || semNum === 8) yearDisplay = 'Fourth Year';
                else yearDisplay = `Year (${semNum})`;

                // Add tiny semester label underneath for clarity
                yearDisplay += ` <br><span class="text-[10px] text-gray-400 font-normal">Sem ${semNum}</span>`;
            } else {
                yearDisplay = student.semester; // fallback
            }
        }

        return `
            <tr class="border-b dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
                <td class="px-6 py-4 font-bold text-gray-900 dark:text-white">${student.prn || student.userId}</td>
                <td class="px-6 py-4 font-medium">${student.name || 'N/A'}</td>
                <td class="px-6 py-4 font-medium text-gray-500">${student.email || 'N/A'}</td>
                <td class="px-6 py-4">
                    <span class="bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-300 px-2 py-1 rounded text-xs font-medium">${collegeName}</span>
                </td>
                <td class="px-6 py-4 font-medium">${student.course || 'N/A'}</td>
                <td class="px-6 py-4">${student.departmentEntity?.name || student.department || 'N/A'}</td>
                <td class="px-6 py-4 font-medium">${student.year || yearDisplay}</td>
                <td class="px-6 py-4">${statusBadge}</td>
                <td class="px-6 py-4 text-right space-x-2">
                    <button onclick="viewStudentDetails(${student.userId})" title="View Details & Backlogs" class="px-3 py-1 bg-blue-50 dark:bg-blue-900/20 text-blue-600 hover:bg-blue-100 dark:hover:bg-blue-900/40 rounded text-sm font-medium transition-colors">
                        <i class="fas fa-eye"></i>
                    </button>
                    <button onclick="toggleStudentStatus(${student.userId}, ${isEligible ? 'false' : 'true'})"
                        title="${isEligible ? 'Block User' : 'Activate User'}"
                        class="px-3 py-1 bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 rounded text-sm font-medium transition-colors">
                        <i class="fas ${isEligible ? 'fa-ban text-orange-500' : 'fa-check-circle text-green-500'}"></i>
                    </button>
                    <button onclick="removeStudent(${student.userId})" title="Remove Student" class="px-3 py-1 bg-red-50 dark:bg-red-900/20 text-red-600 hover:bg-red-100 dark:hover:bg-red-900/40 rounded text-sm font-medium transition-colors">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    }).join('');

    tbody.innerHTML = html;
}

// 3. View Student Details (unchanged logic)
window.viewStudentDetails = async function (id) {
    const student = studentsList.find(s => s.userId === id);
    if (!student) return;

    if (!document.getElementById('student-details-modal')) {
        document.body.insertAdjacentHTML('beforeend', `
            <div id="student-details-modal" class="fixed inset-0 z-[110] hidden">
                <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" onclick="closeModal('student-details-modal')"></div>
                <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-full max-w-2xl bg-white dark:bg-gray-800 rounded-3xl p-8 shadow-2xl relative">
                    <button onclick="closeModal('student-details-modal')" class="absolute top-4 right-4 text-gray-400 hover:text-gray-700 bg-gray-100 dark:bg-gray-700 w-8 h-8 flex items-center justify-center rounded-full"><i class="fas fa-times"></i></button>
                    <div id="student-details-content"></div>
                </div>
            </div>
        `);
    }

    const contentDiv = document.getElementById('student-details-content');
    contentDiv.innerHTML = '<div class="text-center py-8"><i class="fas fa-spinner fa-spin text-2xl text-blue-500"></i><p class="mt-2 text-gray-500">Loading Backlog data...</p></div>';
    showModal('student-details-modal');

    try {
        const response = await authFetch(`${ADMIN_API_BASE_URL}/students/${id}/backlogs`);
        const backlogs = response.ok ? await response.json() : [];

        const pendingCount = backlogs.filter(b => b.status === "PENDING").length;
        const totalCount = backlogs.length;

        let backlogHtml = backlogs.length === 0
            ? '<p class="text-sm text-green-600 font-bold bg-green-50 p-3 rounded-lg"><i class="fas fa-check-circle mr-1"></i> ✅ Regular Student (No Backlogs)</p>'
            : `<div class="overflow-hidden rounded-lg border border-gray-200 dark:border-gray-700">
                <table class="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                    <thead class="bg-gray-50 dark:bg-gray-800">
                        <tr>
                            <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Subject Name</th>
                            <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Semester</th>
                            <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                        </tr>
                    </thead>
                    <tbody class="bg-white dark:bg-gray-900 divide-y divide-gray-200 dark:divide-gray-700">
                        ${backlogs.map(b => {
                            const isPending = b.status === "PENDING";
                            const statusColor = isPending ? "bg-red-100 text-red-800" : "bg-green-100 text-green-800";
                            const statusIcon = isPending ? "fa-exclamation-circle" : "fa-check-circle";
                            return `
                            <tr>
                                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900 dark:text-white">${b.subject}</td>
                                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400 font-mono">${b.semester}</td>
                                <td class="px-6 py-4 whitespace-nowrap text-sm">
                                    <span class="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${statusColor}">
                                        <i class="fas ${statusIcon} mr-1 mt-[2px]"></i> ${b.status}
                                    </span>
                                </td>
                            </tr>
                            `;
                        }).join('')}
                    </tbody>
                </table>
               </div>`;

        const collegeName = (student.college && student.college.name)
            ? student.college.name
            : (student.collegeName || 'Unknown');

        contentDiv.innerHTML = `
            <div class="flex items-center gap-4 mb-6 border-b pb-4 dark:border-gray-700">
                <div class="w-16 h-16 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center text-3xl font-bold shadow-inner uppercase">${student.name ? student.name.charAt(0) : '?'}</div>
                <div>
                    <h3 class="text-2xl font-bold text-gray-900 dark:text-white">${student.name || 'Unknown'}</h3>
                    <p class="text-gray-500 font-mono text-sm">${student.prn || 'N/A'}</p>
                </div>
            </div>
            <div class="grid grid-cols-2 gap-4 mb-6">
                <div class="bg-gray-50 dark:bg-gray-900 p-3 rounded-lg"><span class="block text-xs font-bold text-gray-400 uppercase tracking-wider">College</span><span class="font-medium">${collegeName}</span></div>
                <div class="bg-gray-50 dark:bg-gray-900 p-3 rounded-lg"><span class="block text-xs font-bold text-gray-400 uppercase tracking-wider">Course</span><span class="font-medium">${student.course || 'N/A'}</span></div>
                <div class="bg-gray-50 dark:bg-gray-900 p-3 rounded-lg"><span class="block text-xs font-bold text-gray-400 uppercase tracking-wider">Department</span><span class="font-medium">${student.departmentEntity?.name || student.department || 'N/A'}</span></div>
                <div class="bg-gray-50 dark:bg-gray-900 p-3 rounded-lg"><span class="block text-xs font-bold text-gray-400 uppercase tracking-wider">Semester</span><span class="font-medium">${student.semester || 'N/A'}</span></div>
                <div class="bg-gray-50 dark:bg-gray-900 p-3 rounded-lg"><span class="block text-xs font-bold text-gray-400 uppercase tracking-wider">Student Type</span><span class="font-medium text-blue-600">${student.studentType || 'REGULAR'}</span></div>
                <div class="bg-gray-50 dark:bg-gray-900 p-3 rounded-lg"><span class="block text-xs font-bold text-gray-400 uppercase tracking-wider">Fees Paid</span><span class="font-medium ${student.feesPaid ? 'text-green-600' : 'text-red-500'}">${student.feesPaid ? 'Yes' : 'No'}</span></div>
            </div>
            <div class="mb-2">
                <div class="flex justify-between items-end border-b pb-1 dark:border-gray-700 mb-2">
                    <h4 class="font-bold text-gray-700 dark:text-gray-300 text-sm uppercase tracking-widest">Academic Backlogs</h4>
                    ${totalCount > 0 ? `<span class="text-xs font-bold px-2 py-1 bg-gray-200 dark:bg-gray-700 rounded-md shadow-sm">Total: ${totalCount} | Pending: ${pendingCount}</span>` : ''}
                </div>
                ${backlogHtml}
            </div>
        `;
    } catch (e) {
        contentDiv.innerHTML = '<div class="text-center py-8 text-red-500"><i class="fas fa-wifi text-4xl mb-2"></i><p>Failed to load connected data.</p></div>';
    }
};

window.toggleStudentStatus = async function (id, activeStatus) {
    if (!confirm("Are you sure you want to alter this student's access rules?")) return;
    try {
        const token = localStorage.getItem('token');
        await fetch(`${ADMIN_API_BASE_URL}/students/${id}/status`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                ...(token ? { 'Authorization': 'Bearer ' + token } : {})
            },
            body: JSON.stringify({ active: activeStatus })
        });
        window.loadStudents();
    } catch (err) {
        console.error(err);
        alert('Failed to establish connection for status update.');
    }
};

window.removeStudent = async function (id) {
    if (!confirm("Are you absolutely certain you want to remove this student completely from the University database? This action is irrecoverable!")) return;
    try {
        const token = localStorage.getItem('token');
        await fetch(`${ADMIN_API_BASE_URL}/students/${id}`, {
            method: 'DELETE',
            headers: token ? { 'Authorization': 'Bearer ' + token } : {}
        });
        window.loadStudents();
    } catch (err) {
        console.error(err);
        alert('Failed to establish connection for target deletion.');
    }
};

// 4. Inject CSV Upload Modal (no college dropdown — uses context)
function injectStudentModal() {
    if (document.getElementById('add-student-modal')) {
        setupStudentUploadListener();
        return;
    }

    const modalHtml = `
        <div id="add-student-modal" class="fixed inset-0 z-[100] hidden">
            <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" onclick="closeModal('add-student-modal')"></div>
            <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-full max-w-lg bg-white dark:bg-gray-800 rounded-3xl p-8 shadow-2xl relative">

                <button onclick="closeModal('add-student-modal')" class="absolute top-4 right-4 text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 transition-colors bg-gray-100 dark:bg-gray-700 w-8 h-8 flex items-center justify-center rounded-full">
                    <i class="fas fa-times"></i>
                </button>

                <div class="mb-6 flex items-center gap-4">
                    <div class="w-12 h-12 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center text-2xl shadow-inner">
                        <i class="fas fa-file-csv"></i>
                    </div>
                    <div>
                        <h3 class="text-2xl font-bold font-display text-gray-900 dark:text-white">Bulk Add Students</h3>
                        <p class="text-sm text-gray-500">Upload a designated CSV to synchronize records.</p>
                    </div>
                </div>

                <!-- Active College Context Info Banner -->
                <div id="student-modal-college-banner" class="mb-4 px-4 py-3 rounded-xl bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-700 flex items-center gap-3">
                    <i class="fas fa-building text-blue-500"></i>
                    <div>
                        <p class="text-xs font-bold text-blue-600 dark:text-blue-400 uppercase tracking-wider">Assigning to College</p>
                        <p id="student-modal-college-name" class="font-bold text-gray-900 dark:text-white">—</p>
                    </div>
                </div>

                <div class="bg-gray-50 dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-xl p-4 mb-6">
                    <h4 class="text-xs font-bold text-gray-400 uppercase tracking-widest mb-2 border-b border-gray-200 dark:border-gray-700 pb-2">CSV Formatting Rules</h4>
                    <ul class="text-sm text-gray-600 dark:text-gray-400 leading-relaxed font-mono">
                        <li>Column 1: <strong>PRN</strong> (Unique Identifier)</li>
                        <li>Column 2: <strong>Full Name</strong></li>
                        <li>Column 3: <strong>Email</strong></li>
                        <li>Column 4: <strong>Course</strong></li>
                        <li>Column 5: <strong>Department</strong></li>
                        <li>Column 6: <strong>Semester</strong></li>
                        <li>Column 7: <strong>Backlogs</strong> <span class="text-[10px] text-gray-400 ml-1">(Format: CN-4;Math3-3) *Optional</span></li>
                    </ul>
                </div>

                <form id="student-upload-form" class="space-y-6">
                    <div class="border-2 border-dashed border-blue-300 dark:border-gray-600 rounded-2xl p-8 text-center hover:bg-blue-50 dark:hover:bg-gray-700/50 transition-colors group cursor-pointer relative" onclick="document.getElementById('student-csv-input').click()">
                        <input type="file" id="student-csv-input" accept=".csv" required class="hidden">
                        <i class="fas fa-cloud-upload-alt text-4xl text-blue-400 group-hover:text-blue-600 group-hover:-translate-y-1 transition-all"></i>
                        <p class="mt-3 text-sm font-medium text-gray-600 dark:text-gray-300" id="student-file-name">Click here to browse files</p>
                        <p class="text-xs text-gray-400 mt-1">Accepts strictly .csv format</p>
                    </div>

                    <div id="student-upload-alert" class="hidden p-4 rounded-xl text-sm font-bold flex items-center gap-2"></div>

                    <div class="flex gap-3">
                        <button type="button" onclick="closeModal('add-student-modal')" class="flex-1 py-3 bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold rounded-xl transition-colors">Cancel</button>
                        <button type="submit" id="student-upload-submit" class="flex-1 py-3 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-xl shadow-lg shadow-blue-500/30 transition-all flex items-center justify-center gap-2">
                            <span>Start Processing</span> <i class="fas fa-arrow-right"></i>
                        </button>
                    </div>
                </form>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHtml);
    setupStudentUploadListener();
}

// 5. Listeners
function setupStudentUploadListener() {
    const addBtn = document.getElementById('btn-add-student');
    if (addBtn && !addBtn.getAttribute('data-listener')) {
        addBtn.addEventListener('click', () => {
            // Update college banner inside modal
            const bannerName = document.getElementById('student-modal-college-name');
            if (bannerName) {
                bannerName.textContent = window.CollegeContext?.selectedCollegeName || '(None selected)';
            }
            document.getElementById('student-upload-form').reset();
            document.getElementById('student-upload-alert').classList.add('hidden');
            const fn = document.getElementById('student-file-name');
            if (fn) { fn.textContent = 'Click here to browse files'; fn.classList.remove('text-blue-600', 'font-bold'); }
            showModal('add-student-modal');
        });
        addBtn.setAttribute('data-listener', 'true');
    }

    const fileInput = document.getElementById('student-csv-input');
    const fileNameEl = document.getElementById('student-file-name');
    if (fileInput && fileNameEl && !fileInput.getAttribute('data-listener')) {
        fileInput.addEventListener('change', (e) => {
            if (e.target.files.length > 0) {
                fileNameEl.classList.add('text-blue-600', 'font-bold');
                fileNameEl.innerText = e.target.files[0].name;
            } else {
                fileNameEl.classList.remove('text-blue-600', 'font-bold');
                fileNameEl.innerText = "Click here to browse files";
            }
        });
        fileInput.setAttribute('data-listener', 'true');
    }

    const form = document.getElementById('student-upload-form');
    if (form && !form.getAttribute('data-listener')) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();

            const file = fileInput.files[0];
            if (!file) return;

            const submitBtn = document.getElementById('student-upload-submit');
            const alertBox = document.getElementById('student-upload-alert');

            // Derive collegeId from CollegeContext — no dropdown in this modal
            const collegeId = window.CollegeContext?.selectedCollegeId;
            if (!collegeId) {
                alertBox.classList.remove('hidden');
                alertBox.className = 'p-4 rounded-xl text-sm font-bold flex items-center gap-2 bg-amber-50 text-amber-700 border border-amber-200';
                alertBox.innerHTML = `<i class="fas fa-exclamation-triangle text-lg"></i> Please select a college from the header context bar before uploading.`;
                return;
            }

            submitBtn.disabled = true;
            submitBtn.innerHTML = `<i class="fas fa-circle-notch fa-spin"></i> Processing...`;
            alertBox.classList.add('hidden');

            const formData = new FormData();
            formData.append('file', file);
            formData.append('collegeId', collegeId);

            try {
                const token = localStorage.getItem('token');
                const response = await fetch(`${ADMIN_API_BASE_URL}/students/upload`, {
                    method: 'POST',
                    headers: token ? { 'Authorization': `Bearer ${token}` } : {},
                    body: formData
                });

                const textData = await response.text();

                if (response.ok) {
                    alertBox.classList.remove('hidden');
                    alertBox.className = 'p-4 rounded-xl text-sm font-bold flex items-center gap-2 bg-green-50 text-green-700 border border-green-200';
                    alertBox.innerHTML = `<i class="fas fa-check-circle text-lg"></i> ${textData}`;
                    setTimeout(() => {
                        closeModal('add-student-modal');
                        alertBox.classList.add('hidden');
                        if (fileInput) fileInput.value = '';
                        if (fileNameEl) { fileNameEl.innerText = "Click here to browse files"; fileNameEl.classList.remove('text-blue-600', 'font-bold'); }
                        window.loadStudents();
                    }, 2500);
                } else {
                    alertBox.classList.remove('hidden');
                    alertBox.className = 'p-4 rounded-xl text-sm font-bold flex items-center gap-2 bg-red-50 text-red-700 border border-red-200';
                    alertBox.innerHTML = `<i class="fas fa-times-circle text-lg"></i> ${textData}`;
                }
            } catch (err) {
                alertBox.classList.remove('hidden');
                alertBox.className = 'p-4 rounded-xl text-sm font-bold flex items-center gap-2 bg-red-50 text-red-700 border border-red-200';
                alertBox.innerHTML = `<i class="fas fa-plug text-lg"></i> Server connectivity error.`;
            } finally {
                submitBtn.disabled = false;
                submitBtn.innerHTML = `<span>Start Processing</span> <i class="fas fa-arrow-right"></i>`;
            }
        });
        form.setAttribute('data-listener', 'true');
    }
}
