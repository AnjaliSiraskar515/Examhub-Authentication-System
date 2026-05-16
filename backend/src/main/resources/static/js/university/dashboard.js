// Global Config
const API_BASE_URL = '/api/university';
const ADMIN_API_BASE_URL = '/api/admin';
const UNIVERSITY_ID = 1; // Hardcoded for demo

// Auth-aware fetch helper – attaches JWT token from localStorage
function authFetch(url, options = {}) {
    const token = localStorage.getItem('token');
    
    // Default headers
    const headers = { ...options.headers };
    
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    // Only set application/json if body is not FormData
    if (!(options.body instanceof FormData) && !headers['Content-Type']) {
        headers['Content-Type'] = 'application/json';
    } else if (headers['Content-Type'] === null) {
        delete headers['Content-Type']; // Allow explicit removal
    }

    return fetch(url, { ...options, headers });
}

// State Management
const DashboardState = {
    stats: {
        totalStudents: 0,
        activeExams: 0,
        totalRegistrations: 0,
        approvedRegistrations: 0,
        pendingApprovals: 0
    },
    exams: [],
    registrations: [],
    notifications: [],
    adminProfile: null
};

// Initialization
document.addEventListener('DOMContentLoaded', async () => {
    await initDashboard();
});

async function initDashboard() {
    setupNavigation();
    setupThemeToggle();
    await loadStats();
    await updateExamStats(); // Load exam stats on init

    // ── College Context (must come before loadStudents/loadStaff) ────────────
    if (typeof window.initCollegeContext === 'function') {
        await window.initCollegeContext();
    }

    // Wire up context-bar dropdown
    const globalCollegeSelect = document.getElementById('global-college-select');
    if (globalCollegeSelect) {
        globalCollegeSelect.addEventListener('change', (e) => {
            const selectedOption = e.target.options[e.target.selectedIndex];
            const id = e.target.value;
            const name = id ? selectedOption.text.replace(/\s*\(.*?\)\s*$/, '').trim() : null;
            window.setSelectedCollege(id || null, name);
        });
    }
    // ────────────────────────────────────────────────────────────────────────

    // Default load (will show placeholder if no college selected)
    loadExams();
    if (typeof window.loadStudents === 'function') window.loadStudents();
    if (typeof window.loadStaff === 'function') window.loadStaff();

    // Admin profile + security (should not block existing dashboard features)
    loadAdminProfile();
    setupProfileAndSecurityHandlers();
}

function setupNavigation() {
    const tabs = document.querySelectorAll('.nav-link');
    tabs.forEach(tab => {
        tab.addEventListener('click', (e) => {
            e.preventDefault();

            // UI Update
            tabs.forEach(t => t.classList.remove('active', 'bg-blue-50', 'text-blue-600'));
            tab.classList.add('active', 'bg-blue-50', 'text-blue-600');

            // Content Switch
            const targetId = tab.getAttribute('data-target');
            document.querySelectorAll('.tab-content').forEach(c => c.classList.add('hidden'));
            document.getElementById(targetId).classList.remove('hidden');

            // Toggle Global College Context Bar visibility
            const contextBar = document.getElementById('global-college-context-bar');
            if (contextBar) {
                if (targetId === 'students-section' || targetId === 'staff-section') {
                    contextBar.classList.remove('hidden');
                } else {
                    contextBar.classList.add('hidden');
                }
            }

            // Data Load based on tab
            if (targetId === 'exams-section') {
                loadExams();
                updateExamStats(); // Update stats when switching to exams tab
            }
            if (targetId === 'students-section' && typeof window.loadStudents === 'function') {
                window.loadStudents();
            }
            if (targetId === 'staff-section' && typeof window.loadStaff === 'function') {
                window.loadStaff();
            }
            if (targetId === 'registrations-section') loadRegistrations();
            if (targetId === 'analytics-section') loadAnalytics();
            if (targetId === 'communication-section' && typeof window.loadInboxMessages === 'function') {
                window.loadInboxMessages();
            }
        });
    });
}

function setupThemeToggle() {
    const btn = document.getElementById('theme-toggle');
    if (!btn) return;

    btn.addEventListener('click', () => {
        document.documentElement.classList.toggle('dark');
        // Save preference
        const isDark = document.documentElement.classList.contains('dark');
        localStorage.setItem('theme', isDark ? 'dark' : 'light');
    });

    // Init from storage
    if (localStorage.getItem('theme') === 'dark') {
        document.documentElement.classList.add('dark');
    }
}

async function loadStats() {
    try {
        const response = await authFetch(`${API_BASE_URL}/stats`);
        if (!response.ok) throw new Error('Failed to load stats');

        const data = await response.json();
        DashboardState.stats = data;
        renderStats();
    } catch (e) {
        console.error("Stats load error", e);
    }
}

function renderStats() {
    setText('total-students', DashboardState.stats.totalStudents);
    setText('active-exams', DashboardState.stats.activeExams);
    setText('total-registrations', DashboardState.stats.totalRegistrations);
    setText('pending-approvals', DashboardState.stats.pendingApprovals);

    // Update progress bar example
    const total = DashboardState.stats.totalRegistrations || 1;
    const approved = DashboardState.stats.approvedRegistrations || 0;
    const percent = Math.round((approved / total) * 100);

    const bar = document.getElementById('approval-progress-bar');
    if (bar) bar.style.width = `${percent}%`;
    setText('approval-percent', `${percent}%`);
}

// New function to update exam-specific stats
async function updateExamStats() {
    try {
        const response = await authFetch(`${API_BASE_URL}/${UNIVERSITY_ID}/exam`);
        const exams = await response.json();

        if (exams && exams.length > 0) {
            // Count upcoming exams (exams with date in the future or status = 'upcoming')
            const today = new Date();
            today.setHours(0, 0, 0, 0);

            const upcomingCount = exams.filter(exam => {
                if (exam.status && exam.status.toLowerCase() === 'upcoming') return true;
                if (exam.examDate) {
                    const examDate = new Date(exam.examDate);
                    return examDate >= today;
                }
                return false;
            }).length;

            setText('upcoming-exams', upcomingCount);

            // Count evaluations due (you can modify this logic based on your needs)
            const evaluationsCount = exams.filter(exam =>
                exam.status && exam.status.toLowerCase() === 'completed'
            ).length;
            setText('evaluations-due', evaluationsCount);
        } else {
            setText('upcoming-exams', 0);
            setText('evaluations-due', 0);
        }
    } catch (e) {
        console.error("Error updating exam stats", e);
        setText('upcoming-exams', 0);
        setText('evaluations-due', '--');
    }
}

// Utility
function setText(id, value) {
    const el = document.getElementById(id);
    if (el) el.innerText = value;
}

function formatDate(dateStr) {
    return new Date(dateStr).toLocaleDateString('en-US', {
        year: 'numeric', month: 'short', day: 'numeric'
    });
}

function showModal(id) {
    document.getElementById(id).classList.remove('hidden');
}

function closeModal(id) {
    document.getElementById(id).classList.add('hidden');
}

// ─── University Identity ──────────────────────────────────────────────────────

function _showLogoPreview(src) {
    const preview = document.getElementById('uni-logo-preview');
    const placeholder = document.getElementById('uni-logo-placeholder');
    if (!preview || !placeholder) return;
    preview.src = src;
    preview.classList.remove('hidden');
    placeholder.classList.add('hidden');
}

// Wire up logo file-input: local preview + immediate backend upload
document.addEventListener('DOMContentLoaded', () => {
    const logoInput = document.getElementById('uni-logo-input');
    if (logoInput) {
        logoInput.addEventListener('change', async (e) => {
            const file = e.target.files[0];
            if (!file) return;
            const MAX_BYTES = 10 * 1024 * 1024; // 10 MB
            if (file.size > MAX_BYTES) {
                alert('File is too large. Please choose an image under 10 MB.');
                e.target.value = '';
                return;
            }

            // 1. Show local preview immediately
            const reader = new FileReader();
            reader.onload = (ev) => _showLogoPreview(ev.target.result);
            reader.readAsDataURL(file);

            // 2. Upload to backend in background
            const msgEl = document.getElementById('uni-identity-message');
            try {
                const token = getAuthTokenOrNull();
                const formData = new FormData();
                formData.append('logo', file);

                const resp = await fetch(`${ADMIN_API_BASE_URL}/upload-logo`, {
                    method: 'POST',
                    headers: token ? { 'Authorization': 'Bearer ' + token } : {},
                    body: formData
                });
                const data = await resp.json().catch(() => ({}));
                if (!resp.ok) {
                    const err = data?.error || 'Logo upload failed';
                    if (msgEl) {
                        msgEl.innerText = '⚠️ ' + err;
                        msgEl.className = 'mt-3 p-3 rounded-xl text-sm text-red-600 bg-red-50';
                        msgEl.classList.remove('hidden');
                    }
                } else {
                    // Update preview to use the real server URL (avoids large data URLs)
                    if (data.logoUrl) _showLogoPreview('' + data.logoUrl);
                    if (msgEl) {
                        msgEl.innerText = '✅ Logo saved to server!';
                        msgEl.className = 'mt-3 p-3 rounded-xl text-sm text-green-700 bg-green-50';
                        msgEl.classList.remove('hidden');
                        setTimeout(() => msgEl.classList.add('hidden'), 3000);
                    }
                }
            } catch (err) {
                console.error('Logo upload error', err);
                if (msgEl) {
                    msgEl.innerText = '⚠️ Could not reach server. Logo shown locally only.';
                    msgEl.className = 'mt-3 p-3 rounded-xl text-sm text-orange-700 bg-orange-50';
                    msgEl.classList.remove('hidden');
                }
            }
        });
    }
});


function _setUniNameDisplay(name) {
    const textEl = document.getElementById('uni-name-text');
    if (!textEl) return;
    if (name) {
        textEl.innerText = name;
        textEl.classList.remove('text-gray-500', 'italic');
        textEl.classList.add('text-gray-900', 'dark:text-white');
    } else {
        textEl.innerText = 'Not set — click Edit to add';
        textEl.classList.add('text-gray-500', 'italic');
    }
}

function enableUniversityNameEdit() {
    const display = document.getElementById('uni-name-display');
    const input = document.getElementById('uni-name');
    const editBtn = document.getElementById('edit-uni-name-btn');
    const saveBtn = document.getElementById('save-uni-name-btn');
    const cancelBtn = document.getElementById('cancel-uni-name-btn');
    const textEl = document.getElementById('uni-name-text');

    // Pre-fill input with current display value
    if (input && textEl) {
        const current = textEl.innerText;
        input.value = (current === 'Not set — click Edit to add') ? '' : current;
    }

    display?.classList.add('hidden');
    input?.classList.remove('hidden');
    editBtn?.classList.add('hidden');
    saveBtn?.classList.remove('hidden');
    cancelBtn?.classList.remove('hidden');
    input?.focus();
}

function cancelUniversityNameEdit() {
    const display = document.getElementById('uni-name-display');
    const input = document.getElementById('uni-name');
    const editBtn = document.getElementById('edit-uni-name-btn');
    const saveBtn = document.getElementById('save-uni-name-btn');
    const cancelBtn = document.getElementById('cancel-uni-name-btn');

    display?.classList.remove('hidden');
    input?.classList.add('hidden');
    editBtn?.classList.remove('hidden');
    saveBtn?.classList.add('hidden');
    cancelBtn?.classList.add('hidden');
}

async function saveUniversityIdentity() {
    const input = document.getElementById('uni-name');
    const msgEl = document.getElementById('uni-identity-message');
    const newName = (input?.value || '').trim();

    if (!newName) {
        if (msgEl) {
            msgEl.innerText = 'Please enter a university name.';
            msgEl.className = 'mt-3 p-3 rounded-xl text-sm text-red-600 bg-red-50';
            msgEl.classList.remove('hidden');
        }
        return;
    }

    const saveBtn = document.getElementById('save-uni-name-btn');
    if (saveBtn) saveBtn.disabled = true;

    try {
        const response = await authFetch(`${ADMIN_API_BASE_URL}/update-profile`, {
            method: 'POST',
            body: JSON.stringify({ universityName: newName })
        });
        const data = await response.json().catch(() => ({}));

        if (!response.ok) {
            const err = data?.error || data?.message || 'Failed to update university name';
            if (msgEl) {
                msgEl.innerText = err;
                msgEl.className = 'mt-3 p-3 rounded-xl text-sm text-red-600 bg-red-50';
                msgEl.classList.remove('hidden');
            }
            return;
        }

        // Update the read-only display and switch back to read mode
        _setUniNameDisplay(newName);
        cancelUniversityNameEdit();

        if (msgEl) {
            msgEl.innerText = '✅ University name updated successfully!';
            msgEl.className = 'mt-3 p-3 rounded-xl text-sm text-green-700 bg-green-50';
            msgEl.classList.remove('hidden');
            setTimeout(() => msgEl.classList.add('hidden'), 3000);
        }
    } catch (err) {
        console.error('University identity save error', err);
        if (msgEl) {
            msgEl.innerText = 'Error saving. Please try again.';
            msgEl.className = 'mt-3 p-3 rounded-xl text-sm text-red-600 bg-red-50';
            msgEl.classList.remove('hidden');
        }
    } finally {
        if (saveBtn) saveBtn.disabled = false;
    }
}
// ─────────────────────────────────────────────────────────────────────────────



function getAuthTokenOrNull() {
    return localStorage.getItem('token');
}

async function loadAdminProfile() {
    try {
        const token = getAuthTokenOrNull();
        if (!token) return; // UI will keep defaults; backend would reject anyway

        const response = await authFetch(`${ADMIN_API_BASE_URL}/profile`);
        if (!response.ok) throw new Error('Failed to load admin profile');

        const profile = await response.json();
        DashboardState.adminProfile = profile;
        renderAdminProfile(profile);
    } catch (e) {
        console.error('Admin profile load error', e);
        // Keep existing dashboard UI functional even if profile fails.
    }
}

function renderAdminProfile(profile) {
    const name = profile?.name || '';
    const email = profile?.email || '';
    const phoneNumber = profile?.phoneNumber || '';

    const nameEl = document.getElementById('profile-name');
    if (nameEl) nameEl.value = name;

    const emailEl = document.getElementById('profile-email');
    if (emailEl) emailEl.value = email;

    const phoneEl = document.getElementById('profile-phone');
    if (phoneEl) phoneEl.value = phoneNumber;

    // Replace static header label with backend profile name.
    const desktopLabel = document.getElementById('university-admin-label');
    if (desktopLabel) desktopLabel.innerText = name || 'University Admin';

    const mobileLabel = document.getElementById('mobile-university-admin');
    if (mobileLabel) mobileLabel.innerText = name || 'University Admin';

    // Populate the read-only university name display from DB
    _setUniNameDisplay(profile?.universityName || '');

    // Display Institution Code if available (read-only)
    const codeEl = document.getElementById('uni-code-text');
    if (codeEl) {
        codeEl.innerText = profile?.institutionCode || 'N/A';
    }

    // Load university logo from DB (stored as filename in uploads/logo/)
    if (profile?.universityLogoPath) {
        _showLogoPreview('/uploads/logo/' + profile.universityLogoPath);
    }
}

function setMessage(el, message, type) {
    if (!el) return;
    if (!message) {
        el.innerText = '';
        el.classList.add('hidden');
        el.classList.remove('text-red-600', 'text-green-700', 'bg-red-50', 'bg-green-50');
        return;
    }

    el.classList.remove('hidden');
    el.innerText = message;
    el.classList.remove('text-red-600', 'text-green-700', 'bg-red-50', 'bg-green-50');

    if (type === 'success') {
        el.classList.add('text-green-700', 'bg-green-50');
    } else if (type === 'error') {
        el.classList.add('text-red-600', 'bg-red-50');
    }
}

function setupProfileAndSecurityHandlers() {
    const profileForm = document.getElementById('profile-form');
    if (profileForm) {
        profileForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const nameEl = document.getElementById('profile-name');
            const emailEl = document.getElementById('profile-email');
            const phoneEl = document.getElementById('profile-phone');
            const msgEl = document.getElementById('profile-update-message');

            const name = (nameEl?.value || '').trim();
            const email = (emailEl?.value || '').trim();
            const phoneNumber = (phoneEl?.value || '').trim();

            const token = getAuthTokenOrNull();
            if (!token) {
                setMessage(msgEl, 'You are not authenticated. Please login again.', 'error');
                return;
            }

            try {
                const saveBtn = document.getElementById('save-profile-btn');
                if (saveBtn) saveBtn.disabled = true;
                setMessage(msgEl, '', null); // clear

                const response = await authFetch(`${ADMIN_API_BASE_URL}/update-profile`, {
                    method: 'POST',
                    body: JSON.stringify({
                        name,
                        email,
                        phoneNumber
                    })
                });

                const data = await response.json().catch(() => ({}));
                if (!response.ok) {
                    const err = data?.error || data?.message || 'Failed to update profile';
                    setMessage(msgEl, err, 'error');
                    return;
                }

                const successMsg = data?.message || 'Profile updated successfully';
                setMessage(msgEl, successMsg, 'success');
                await loadAdminProfile(); // refresh header + form values
            } catch (err) {
                setMessage(msgEl, 'Error updating profile. Please try again.', 'error');
                console.error('Profile update error', err);
            } finally {
                const saveBtn = document.getElementById('save-profile-btn');
                if (saveBtn) saveBtn.disabled = false;
            }
        });
    }

    const changePasswordForm = document.getElementById('change-password-form');
    if (changePasswordForm) {
        changePasswordForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const currentPasswordEl = document.getElementById('current-password');
            const newPasswordEl = document.getElementById('new-password');
            const msgEl = document.getElementById('password-update-message');

            const currentPassword = (currentPasswordEl?.value || '').trim();
            const newPassword = (newPasswordEl?.value || '').trim();

            const token = getAuthTokenOrNull();
            if (!token) {
                setMessage(msgEl, 'You are not authenticated. Please login again.', 'error');
                return;
            }

            if (!currentPassword || !newPassword) {
                setMessage(msgEl, 'Please fill both password fields.', 'error');
                return;
            }

            try {
                const updateBtn = changePasswordForm.querySelector('button[type="submit"]');
                if (updateBtn) updateBtn.disabled = true;
                setMessage(msgEl, '', null); // clear

                // Backend expects request params (not JSON) for update-password
                const formBody = new URLSearchParams({
                    currentPassword,
                    newPassword
                }).toString();

                const response = await authFetch(`${ADMIN_API_BASE_URL}/update-password`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: formBody
                });

                const data = await response.json().catch(() => ({}));
                if (!response.ok) {
                    const err = data?.error || data?.message || 'Failed to update password';
                    setMessage(msgEl, err, 'error');
                    return;
                }

                const successMsg = data?.message || 'Password updated successfully';
                setMessage(msgEl, successMsg, 'success');
                currentPasswordEl.value = '';
                newPasswordEl.value = '';
            } catch (err) {
                setMessage(msgEl, 'Error updating password. Please try again.', 'error');
                console.error('Password update error', err);
            } finally {
                const updateBtn = changePasswordForm.querySelector('button[type="submit"]');
                if (updateBtn) updateBtn.disabled = false;
            }
        });
    }
}
