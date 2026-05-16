// colleges.js - Logic for Managing Colleges

let collegesList = [];

window.loadAdminColleges = async function() {
    try {
        const tbody = document.getElementById('colleges-table-body');
        if (!tbody) return;

        tbody.innerHTML = `<tr><td colspan="5" class="px-6 py-4 text-center text-gray-500"><i class="fas fa-spinner fa-spin mr-2"></i>Loading colleges...</td></tr>`;

        const response = await authFetch(`${ADMIN_API_BASE_URL}/colleges`);
        if (!response.ok) throw new Error('Failed to fetch colleges');

        collegesList = await response.json();

        if (collegesList.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" class="px-6 py-4 text-center text-gray-500">No colleges found. Add a college to get started.</td></tr>`;
            return;
        }

        const html = collegesList.map(c => `
            <tr class="border-b dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
                <td class="px-6 py-4 font-bold text-gray-500 dark:text-gray-400">#${c.id}</td>
                <td class="px-6 py-4 font-bold text-blue-600 dark:text-blue-400 text-base">${c.name}</td>
                <td class="px-6 py-4 font-mono text-sm text-gray-500">${c.code || 'N/A'}</td>
                <td class="px-6 py-4">
                    <span id="stats-badge-${c.id}" class="text-xs text-gray-400 italic">
                        <button onclick="loadCollegeStats(${c.id})" class="px-3 py-1 bg-indigo-50 dark:bg-indigo-900/20 text-indigo-600 dark:text-indigo-400 hover:bg-indigo-100 dark:hover:bg-indigo-900/40 rounded text-xs font-medium transition-colors">
                            <i class="fas fa-chart-bar mr-1"></i>View Stats
                        </button>
                    </span>
                </td>
                <td class="px-6 py-4 text-right">
                    <button onclick="deleteCollege(${c.id})" class="px-3 py-1 bg-red-50 dark:bg-red-900/20 text-red-600 hover:bg-red-100 dark:hover:bg-red-900/40 rounded text-sm transition-colors">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            </tr>
        `).join('');

        tbody.innerHTML = html;

        // Refresh context bar options after colleges reload
        if (typeof window.initCollegeContext === 'function') {
            window.initCollegeContext();
        }

    } catch (e) {
        console.error(e);
        const tbody = document.getElementById('colleges-table-body');
        if (tbody) tbody.innerHTML = `<tr><td colspan="5" class="px-6 py-4 text-center text-red-500"><i class="fas fa-exclamation-triangle mr-2"></i>Failed to load colleges.</td></tr>`;
    }
};

window.loadCollegeStats = async function(id) {
    const badge = document.getElementById(`stats-badge-${id}`);
    if (!badge) return;

    badge.innerHTML = `<span class="text-gray-400"><i class="fas fa-circle-notch fa-spin mr-1"></i>Loading...</span>`;

    try {
        const response = await authFetch(`${ADMIN_API_BASE_URL}/colleges/${id}/stats`);
        if (!response.ok) throw new Error('Failed to fetch stats');
        const data = await response.json();
        badge.innerHTML = `
            <span class="inline-flex items-center gap-2">
                <span class="px-2 py-0.5 bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 rounded text-xs font-bold">
                    <i class="fas fa-user-graduate mr-1"></i>${data.totalStudents} Students
                </span>
                <span class="px-2 py-0.5 bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300 rounded text-xs font-bold">
                    <i class="fas fa-user-tie mr-1"></i>${data.totalSupervisors} Staff
                </span>
            </span>
        `;
    } catch (e) {
        badge.innerHTML = `<span class="text-red-500 text-xs">Failed to load</span>`;
    }
};

window.openCollegeModal = function() {
    if (!document.getElementById('add-college-modal')) {
        document.body.insertAdjacentHTML('beforeend', `
            <div id="add-college-modal" class="fixed inset-0 z-[100] hidden">
                <div class="absolute inset-0 bg-black/60" onclick="closeModal('add-college-modal')"></div>
                <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-full max-w-md bg-white dark:bg-gray-800 rounded-2xl p-6 shadow-2xl">
                    <h3 class="text-xl font-bold mb-4">Add New College</h3>
                    <form id="add-college-form" class="space-y-4">
                        <div>
                            <label class="block text-sm font-medium mb-1">College Name <span class="text-red-500">*</span></label>
                            <input type="text" id="college-name-input" required class="w-full px-4 py-2 border rounded-xl dark:bg-gray-700 dark:border-gray-600 outline-none focus:ring-2 focus:ring-blue-500">
                        </div>
                        <div>
                            <label class="block text-sm font-medium mb-1">College Code <span class="text-gray-400 font-normal">(Optional)</span></label>
                            <input type="text" id="college-code-input" class="w-full px-4 py-2 border rounded-xl dark:bg-gray-700 dark:border-gray-600 outline-none focus:ring-2 focus:ring-blue-500">
                        </div>
                        <div id="add-college-alert" class="hidden p-3 rounded-xl text-sm font-bold"></div>
                        <div class="flex gap-3 pt-2">
                            <button type="button" onclick="closeModal('add-college-modal')" class="flex-1 py-2 bg-gray-100 dark:bg-gray-700 dark:text-white text-gray-700 rounded-xl font-bold hover:bg-gray-200 transition-colors">Cancel</button>
                            <button type="submit" class="flex-1 py-2 bg-blue-600 text-white rounded-xl hover:bg-blue-700 font-bold transition-colors">Save College</button>
                        </div>
                    </form>
                </div>
            </div>
        `);

        document.getElementById('add-college-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            const name = document.getElementById('college-name-input').value.trim();
            const code = document.getElementById('college-code-input').value.trim();
            const alertBox = document.getElementById('add-college-alert');

            try {
                const response = await authFetch(`${ADMIN_API_BASE_URL}/colleges`, {
                    method: 'POST',
                    body: JSON.stringify({ name, code })
                });
                if (response.ok) {
                    closeModal('add-college-modal');
                    window.loadAdminColleges();
                } else {
                    const err = await response.json();
                    alertBox.className = 'p-3 rounded-xl text-sm font-bold bg-red-50 text-red-700 border border-red-200';
                    alertBox.textContent = err.error || 'Failed to add college';
                    alertBox.classList.remove('hidden');
                }
            } catch (err) {
                alertBox.className = 'p-3 rounded-xl text-sm font-bold bg-red-50 text-red-700 border border-red-200';
                alertBox.textContent = 'Server error';
                alertBox.classList.remove('hidden');
            }
        });
    }

    document.getElementById('add-college-form').reset();
    const alertBox = document.getElementById('add-college-alert');
    if (alertBox) alertBox.classList.add('hidden');
    showModal('add-college-modal');
};

window.deleteCollege = async function(id) {
    if (!confirm("Are you sure? This will delete the college and cannot be undone.")) return;
    try {
        const response = await authFetch(`${ADMIN_API_BASE_URL}/colleges/${id}`, { method: 'DELETE' });
        if (response.ok) {
            // If this was the selected college, clear context
            if (window.CollegeContext?.selectedCollegeId === id) {
                window.setSelectedCollege(null, null);
                const sel = document.getElementById('global-college-select');
                if (sel) sel.value = '';
            }
            window.loadAdminColleges();
        } else {
            alert('Delete failed. The college may have associated students or staff.');
        }
    } catch (e) {
        alert('Server error');
    }
};

// Hook up nav link clicks
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.nav-link').forEach(link => {
        link.addEventListener('click', (e) => {
            const target = e.currentTarget.getAttribute('data-target');
            if (target === 'colleges-section') {
                if (typeof window.loadAdminColleges === 'function') window.loadAdminColleges();
            }
        });
    });

    // CSV Upload logic for Colleges
    const csvBtn = document.getElementById('btn-college-csv');
    if (csvBtn) {
        // Create hidden file input
        let fileInput = document.getElementById('college-csv-input');
        if (!fileInput) {
            fileInput = document.createElement('input');
            fileInput.type = 'file';
            fileInput.id = 'college-csv-input';
            fileInput.accept = '.csv';
            fileInput.classList.add('hidden');
            document.body.appendChild(fileInput);

            fileInput.addEventListener('change', async (e) => {
                const file = e.target.files[0];
                if (!file) return;

                const formData = new FormData();
                // Add college list CSV
                formData.append('file', file);

                try {
                    csvBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Uploading...';
                    csvBtn.disabled = true;

                    const token = localStorage.getItem('token');
                    const response = await fetch(`${ADMIN_API_BASE_URL}/colleges/upload`, {
                        method: 'POST',
                        headers: token ? { 'Authorization': `Bearer ${token}` } : {},
                        body: formData
                    });

                    if (response.ok) {
                        const data = await response.json();
                        alert(data.message || 'CSV Uploaded Successfully!');
                        window.loadAdminColleges();
                    } else {
                        const errorData = await response.json().catch(()=>({}));
                        alert('Upload failed: ' + (errorData.error || 'Server error'));
                    }
                } catch (err) {
                    alert('Error connecting to server for upload.');
                    console.error(err);
                } finally {
                    csvBtn.innerHTML = '<i class="fas fa-file-csv"></i> Upload CSV';
                    csvBtn.disabled = false;
                    fileInput.value = ''; // Reset
                }
            });
        }
        
        csvBtn.addEventListener('click', () => fileInput.click());
    }
});
