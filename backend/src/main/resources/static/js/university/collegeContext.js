// collegeContext.js — Global College Context State Module
// Provides a shared selectedCollegeId/Name that drives filtering across
// the Students and Staff tabs. Fires a 'collegeContextChanged' CustomEvent
// on the document whenever the selection changes.

window.CollegeContext = {
    selectedCollegeId: null,
    selectedCollegeName: null,
    colleges: []
};

/**
 * Load colleges from the API and populate the global context bar dropdown.
 * Called once on dashboard init.
 */
window.initCollegeContext = async function () {
    try {
        const response = await authFetch(`${ADMIN_API_BASE_URL}/colleges`);
        if (!response.ok) throw new Error('Failed to load colleges');

        window.CollegeContext.colleges = await response.json();
        _renderContextBarOptions(window.CollegeContext.colleges);

    } catch (e) {
        console.error('[CollegeContext] Failed to load colleges:', e);
        const sel = document.getElementById('global-college-select');
        if (sel) {
            sel.innerHTML = '<option value="">⚠ Failed to load colleges</option>';
        }
    }
};

/**
 * Set the active college. Broadcasts 'collegeContextChanged' so listeners
 * (students.js, staff.js) can react and reload their data.
 *
 * @param {string|number|null} id   College ID (or null to clear)
 * @param {string|null}        name College display name
 */
window.setSelectedCollege = function (id, name) {
    window.CollegeContext.selectedCollegeId = id ? Number(id) : null;
    window.CollegeContext.selectedCollegeName = name || null;

    // Update context badge
    _updateContextBadge();

    // Toggle action buttons
    _toggleActionButtons(!!id);

    // Broadcast change — listeners in students.js and staff.js will react
    document.dispatchEvent(new CustomEvent('collegeContextChanged', {
        detail: {
            collegeId: window.CollegeContext.selectedCollegeId,
            collegeName: window.CollegeContext.selectedCollegeName
        }
    }));
};

// ─── Private Helpers ──────────────────────────────────────────────────────────

function _renderContextBarOptions(colleges) {
    const sel = document.getElementById('global-college-select');
    if (!sel) return;

    let html = '<option value="">— Select a College —</option>';
    colleges.forEach(c => {
        html += `<option value="${c.id}">${c.name}${c.code ? ' (' + c.code + ')' : ''}</option>`;
    });
    sel.innerHTML = html;

    // Restore previous selection from sessionStorage (survives tab switches, not page reload)
    const savedId = sessionStorage.getItem('selectedCollegeId');
    const savedName = sessionStorage.getItem('selectedCollegeName');
    if (savedId) {
        sel.value = savedId;
        // Only restore if the option actually exists
        if (sel.value === savedId) {
            window.setSelectedCollege(savedId, savedName);
            return;
        }
    }
    // No saved selection — show placeholder state
    _updateContextBadge();
    _toggleActionButtons(false);
}

function _updateContextBadge() {
    const badge = document.getElementById('college-context-badge');
    const badgeText = document.getElementById('college-context-badge-text');
    const noSelectMsg = document.getElementById('college-context-hint');
    if (!badge) return;

    if (window.CollegeContext.selectedCollegeName) {
        if (badgeText) badgeText.textContent = window.CollegeContext.selectedCollegeName;
        badge.classList.remove('hidden');
        if (noSelectMsg) noSelectMsg.classList.add('hidden');

        // Persist to sessionStorage
        sessionStorage.setItem('selectedCollegeId', window.CollegeContext.selectedCollegeId);
        sessionStorage.setItem('selectedCollegeName', window.CollegeContext.selectedCollegeName);
    } else {
        badge.classList.add('hidden');
        if (noSelectMsg) noSelectMsg.classList.remove('hidden');

        sessionStorage.removeItem('selectedCollegeId');
        sessionStorage.removeItem('selectedCollegeName');
    }
}

function _toggleActionButtons(enabled) {
    // Student section
    const addStudentBtn = document.getElementById('btn-add-student');
    const uploadStudentCsvBtn = document.getElementById('btn-student-csv');
    // Staff section
    const addStaffBtn = document.getElementById('btn-add-staff');
    const uploadStaffCsvBtn = document.getElementById('btn-staff-csv');

    [addStudentBtn, uploadStudentCsvBtn, addStaffBtn, uploadStaffCsvBtn].forEach(btn => {
        if (!btn) return;
        btn.disabled = !enabled;
        if (enabled) {
            btn.classList.remove('opacity-50', 'cursor-not-allowed');
        } else {
            btn.classList.add('opacity-50', 'cursor-not-allowed');
        }
    });
}
