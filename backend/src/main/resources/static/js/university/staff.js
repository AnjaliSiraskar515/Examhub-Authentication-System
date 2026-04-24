// staff.js - Logic for Managing Faculty / Supervisors
// Uses global CollegeContext (collegeContext.js) for filtering.
// No college dropdown in modals — context bar drives all assignments.

let staffList = []; // Memory cache

// ─── Listen for College Context Changes ──────────────────────────────────────
document.addEventListener('collegeContextChanged', () => {
    const section = document.getElementById('staff-section');
    if (section && !section.classList.contains('hidden')) {
        window.loadStaff();
    }
});

let currentStaffDeptFilter = 'All Departments';

// Hook up filter buttons
document.addEventListener('DOMContentLoaded', () => {
    // Find all department filter buttons in staff section
    const deptButtons = document.querySelectorAll('#staff-section button.rounded-full');
    deptButtons.forEach(btn => {
        btn.addEventListener('click', (e) => {
            // Unselect all
            deptButtons.forEach(b => {
                b.classList.remove('bg-blue-100', 'text-blue-700', 'hover:bg-blue-200');
                b.classList.add('bg-gray-100', 'text-gray-700', 'hover:bg-gray-200');
            });
            // Select clicked
            e.target.classList.remove('bg-gray-100', 'text-gray-700', 'hover:bg-gray-200');
            e.target.classList.add('bg-blue-100', 'text-blue-700', 'hover:bg-blue-200');
            
            // Re-load staff with new filter
            currentStaffDeptFilter = e.target.innerText.trim();
            window.loadStaff();
        });
    });
});

// 1. Initial Load Function
window.loadStaff = async function () {
    const tbody = document.getElementById('staff-table-body');
    if (!tbody) return;

    // Guard: require college selection
    if (!window.CollegeContext || !window.CollegeContext.selectedCollegeId) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="px-6 py-12 text-center">
                    <div class="flex flex-col items-center gap-3 text-gray-400 dark:text-gray-500">
                        <i class="fas fa-users text-4xl opacity-40"></i>
                        <p class="font-semibold text-base">Please select a college to view staff</p>
                        <p class="text-sm opacity-70">Use the college selector in the header above</p>
                    </div>
                </td>
            </tr>`;
        const suppStaff = document.getElementById('support-staff');
        if (suppStaff) suppStaff.innerText = '—';
        injectStaffModal();
        return;
    }

    try {
        tbody.innerHTML = `<tr><td colspan="5" class="px-6 py-4 text-center text-gray-500"><i class="fas fa-spinner fa-spin mr-2"></i>Loading staff data...</td></tr>`;

        const collegeId = window.CollegeContext.selectedCollegeId;
        let url = `${ADMIN_API_BASE_URL}/supervisors?collegeId=${collegeId}`;
        
        if (currentStaffDeptFilter && currentStaffDeptFilter !== 'All Departments') {
            url += `&department=${encodeURIComponent(currentStaffDeptFilter)}`;
        }

        const response = await authFetch(url);
        if (!response.ok) throw new Error('Failed to fetch supervisors');

        staffList = await response.json();

        const suppStaff = document.getElementById('support-staff');
        if (suppStaff) suppStaff.innerText = staffList.length;

        renderStaffTable();
        injectStaffModal();

    } catch (e) {
        console.error('Error loading staff:', e);
        if (tbody) {
            tbody.innerHTML = `<tr><td colspan="5" class="px-6 py-4 text-center text-red-500"><i class="fas fa-exclamation-triangle mr-2"></i>Failed to load staff. Backend may be offline.</td></tr>`;
        }
    }
};

// 2. Render Table
function renderStaffTable() {
    const tbody = document.getElementById('staff-table-body');
    if (!tbody) return;

    if (staffList.length === 0) {
        const collegeName = window.CollegeContext?.selectedCollegeName || 'selected college';
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="px-6 py-12 text-center">
                    <div class="flex flex-col items-center gap-3 text-gray-400 dark:text-gray-500">
                        <i class="fas fa-user-tie text-4xl opacity-40"></i>
                        <p class="font-semibold text-base">No staff found for <span class="text-green-500">${collegeName}</span></p>
                        <p class="text-sm opacity-70">Onboard staff using the button above or upload a CSV</p>
                    </div>
                </td>
            </tr>`;
        return;
    }

    const html = staffList.map(staff => {
        const photoUrl = staff.photoPath ? `http://localhost:8080/${staff.photoPath}` : null;
        const avatarHtml = photoUrl
            ? `<img src="${photoUrl}" alt="${staff.name}" class="w-8 h-8 rounded-full object-cover border border-gray-200" onerror="this.style.display='none'; this.nextElementSibling.style.display='flex'">
               <div class="w-8 h-8 rounded-full bg-blue-100 dark:bg-blue-900/50 text-blue-600 justify-center items-center font-bold uppercase text-xs hidden">${(staff.name || 'U').substring(0, 2)}</div>`
            : `<div class="w-8 h-8 rounded-full bg-blue-100 dark:bg-blue-900/50 text-blue-600 flex justify-center items-center font-bold uppercase text-xs">${(staff.name || 'U').substring(0, 2)}</div>`;

        return `
            <tr class="border-b dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors cursor-pointer">
                <td class="px-6 py-4 font-bold text-gray-900 dark:text-gray-200">#${staff.userId}</td>
                <td class="px-6 py-4">
                    <div class="flex items-center gap-3">
                        <div class="flex items-center shrink-0">${avatarHtml}</div>
                        <div>
                            <div class="font-bold text-gray-900 dark:text-white">${staff.name || 'N/A'}</div>
                            <div class="text-[10px] text-gray-400 font-mono tracking-wide">${staff.email || 'N/A'}</div>
                        </div>
                    </div>
                </td>
                <td class="px-6 py-4 font-medium text-gray-700 dark:text-gray-300">
                    ${staff.department || '<span class="text-gray-400 italic">Not Assigned</span>'}
                </td>
                <td class="px-6 py-4 text-gray-700 font-bold dark:text-gray-300">
                    <div>${staff.designation || 'Supervisor'}</div>
                    <div class="text-[10px] text-gray-400 font-mono tracking-wide mt-1">${staff.supervisorType === 'HEAD' ? '<span class="text-indigo-500"><i class="fas fa-crown"></i> Head Supervisor</span>' : 'Exam Supervisor'}</div>
                </td>
                <td class="px-6 py-4"><span class="px-2 py-1 bg-green-100 text-green-700 text-xs font-bold rounded-md uppercase tracking-wider shadow-sm"><i class="fas fa-check-circle mr-1"></i>Active</span></td>
            </tr>
        `;
    }).join('');

    tbody.innerHTML = html;
}

// 3. Inject Add-Staff Modal
function injectStaffModal() {
    if (document.getElementById('add-staff-modal')) {
        setupStaffFormListener();
        return;
    }

    const modalHtml = `
        <div id="add-staff-modal" class="fixed inset-0 z-[100] hidden">
            <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" onclick="closeModal('add-staff-modal')"></div>
            <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-full max-w-lg bg-white dark:bg-gray-800 rounded-3xl p-8 shadow-2xl relative">

                <button onclick="closeModal('add-staff-modal')" class="absolute top-4 right-4 text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 transition-colors bg-gray-100 dark:bg-gray-700 w-8 h-8 flex items-center justify-center rounded-full">
                    <i class="fas fa-times"></i>
                </button>

                <div class="mb-6 flex items-center gap-4">
                    <div class="w-12 h-12 bg-green-100 text-green-600 rounded-full flex items-center justify-center text-xl shadow-inner">
                        <i class="fas fa-user-tie"></i>
                    </div>
                    <div>
                        <h3 class="text-2xl font-bold font-display text-gray-900 dark:text-white">Onboard Staff</h3>
                        <p class="text-[12px] text-gray-500 leading-tight mt-1">Credentials will be generated and automatically mailed to the provided email address by the system.</p>
                    </div>
                </div>

                <!-- Active College Context Info Banner -->
                <div id="staff-modal-college-banner" class="mb-5 px-4 py-3 rounded-xl bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-700 flex items-center gap-3">
                    <i class="fas fa-building text-green-500"></i>
                    <div>
                        <p class="text-xs font-bold text-green-600 dark:text-green-400 uppercase tracking-wider">Assigning to College</p>
                        <p id="staff-modal-college-name" class="font-bold text-gray-900 dark:text-white">—</p>
                    </div>
                </div>

                <form id="staff-onboarding-form" class="space-y-4">
                    <div>
                        <label class="block text-xs font-bold mb-1 text-gray-600 dark:text-gray-300 uppercase tracking-widest">Full Name</label>
                        <input type="text" id="staff-name" required placeholder="Dr. John Doe" class="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 outline-none focus:ring-2 focus:ring-green-500 transition-all text-gray-900 dark:text-white font-medium">
                    </div>

                    <div class="grid grid-cols-2 gap-4">
                        <div>
                            <label class="block text-xs font-bold mb-1 text-gray-600 dark:text-gray-300 uppercase tracking-widest">Email Address</label>
                            <input type="email" id="staff-email" required placeholder="john@university.edu" class="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 outline-none focus:ring-2 focus:ring-green-500 transition-all text-gray-900 dark:text-white font-medium">
                        </div>
                        <div>
                            <label class="block text-xs font-bold mb-1 text-gray-600 dark:text-gray-300 uppercase tracking-widest">Phone Number</label>
                            <input type="tel" id="staff-phone" required placeholder="1234567890" class="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 outline-none focus:ring-2 focus:ring-green-500 transition-all text-gray-900 dark:text-white font-medium">
                        </div>
                    </div>

                    <div class="grid grid-cols-2 gap-4">
                        <div>
                            <label class="block text-xs font-bold mb-1 text-gray-600 dark:text-gray-300 uppercase tracking-widest">Designation</label>
                            <select id="staff-designation" class="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 outline-none focus:ring-2 focus:ring-green-500 transition-all text-gray-900 dark:text-white font-bold cursor-pointer">
                                <option value="Senior Invigilator">Senior Invigilator</option>
                                <option value="Chief Supervisor">Chief Supervisor</option>
                                <option value="Lab Assistant">Lab Assistant</option>
                                <option value="Support Staff">Support Staff</option>
                            </select>
                        </div>
                        <div>
                            <label class="block text-xs font-bold mb-1 text-gray-600 dark:text-gray-300 uppercase tracking-widest">Department</label>
                            <select id="staff-department" class="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 outline-none focus:ring-2 focus:ring-green-500 transition-all text-gray-900 dark:text-white font-bold cursor-pointer">
                                <option value="Computer Science">Computer Science</option>
                                <option value="Administration">Administration</option>
                                <option value="Finance">Finance</option>
                                <option value="Humanities">Humanities</option>
                            </select>
                        </div>
                    </div>

                    <div>
                        <label class="block text-xs font-bold mb-1 text-gray-600 dark:text-gray-300 uppercase tracking-widest">Supervisor Role</label>
                        <select id="staff-supervisorType" class="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 outline-none focus:ring-2 focus:ring-green-500 transition-all text-gray-900 dark:text-white font-bold cursor-pointer">
                            <option value="EXAM">Exam Supervisor (Specific Exams)</option>
                            <option value="HEAD">Head Supervisor (College-Level Access)</option>
                        </select>
                    </div>

                    <div id="staff-upload-alert" class="hidden p-4 rounded-xl text-sm font-bold flex items-center gap-2 mt-4"></div>

                    <div class="flex gap-3 mt-6 pt-4 border-t border-gray-100 dark:border-gray-700">
                        <button type="button" onclick="closeModal('add-staff-modal')" class="flex-1 py-3 bg-gray-100 dark:bg-gray-700 dark:text-white hover:bg-gray-200 text-gray-700 font-bold rounded-xl transition-colors">Cancel</button>
                        <button type="submit" id="staff-upload-submit" class="flex-[1.5] py-3 bg-green-600 hover:bg-green-700 text-white font-bold rounded-xl shadow-lg shadow-green-500/30 transition-all flex items-center justify-center gap-2">
                            <span>Register Supervisor</span> <i class="fas fa-paper-plane text-sm"></i>
                        </button>
                    </div>
                </form>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHtml);
    setupStaffFormListener();
}

// 4. Form Submission Listeners
function setupStaffFormListener() {
    const addBtn = document.getElementById('btn-add-staff');
    if (addBtn && !addBtn.getAttribute('data-listener')) {
        addBtn.addEventListener('click', () => {
            // Update college banner inside modal
            const bannerName = document.getElementById('staff-modal-college-name');
            if (bannerName) {
                bannerName.textContent = window.CollegeContext?.selectedCollegeName || '(None selected)';
            }
            document.getElementById('staff-onboarding-form').reset();
            document.getElementById('staff-upload-alert').classList.add('hidden');
            showModal('add-staff-modal');
        });
        addBtn.setAttribute('data-listener', 'true');
    }

    const form = document.getElementById('staff-onboarding-form');
    if (form && !form.getAttribute('data-listener')) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();

            const nameEl = document.getElementById('staff-name').value.trim();
            const emailEl = document.getElementById('staff-email').value.trim();
            const phoneEl = document.getElementById('staff-phone').value.trim();
            const desigEl = document.getElementById('staff-designation').value.trim();
            const deptEl = document.getElementById('staff-department').value.trim();
            const typeEl = document.getElementById('staff-supervisorType').value.trim();

            // Derive collegeId from CollegeContext — no dropdown in this modal
            const collegeId = window.CollegeContext?.selectedCollegeId;

            const submitBtn = document.getElementById('staff-upload-submit');
            const alertBox = document.getElementById('staff-upload-alert');

            if (!collegeId) {
                alertBox.classList.remove('hidden');
                alertBox.className = 'p-4 rounded-xl text-sm font-bold flex items-center gap-2 mt-4 bg-amber-50 text-amber-700 border border-amber-200';
                alertBox.innerHTML = `<i class="fas fa-exclamation-triangle text-lg"></i> Please select a college from the header context bar before registering staff.`;
                return;
            }

            submitBtn.disabled = true;
            submitBtn.innerHTML = `<i class="fas fa-circle-notch fa-spin"></i> Processing & Mailing...`;
            alertBox.classList.add('hidden');
            alertBox.className = 'hidden p-4 rounded-xl text-sm font-bold flex items-center gap-2 mt-4';

            try {
                const response = await authFetch(`${ADMIN_API_BASE_URL}/supervisors`, {
                    method: 'POST',
                    body: JSON.stringify({
                        name: nameEl,
                        email: emailEl,
                        phone: phoneEl,
                        designation: desigEl,
                        department: deptEl,
                        collegeId: collegeId,
                        supervisorType: typeEl
                    })
                });

                const textData = await response.text();

                if (response.ok || response.status === 200) {
                    alertBox.classList.remove('hidden');
                    alertBox.className = 'p-4 rounded-xl text-sm font-bold flex items-center gap-2 mt-4 bg-green-50 text-green-700 border border-green-200';
                    alertBox.innerHTML = `<i class="fas fa-check-circle text-lg"></i> ${textData}`;
                    setTimeout(() => {
                        closeModal('add-staff-modal');
                        window.loadStaff();
                    }, 2500);
                } else {
                    alertBox.classList.remove('hidden');
                    alertBox.className = 'p-4 rounded-xl text-sm font-bold flex items-center gap-2 mt-4 bg-red-50 text-red-700 border border-red-200';
                    alertBox.innerHTML = `<i class="fas fa-times-circle text-lg"></i> ${textData}`;
                }
            } catch (err) {
                alertBox.classList.remove('hidden');
                alertBox.className = 'p-4 rounded-xl text-sm font-bold flex items-center gap-2 mt-4 bg-red-50 text-red-700 border border-red-200';
                alertBox.innerHTML = `<i class="fas fa-plug text-lg"></i> Server connectivity error.`;
            } finally {
                submitBtn.disabled = false;
                submitBtn.innerHTML = `<span>Register Supervisor</span> <i class="fas fa-paper-plane text-sm"></i>`;
            }
        });
        form.setAttribute('data-listener', 'true');
    }

    // Connect Staff CSV Upload button
    const csvBtn = document.getElementById('btn-staff-csv');
    if (csvBtn && !csvBtn.getAttribute('data-listener')) {
        csvBtn.addEventListener('click', () => {
            if (!document.getElementById('staff-csv-modal')) {
                const csvModalHtml = `
                    <div id="staff-csv-modal" class="fixed inset-0 z-[100] hidden">
                        <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" onclick="closeModal('staff-csv-modal')"></div>
                        <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-full max-w-lg bg-white dark:bg-gray-800 rounded-3xl p-8 shadow-2xl relative">
                            <button onclick="closeModal('staff-csv-modal')" class="absolute top-4 right-4 text-gray-400 hover:text-gray-700 bg-gray-100 dark:bg-gray-700 w-8 h-8 flex items-center justify-center rounded-full"><i class="fas fa-times"></i></button>
                            <h3 class="text-2xl font-bold mb-2">Bulk Onboard Staff</h3>
                            <p class="text-sm text-gray-500 mb-4">Upload a designated CSV to synchronize records.</p>

                            <!-- Active College Context Info Banner -->
                            <div class="mb-4 px-4 py-3 rounded-xl bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-700 flex items-center gap-3">
                                <i class="fas fa-building text-green-500"></i>
                                <div>
                                    <p class="text-xs font-bold text-green-600 uppercase tracking-wider">Assigning to College</p>
                                    <p id="staff-csv-college-name" class="font-bold text-gray-900 dark:text-white">—</p>
                                </div>
                            </div>

                            <ul class="text-sm font-mono mb-6 pb-4 border-b dark:border-gray-700">
                                <li>Col 1: Name</li><li>Col 2: Email</li><li>Col 3: Phone</li><li>Col 4: Designation</li><li>Col 5: Department</li><li>Col 6: Role (EXAM / HEAD)</li>
                            </ul>
                            <form id="staff-csv-form">
                                <input type="file" id="staff-csv-input" accept=".csv" required class="w-full mb-4">
                                <div id="staff-csv-alert" class="hidden mb-4 p-4 text-sm font-bold rounded-xl"></div>
                                <div class="flex gap-3">
                                    <button type="submit" id="staff-csv-submit" class="flex-1 py-3 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-xl">Upload CSV</button>
                                </div>
                            </form>
                        </div>
                    </div>
                `;
                document.body.insertAdjacentHTML('beforeend', csvModalHtml);

                document.getElementById('staff-csv-form').addEventListener('submit', async (e) => {
                    e.preventDefault();
                    const fileInput = document.getElementById('staff-csv-input');
                    if (!fileInput.files[0]) return;

                    const btn = document.getElementById('staff-csv-submit');
                    const alertBox = document.getElementById('staff-csv-alert');
                    btn.disabled = true;
                    btn.innerText = 'Uploading...';

                    // Derive collegeId from CollegeContext — no dropdown in this modal
                    const collegeId = window.CollegeContext?.selectedCollegeId;
                    if (!collegeId) {
                        alertBox.className = 'mb-4 p-4 text-sm font-bold rounded-xl bg-amber-50 text-amber-700 border border-amber-200';
                        alertBox.innerHTML = '<i class="fas fa-exclamation-triangle mr-2"></i>Please select a college from the header context bar before uploading.';
                        alertBox.classList.remove('hidden');
                        btn.disabled = false;
                        btn.innerText = 'Upload CSV';
                        return;
                    }

                    const formData = new FormData();
                    formData.append('file', fileInput.files[0]);
                    formData.append('collegeId', collegeId);

                    try {
                        const response = await authFetch(`${ADMIN_API_BASE_URL}/supervisors/upload`, {
                            method: 'POST',
                            body: formData
                        });
                        const data = await response.text();
                        alertBox.className = 'mb-4 p-4 text-sm font-bold rounded-xl ' + (response.ok ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700');
                        alertBox.innerHTML = data;
                        alertBox.classList.remove('hidden');
                        if (response.ok) {
                            setTimeout(() => { closeModal('staff-csv-modal'); window.loadStaff(); }, 2000);
                        }
                    } catch (err) {
                        alertBox.className = 'mb-4 p-4 text-sm font-bold rounded-xl bg-red-100 text-red-700';
                        alertBox.innerHTML = 'Connectivity error.';
                        alertBox.classList.remove('hidden');
                    } finally {
                        btn.disabled = false;
                        btn.innerText = 'Upload CSV';
                    }
                });
            }

            // Update college name in the CSV modal banner
            const csvCollegeName = document.getElementById('staff-csv-college-name');
            if (csvCollegeName) {
                csvCollegeName.textContent = window.CollegeContext?.selectedCollegeName || '(None selected)';
            }
            document.getElementById('staff-csv-form').reset();
            document.getElementById('staff-csv-alert').classList.add('hidden');
            showModal('staff-csv-modal');
        });
        csvBtn.setAttribute('data-listener', 'true');
    }
}
