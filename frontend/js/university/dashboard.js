// Global Config
const API_BASE_URL = 'http://localhost:8081/api/university';
const ADMIN_API_BASE_URL = 'http://localhost:8081/api/admin';
const UNIVERSITY_ID = 1; // Hardcoded for demo

// Auth-aware fetch helper – attaches JWT token from localStorage
function authFetch(url, options = {}) {
    const token = localStorage.getItem('token');
    const headers = {
        'Content-Type': 'application/json',
        ...(options.headers || {}),
        ...(token ? { 'Authorization': `Bearer ${token}` } : {})
    };
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
    adminProfile: null,
    communicationPoller: null,
    selectedCommunicationMessage: null
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
    await loadCommunicationInbox();
    startCommunicationPolling();
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
            if (targetId === 'communication-section') loadCommunicationInbox();
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
                    if (data.logoUrl) _showLogoPreview('http://localhost:8081' + data.logoUrl);
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
        _showLogoPreview('http://localhost:8081/uploads/logo/' + profile.universityLogoPath);
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

function unwrapApiResponse(payload) {
    if (payload && typeof payload === 'object' && Object.prototype.hasOwnProperty.call(payload, 'success')) {
        return payload.data;
    }
    return payload;
}

function communicationPriorityBadge(priority) {
    const p = String(priority || 'NORMAL').toUpperCase();
    if (p === 'URGENT') return 'bg-red-100 text-red-700';
    if (p === 'LOW') return 'bg-gray-100 text-gray-700';
    return 'bg-blue-100 text-blue-700';
}

async function loadCommunicationInbox() {
    const tbody = document.getElementById('communication-inbox-body');
    if (!tbody) return;
    tbody.innerHTML = '<tr><td colspan="6" class="px-6 py-4 text-center text-gray-500">Loading inbox...</td></tr>';

    try {
        const response = await authFetch(`${API_BASE_URL}/communication/inbox`);
        const payload = await response.json().catch(() => ({}));
        if (!response.ok || payload.success === false) {
            throw new Error(payload.message || payload.error || 'Failed to fetch communication inbox');
        }

        const messages = unwrapApiResponse(payload) || [];
        if (!messages.length) {
            tbody.innerHTML = '<tr><td colspan="6" class="px-6 py-4 text-center text-gray-500">No communication messages yet.</td></tr>';
            return;
        }

        tbody.innerHTML = '';
        messages.forEach((msg) => {
            const tr = document.createElement('tr');
            tr.className = 'border-b dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700 cursor-pointer';
            const createdAt = msg.createdAt ? new Date(msg.createdAt) : null;
            const status = String(msg.status || 'SENT').toUpperCase();
            const statusClass = status === 'READ' ? 'text-green-600' : 'text-gray-500';
            const msgType = String(msg.type || 'GENERAL').toUpperCase();
            const typeTagClass = msgType === 'WARNING' ? 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400' : msgType === 'BROADCAST' ? 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400' : 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400';
            tr.innerHTML = `
                <td class="px-6 py-4 font-medium text-gray-900 dark:text-gray-100">${msg.subject || '-'}</td>
                <td class="px-6 py-4"><span class="px-2 py-1 rounded text-[10px] font-semibold ${typeTagClass}">${msgType}</span></td>
                <td class="px-6 py-4">SUPER_ADMIN</td>
                <td class="px-6 py-4 text-xs">${createdAt ? createdAt.toLocaleString() : '-'}</td>
                <td class="px-6 py-4"><span class="px-2 py-1 rounded text-[10px] font-semibold ${communicationPriorityBadge(msg.priority)}">${String(msg.priority || 'NORMAL').toUpperCase()}</span></td>
                <td class="px-6 py-4"><span class="${statusClass} text-xs font-semibold">${status}</span></td>
            `;
            tr.addEventListener('click', () => openCommunicationMessage(msg));
            tbody.appendChild(tr);
        });

        // Keep selected message in sync with fresh data
        if (DashboardState.selectedCommunicationMessage?.id) {
            const latest = messages.find(m => m.id === DashboardState.selectedCommunicationMessage.id);
            if (latest) {
                renderCommunicationCard(latest);
            }
        }
    } catch (error) {
        console.error('Communication inbox load error', error);
        tbody.innerHTML = '<tr><td colspan="6" class="px-6 py-4 text-center text-red-500">Failed to load inbox.</td></tr>';
    }
}

async function openCommunicationMessage(message) {
    renderCommunicationCard(message);
    if (String(message.status || '').toUpperCase() !== 'SENT' || !message.id) return;
    try {
        const response = await authFetch(`${API_BASE_URL}/communication/mark-read`, {
            method: 'POST',
            body: JSON.stringify({ id: message.id })
        });
        const payload = await response.json().catch(() => ({}));
        if (!response.ok || payload.success === false) {
            throw new Error(payload.message || 'Failed to mark message as read');
        }
        await loadCommunicationInbox();
    } catch (error) {
        console.error('Mark read failed', error);
    }
}

function statusColor(status) {
    const s = String(status || '').toUpperCase();
    if (s === 'APPROVED') return 'text-green-600';
    if (s === 'REJECTED') return 'text-red-600';
    if (s === 'REQUESTED') return 'text-orange-600';
    if (s === 'READ') return 'text-blue-600';
    return 'text-gray-600';
}

function communicationTypeTagClass(type) {
    const t = String(type || 'GENERAL').toUpperCase();
    if (t === 'WARNING') return 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400';
    if (t === 'BROADCAST') return 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400';
    return 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400';
}

function renderCommunicationCard(message) {
    DashboardState.selectedCommunicationMessage = message;
    const empty = document.getElementById('comm-card-empty');
    const content = document.getElementById('comm-card-content');
    const statusEl = document.getElementById('comm-card-status');
    const typeEl = document.getElementById('comm-card-type');
    const subjectEl = document.getElementById('comm-card-subject');
    const senderEl = document.getElementById('comm-card-sender');
    const dateEl = document.getElementById('comm-card-date');
    const messageEl = document.getElementById('comm-card-message');
    const actionsEl = document.getElementById('comm-card-actions');
    const replyText = document.getElementById('comm-reply-text');

    if (!empty || !content || !statusEl || !subjectEl || !senderEl || !dateEl || !messageEl || !actionsEl || !replyText) return;
    empty.classList.add('hidden');
    content.classList.remove('hidden');

    const status = String(message.status || 'SENT').toUpperCase();
    const msgType = String(message.type || 'GENERAL').toUpperCase();
    statusEl.className = `font-semibold ${statusColor(status)}`;
    statusEl.textContent = status;
    if (typeEl) {
        typeEl.className = `font-semibold px-2 py-0.5 rounded text-xs ${communicationTypeTagClass(msgType)}`;
        typeEl.textContent = msgType;
    }
    subjectEl.textContent = message.subject || '-';
    senderEl.textContent = message.senderRole || 'SUPER_ADMIN';
    dateEl.textContent = message.createdAt ? new Date(message.createdAt).toLocaleString() : '-';
    messageEl.textContent = message.message || '';

    actionsEl.innerHTML = '';
    replyText.classList.add('hidden');
    replyText.value = '';

    // GENERAL: Allow reply, approval workflow applies
    if (msgType === 'GENERAL') {
        if (status === 'READ') {
            const btn = document.createElement('button');
            btn.className = 'px-3 py-2 rounded-lg text-sm bg-orange-100 text-orange-700 hover:bg-orange-200 dark:bg-orange-900/30 dark:text-orange-400';
            btn.textContent = 'Request Reply Permission';
            btn.addEventListener('click', () => requestReplyPermission(message.id));
            actionsEl.appendChild(btn);
        }
        if (status === 'REQUESTED') {
            const hint = document.createElement('span');
            hint.className = 'text-xs text-orange-600 dark:text-orange-400';
            hint.textContent = 'Reply request pending approval.';
            actionsEl.appendChild(hint);
        }
        if (Boolean(message.replyAllowed) || status === 'APPROVED') {
            const replyBtn = document.createElement('button');
            replyBtn.className = 'px-3 py-2 rounded-lg text-sm bg-blue-100 text-blue-700 hover:bg-blue-200 dark:bg-blue-900/30 dark:text-blue-400';
            replyBtn.textContent = 'Reply';
            replyBtn.addEventListener('click', () => {
                replyText.classList.remove('hidden');
                renderReplySendButton(message.id, actionsEl, replyText);
            });
            actionsEl.appendChild(replyBtn);
        }
        if (!Boolean(message.replyAllowed) && status !== 'READ' && status !== 'REQUESTED' && status !== 'APPROVED') {
            const denied = document.createElement('span');
            denied.className = 'text-xs text-gray-500 dark:text-gray-400';
            denied.textContent = 'Reply not allowed.';
            actionsEl.appendChild(denied);
        }
    }

    // WARNING: Acknowledge button only
    if (msgType === 'WARNING') {
        if (!Boolean(message.acknowledged)) {
            const ackBtn = document.createElement('button');
            ackBtn.className = 'px-3 py-2 rounded-lg text-sm bg-red-100 text-red-700 hover:bg-red-200 dark:bg-red-900/30 dark:text-red-400';
            ackBtn.textContent = 'Acknowledge';
            ackBtn.addEventListener('click', () => acknowledgeMessage(message.id));
            actionsEl.appendChild(ackBtn);
        } else {
            const acked = document.createElement('span');
            acked.className = 'text-xs text-green-600 dark:text-green-400 flex items-center gap-1';
            acked.innerHTML = '<i class="fas fa-check-circle"></i> Acknowledged';
            actionsEl.appendChild(acked);
        }
    }

    // BROADCAST: No actions (read-only)

    // Delete button (all message types)
    const deleteBtn = document.createElement('button');
    deleteBtn.className = 'px-3 py-2 rounded-lg text-sm border border-red-200 text-red-600 hover:bg-red-50 dark:border-red-800 dark:text-red-400 dark:hover:bg-red-900/20 ml-auto';
    deleteBtn.innerHTML = '<i class="fas fa-trash mr-1"></i>Delete';
    deleteBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        deleteCommunicationMessage(message.id);
    });
    actionsEl.appendChild(deleteBtn);
}

function renderReplySendButton(messageId, actionsEl, replyTextEl) {
    let sendBtn = actionsEl.querySelector('[data-role="send-reply-btn"]');
    if (sendBtn) return;
    sendBtn = document.createElement('button');
    sendBtn.dataset.role = 'send-reply-btn';
    sendBtn.className = 'px-3 py-2 rounded-lg text-sm bg-green-100 text-green-700 hover:bg-green-200';
    sendBtn.textContent = 'Send Reply';
    sendBtn.addEventListener('click', () => sendReply(messageId, replyTextEl.value));
    actionsEl.appendChild(sendBtn);
}

async function requestReplyPermission(messageId) {
    try {
        const response = await authFetch('http://localhost:8081/api/messages/request-reply', {
            method: 'POST',
            body: JSON.stringify({ id: messageId })
        });
        const payload = await response.json().catch(() => ({}));
        if (!response.ok || payload.success === false) {
            throw new Error(payload.message || 'Failed to request reply permission');
        }
        await loadCommunicationInbox();
    } catch (error) {
        alert(error.message || 'Failed to request reply permission');
    }
}

async function deleteCommunicationMessage(id) {
    if (!confirm('Delete this message?')) return;
    try {
        const response = await authFetch(`${ADMIN_API_BASE_URL}/communication/${id}`, { method: 'DELETE' });
        const payload = await response.json().catch(() => ({}));
        if (!response.ok || payload.success === false) {
            throw new Error(payload.message || 'Failed to delete message');
        }
        DashboardState.selectedCommunicationMessage = null;
        const content = document.getElementById('comm-card-content');
        const empty = document.getElementById('comm-card-empty');
        if (content) content.classList.add('hidden');
        if (empty) empty.classList.remove('hidden');
        await loadCommunicationInbox();
    } catch (error) {
        alert(error.message || 'Failed to delete message');
    }
}

async function acknowledgeMessage(messageId) {
    try {
        const response = await authFetch(`${API_BASE_URL}/communication/acknowledge`, {
            method: 'POST',
            body: JSON.stringify({ id: messageId })
        });
        const payload = await response.json().catch(() => ({}));
        if (!response.ok || payload.success === false) {
            throw new Error(payload.message || 'Failed to acknowledge message');
        }
        await loadCommunicationInbox();
    } catch (error) {
        alert(error.message || 'Failed to acknowledge message');
    }
}

async function sendReply(parentMessageId, messageText) {
    const cleanMessage = (messageText || '').trim();
    if (!cleanMessage) {
        alert('Reply message cannot be empty.');
        return;
    }
    try {
        const response = await authFetch('http://localhost:8081/api/messages/reply', {
            method: 'POST',
            body: JSON.stringify({
                parentMessageId,
                message: cleanMessage
            })
        });
        const payload = await response.json().catch(() => ({}));
        if (!response.ok || payload.success === false) {
            throw new Error(payload.message || 'Reply not allowed');
        }
        await loadCommunicationInbox();
        alert(payload.message || 'Reply sent successfully');
    } catch (error) {
        alert(error.message || 'Reply not allowed');
    }
}

function startCommunicationPolling() {
    if (DashboardState.communicationPoller) {
        clearInterval(DashboardState.communicationPoller);
    }
    DashboardState.communicationPoller = setInterval(() => {
        loadCommunicationInbox();
    }, 10000);
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
