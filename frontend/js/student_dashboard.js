// Student Dashboard Logic

// --- Mock Data ---

const studentProfile = {
    name: "John Student",
    email: "john.student@university.edu",
    phone: "+91 98765 43210",
    course: "B.Tech Computer Science",
    year: "Final Year",
    regNo: "REG2022001",
    avatar: "https://ui-avatars.com/api/?name=John+Student&background=4f46e5&color=fff",
    verificationStatus: "Verified" // Verified, Pending, Rejected
};

const availableExams = [
    { id: 1, title: "Advanced Java Programming", date: "2026-03-10", type: "Semester End", university: "Tech University", college: "Engineering College A" },
    { id: 2, title: "Database Management Systems", date: "2026-03-12", type: "Clas Test", university: "Tech University", college: "Engineering College A" },
    { id: 3, title: "Artificial Intelligence", date: "2026-03-15", type: "Semester End", university: "Tech University", college: "Engineering College A" },
    { id: 4, title: "Web Technologies", date: "2026-03-18", type: "Lab Exam", university: "Tech University", college: "Engineering College A" }
];

const registeredExams = [
    {
        id: 101,
        title: "Software Engineering",
        date: "2026-02-20",
        status: "Approved", // Pending, Approved, Rejected
        steps: {
            uploaded: true,
            aiVerified: true,
            supervisorApproved: true,
            admitCard: true
        }
    },
    {
        id: 102,
        title: "Computer Networks",
        date: "2026-02-25",
        status: "Pending",
        steps: {
            uploaded: true,
            aiVerified: true,
            supervisorApproved: false,
            admitCard: false
        }
    }
];

const notifications = [
    { id: 1, title: "Admit Card Released", message: "Your admit card for Software Engineering is now available.", date: "2 hours ago", unread: true },
    { id: 2, title: "Exam Schedule Update", message: "The AI exam has been rescheduled to March 15th.", date: "1 day ago", unread: false },
    { id: 3, title: "Registration Approved", message: "Your registration for Computer Networks is under review.", date: "2 days ago", unread: false }
];


// --- Initialization ---

// --- Initialization ---

document.addEventListener('DOMContentLoaded', () => {
    initDashboard();
    renderAllSections();
    // Tab Switching
    window.switchTab = function (tabId) {
        // Hide all tabs
        document.querySelectorAll('.tab-section').forEach(el => el.classList.remove('active'));
        // Show target tab
        document.getElementById(tabId)?.classList.add('active');

        // Update nav state
        document.querySelectorAll('.tab-btn').forEach(el => el.classList.remove('active'));
        document.getElementById('nav-' + tabId)?.classList.add('active');

        // Mobile: Close sidebar after click
        const sidebar = document.getElementById('sidebar');
        const overlay = document.getElementById('sidebarOverlay');
        if (sidebar && overlay) {
            sidebar.classList.remove('open');
            overlay.classList.remove('open');
        }
    };

    // Mobile Menu Toggle
    const menuBtn = document.getElementById('menuBtn');
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('sidebarOverlay');

    if (menuBtn && sidebar && overlay) {
        menuBtn.addEventListener('click', () => {
            sidebar.classList.toggle('open');
            overlay.classList.toggle('open');
        });

        overlay.addEventListener('click', () => {
            sidebar.classList.remove('open');
            overlay.classList.remove('open');
        });
    }

    // Exam Filter
    const filterSelect = document.getElementById('examFilter');
    if (filterSelect) {
        filterSelect.addEventListener('change', (e) => {
            renderAvailableExams(availableExams);
        });
    }
});

function initDashboard() {
    // Set User Info
    const sets = (id, val) => { const el = document.getElementById(id); if (el) el.textContent = val; };
    const setSrc = (id, val) => { const el = document.getElementById(id); if (el) el.src = val; };

    sets('headerProfileName', studentProfile.name);
    setSrc('headerProfileImg', studentProfile.avatar);
    setSrc('profileImgLarge', studentProfile.avatar);

    sets('profileName', studentProfile.name);
    sets('profileEmail', studentProfile.email);
    sets('profilePhone', studentProfile.phone);
    sets('profileCourse', studentProfile.course);
    sets('profileYear', studentProfile.year);
    sets('profileRegNo', studentProfile.regNo);

    const badge = document.getElementById('profileStatusBadge');
    if (badge) {
        badge.className = `badge ${getStatusBadgeClass(studentProfile.verificationStatus)}`;
        badge.innerHTML = getStatusIcon(studentProfile.verificationStatus) + " " + studentProfile.verificationStatus;
    }

    // Update Stats
    sets('statRegistered', registeredExams.length);
    sets('statPending', registeredExams.filter(e => e.status === 'Pending').length);
    sets('statApproved', registeredExams.filter(e => e.status === 'Approved').length);
    sets('statNotifications', notifications.filter(n => n.unread).length);
}

function renderAllSections() {
    renderAvailableExams(availableExams);
    renderRegisteredExams();
    renderNotifications();
}

// --- Render Functions ---

function renderAvailableExams(exams) {
    const container = document.getElementById('availableExamsList');
    if (!container) return;
    container.innerHTML = '';

    exams.forEach(exam => {
        const card = document.createElement('div');
        card.className = 'bg-white p-6 rounded-xl border border-gray-100 shadow-sm hover:shadow-md transition-all';
        card.innerHTML = `
            <div class="flex justify-between items-start mb-4">
                <div>
                    <h3 class="font-bold text-lg text-gray-800">${exam.title}</h3>
                    <p class="text-sm text-gray-500">${exam.type} • ${exam.college}</p>
                </div>
                <div class="bg-blue-50 text-blue-700 px-3 py-1 rounded-lg text-xs font-semibold">
                    ${new Date(exam.date).toLocaleDateString('en-GB', { day: 'numeric', month: 'short' })}
                </div>
            </div>
            <div class="flex justify-between items-center mt-4">
                <span class="text-xs text-gray-400 font-mono">ID: EX-${exam.id}</span>
                <button class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors shadow-sm" onclick="registerExam(${exam.id})">
                    Register Now
                </button>
            </div>
        `;
        container.appendChild(card);
    });
}

function renderRegisteredExams() {
    const container = document.getElementById('registeredExamsList');
    if (!container) return;
    container.innerHTML = '';

    registeredExams.forEach(exam => {
        const isApproved = exam.status === 'Approved';
        const card = document.createElement('div');
        card.className = 'bg-white p-6 rounded-xl border border-gray-200 shadow-sm mb-4';

        // Progress Steps HTML - Styled for horizontal layout
        const stepsHtml = `
            <div class="flex justify-between items-center mt-6 relative">
                 <!-- Progress Line for visual connection would go here via CSS or absolute divs -->
                <div class="flex flex-col items-center z-10 w-1/4">
                    <div class="w-8 h-8 rounded-full flex items-center justify-center mb-2 ${exam.steps.uploaded ? 'bg-green-100 text-green-600' : 'bg-gray-100 text-gray-400'}">
                        <i class="fas fa-upload text-sm"></i>
                    </div>
                    <span class="text-xs font-semibold ${exam.steps.uploaded ? 'text-green-700' : 'text-gray-400'}">Uploaded</span>
                </div>
                <div class="flex flex-col items-center z-10 w-1/4">
                    <div class="w-8 h-8 rounded-full flex items-center justify-center mb-2 ${exam.steps.aiVerified ? 'bg-green-100 text-green-600' : 'bg-gray-100 text-gray-400'}">
                        <i class="fas fa-robot text-sm"></i>
                    </div>
                    <span class="text-xs font-semibold ${exam.steps.aiVerified ? 'text-green-700' : 'text-gray-400'}">AI Check</span>
                </div>
                <div class="flex flex-col items-center z-10 w-1/4">
                    <div class="w-8 h-8 rounded-full flex items-center justify-center mb-2 ${exam.steps.supervisorApproved ? 'bg-green-100 text-green-600' : 'bg-gray-100 text-gray-400'}">
                        <i class="fas fa-user-check text-sm"></i>
                    </div>
                    <span class="text-xs font-semibold ${exam.steps.supervisorApproved ? 'text-green-700' : 'text-gray-400'}">Supervisor</span>
                </div>
                <div class="flex flex-col items-center z-10 w-1/4">
                    <div class="w-8 h-8 rounded-full flex items-center justify-center mb-2 ${exam.steps.admitCard ? 'bg-green-100 text-green-600' : 'bg-gray-100 text-gray-400'}">
                        <i class="fas fa-id-card text-sm"></i>
                    </div>
                    <span class="text-xs font-semibold ${exam.steps.admitCard ? 'text-green-700' : 'text-gray-400'}">Admit Card</span>
                </div>
            </div>
        `;

        card.innerHTML = `
            <div class="flex justify-between items-center pb-4 border-b border-gray-100">
                <div>
                    <h3 class="font-bold text-lg text-gray-800">${exam.title}</h3>
                    <p class="text-xs text-gray-500 mt-1"><i class="far fa-calendar-alt mr-1"></i> ${exam.date}</p>
                </div>
                <span class="badge ${getStatusBadgeClass(exam.status)}">
                    ${getStatusIcon(exam.status)} ${exam.status}
                </span>
            </div>
            
            ${stepsHtml}
            
            <div class="mt-6 pt-2 flex justify-end">
                ${isApproved ? `
                    <button class="bg-teal-600 hover:bg-teal-700 text-white px-5 py-2 rounded-lg text-sm font-medium shadow-sm transition-all flex items-center" onclick="downloadAdmitCard(${exam.id})">
                        <i class="fas fa-download mr-2"></i> Download Admit Card
                    </button>
                ` : `
                    <button class="text-gray-400 cursor-not-allowed px-4 py-2 text-sm bg-gray-50 rounded" disabled>
                        <i class="fas fa-hourglass-half mr-1"></i> Processing...
                    </button>
                `}
            </div>
        `;
        container.appendChild(card);
    });
}

function renderNotifications() {
    const container = document.getElementById('notificationsList');
    if (!container) return;
    container.innerHTML = '';

    notifications.forEach(notif => {
        const item = document.createElement('div');
        item.className = `p-4 rounded-lg border ${notif.unread ? 'bg-blue-50 border-blue-100' : 'bg-white border-gray-100'} mb-3 transition-colors flex gap-4 items-start`;
        item.innerHTML = `
            <div class="mt-1 ${notif.unread ? 'text-blue-600' : 'text-gray-400'}">
                <i class="fas ${notif.unread ? 'fa-envelope' : 'fa-envelope-open'} text-lg"></i>
            </div>
            <div class="flex-1">
                <div class="flex justify-between items-start">
                    <h4 class="font-semibold text-gray-800 text-sm">${notif.title}</h4>
                    <span class="text-xs text-gray-400 whitespace-nowrap ml-2">${notif.date}</span>
                </div>
                <p class="text-sm text-gray-600 mt-1">${notif.message}</p>
            </div>
        `;
        container.appendChild(item);
    });
}

function getInitials(name) {
    if (!name) return 'ST';
    return name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
}

function getStatusBadgeClass(status) {
    switch (status) {
        case 'Approved': case 'Verified': return 'badge-approved';
        case 'Rejected': return 'badge-rejected';
        default: return 'badge-pending';
    }
}

function getStatusIcon(status) {
    switch (status) {
        case 'Approved': case 'Verified': return '<i class="fas fa-check-circle"></i>';
        case 'Rejected': return '<i class="fas fa-times-circle"></i>';
        default: return '<i class="fas fa-clock"></i>';
    }
}

// ========== ELIGIBILITY CHECK & REGISTRATION ==========

async function registerExam(examId) {
    console.log('[REGISTRATION] Starting registration for exam ID:', examId);

    const exam = availableExams.find(e => e.id === examId);
    if (!exam) {
        console.error('[REGISTRATION] Exam not found:', examId);
        showEligibilityModal('Error', 'Exam not found. Please refresh the page.', 'error');
        return;
    }

    console.log('[REGISTRATION] Exam details:', exam);

    // Get JWT token from localStorage
    const token = localStorage.getItem('token') || localStorage.getItem('jwtToken');
    console.log('[REGISTRATION] Token found:', token ? 'Yes (length: ' + token.length + ')' : 'No');

    if (!token) {
        showEligibilityModal('Authentication Required', 'Please login to register for exams.', 'error');
        return;
    }

    // Show loading state
    showEligibilityModal('Checking Eligibility...', 'Please wait while we verify your eligibility.', 'loading');

    try {
        // Get PRN from student profile
        const prn = studentProfile.regNo;
        // Build exam session string (should match database format exactly)
        const examSession = exam.type + ' ' + new Date(exam.date).getFullYear();

        console.log('[REGISTRATION] PRN:', prn);
        console.log('[REGISTRATION] Exam Session:', examSession);

        if (!prn) {
            console.error('[REGISTRATION] PRN is missing from student profile');
            showEligibilityModal('PRN Required', 'Please update your profile with your PRN number.', 'error');
            return;
        }

        // Build eligibility check URL
        const eligibilityUrl = `http://localhost:8081/api/student/check-exam-eligibility?prnNumber=${encodeURIComponent(prn)}&examSession=${encodeURIComponent(examSession)}`;
        console.log('[REGISTRATION] Calling eligibility API:', eligibilityUrl);

        // Call eligibility API
        const response = await fetch(eligibilityUrl, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        console.log('[REGISTRATION] Eligibility API response status:', response.status);

        if (!response.ok) {
            const errorText = await response.text();
            console.error('[REGISTRATION] Eligibility check failed:', response.status, errorText);
            throw new Error(`HTTP ${response.status}: ${errorText || response.statusText}`);
        }

        const eligibilityData = await response.json();
        console.log('[REGISTRATION] Eligibility response:', eligibilityData);

        if (eligibilityData.eligible) {
            console.log('[REGISTRATION] Student is eligible - showing registration form');
            // Student is eligible - show registration form
            closeEligibilityModal();
            showRegistrationForm(exam, examSession);
        } else {
            console.warn('[REGISTRATION] Student is NOT eligible');
            // Student is NOT eligible - show error
            showEligibilityModal(
                'Not Eligible',
                eligibilityData.message || 'You are not eligible for this exam session.',
                'error'
            );
        }

    } catch (error) {
        console.error('[REGISTRATION] Eligibility check error:', error);
        showEligibilityModal(
            'Error Checking Eligibility',
            'Failed to verify eligibility: ' + error.message + '. Please try again later.',
            'error'
        );
    }
}

function showEligibilityModal(title, message, type = 'info') {
    // Create modal if it doesn't exist
    let modal = document.getElementById('eligibilityModal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'eligibilityModal';
        modal.className = 'fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 hidden';
        modal.innerHTML = `
            <div class="bg-white rounded-xl shadow-2xl max-w-md w-full mx-4 p-6">
                <div class="text-center">
                    <div id="modalIcon" class="mx-auto mb-4"></div>
                    <h3 id="modalTitle" class="text-xl font-bold text-gray-800 mb-2"></h3>
                    <p id="modalMessage" class="text-gray-600 mb-6"></p>
                    <button id="modalCloseBtn" class="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded-lg font-medium transition-colors">
                        Close
                    </button>
                </div>
            </div>
        `;
        document.body.appendChild(modal);

        // Add close handler
        document.getElementById('modalCloseBtn').addEventListener('click', closeEligibilityModal);
        modal.addEventListener('click', (e) => {
            if (e.target === modal) closeEligibilityModal();
        });
    }

    // Update content
    const iconContainer = document.getElementById('modalIcon');
    const titleEl = document.getElementById('modalTitle');
    const messageEl = document.getElementById('modalMessage');
    const closeBtn = document.getElementById('modalCloseBtn');

    titleEl.textContent = title;
    messageEl.textContent = message;

    // Set icon based on type
    if (type === 'error') {
        iconContainer.innerHTML = '<div class="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto"><i class="fas fa-times-circle text-4xl text-red-600"></i></div>';
        closeBtn.className = 'bg-red-600 hover:bg-red-700 text-white px-6 py-2 rounded-lg font-medium transition-colors';
    } else if (type === 'success') {
        iconContainer.innerHTML = '<div class="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto"><i class="fas fa-check-circle text-4xl text-green-600"></i></div>';
        closeBtn.className = 'bg-green-600 hover:bg-green-700 text-white px-6 py-2 rounded-lg font-medium transition-colors';
    } else if (type === 'loading') {
        iconContainer.innerHTML = '<div class="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto"><i class="fas fa-spinner fa-spin text-4xl text-blue-600"></i></div>';
        closeBtn.style.display = 'none';
    } else {
        iconContainer.innerHTML = '<div class="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto"><i class="fas fa-info-circle text-4xl text-blue-600"></i></div>';
        closeBtn.className = 'bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded-lg font-medium transition-colors';
        closeBtn.style.display = 'block';
    }

    // Show modal
    modal.classList.remove('hidden');
}

function closeEligibilityModal() {
    const modal = document.getElementById('eligibilityModal');
    if (modal) {
        modal.classList.add('hidden');
    }
}

function showRegistrationForm(exam, examSession) {
    console.log('[FORM] Showing registration form for exam:', exam.title);
    console.log('[FORM] Exam session:', examSession);

    // Create registration form modal
    let formModal = document.getElementById('registrationFormModal');
    if (!formModal) {
        formModal = document.createElement('div');
        formModal.id = 'registrationFormModal';
        formModal.className = 'fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 hidden overflow-y-auto';
        formModal.innerHTML = `
            <div class="bg-white rounded-xl shadow-2xl max-w-2xl w-full mx-4 my-8 p-8">
                <div class="flex justify-between items-center mb-6">
                    <h2 class="text-2xl font-bold text-gray-800">Exam Registration</h2>
                    <button onclick="closeRegistrationForm()" class="text-gray-400 hover:text-gray-600">
                        <i class="fas fa-times text-2xl"></i>
                    </button>
                </div>
                
                <div id="examDetailsSection" class="bg-blue-50 p-4 rounded-lg mb-6"></div>
                
                <form id="examRegistrationForm" class="space-y-4">
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-2">PRN Number</label>
                        <input type="text" id="regPrn" value="${studentProfile.regNo}" readonly
                            class="w-full px-4 py-2 border border-gray-300 rounded-lg bg-gray-50 text-gray-600">
                    </div>
                    
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-2">Full Name</label>
                        <input type="text" id="regFullName" value="${studentProfile.name}" required
                            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent">
                    </div>
                    
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-2">Course</label>
                        <input type="text" id="regCourse" value="${studentProfile.course}" required
                            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent">
                    </div>
                    
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-2">Year</label>
                        <input type="text" id="regYear" value="${studentProfile.year}" required
                            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent">
                    </div>
                    
                    <div class="flex gap-3 mt-6">
                        <button type="submit" class="flex-1 bg-blue-600 hover:bg-blue-700 text-white py-3 rounded-lg font-medium transition-colors">
                            <i class="fas fa-check mr-2"></i> Submit Registration
                        </button>
                        <button type="button" onclick="closeRegistrationForm()" 
                            class="px-6 bg-gray-200 hover:bg-gray-300 text-gray-700 py-3 rounded-lg font-medium transition-colors">
                            Cancel
                        </button>
                    </div>
                </form>
            </div>
        `;
        document.body.appendChild(formModal);

        // Add form submit handler
        document.getElementById('examRegistrationForm').addEventListener('submit', handleRegistrationSubmit);
    }

    // Update exam details
    const detailsSection = document.getElementById('examDetailsSection');
    detailsSection.innerHTML = `
        <div class="flex justify-between items-center">
            <div>
                <h3 class="font-bold text-lg text-gray-800">${exam.title}</h3>
                <p class="text-sm text-gray-600 mt-1">
                    <i class="far fa-calendar-alt mr-1"></i> ${exam.date} • ${exam.type}
                </p>
                <p class="text-xs text-gray-500 mt-1">Session: ${examSession}</p>
            </div>
            <span class="bg-green-100 text-green-700 px-3 py-1 rounded-lg text-xs font-semibold">
                <i class="fas fa-check-circle mr-1"></i> Eligible
            </span>
        </div>
    `;

    // Store exam ID and session for submission
    formModal.dataset.examId = exam.id;
    formModal.dataset.examSession = examSession;

    // Show modal
    formModal.classList.remove('hidden');
}

function closeRegistrationForm() {
    const modal = document.getElementById('registrationFormModal');
    if (modal) {
        modal.classList.add('hidden');
    }
}

async function handleRegistrationSubmit(e) {
    e.preventDefault();

    const formModal = document.getElementById('registrationFormModal');
    const examId = formModal?.dataset.examId;

    const formData = {
        examId: parseInt(examId),
        prn: document.getElementById('regPrn').value,
        fullName: document.getElementById('regFullName').value,
        course: document.getElementById('regCourse').value,
        year: document.getElementById('regYear').value,
        examSession: 'Semester End 2026' // Should be dynamic based on exam
    };

    const token = localStorage.getItem('token') || localStorage.getItem('jwtToken');

    try {
        const response = await fetch('http://localhost:8081/api/student/registrations', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        });

        if (response.ok) {
            closeRegistrationForm();
            showEligibilityModal('Success', 'Your exam registration has been submitted successfully!', 'success');

            // Refresh registered exams list after 2 seconds
            setTimeout(() => {
                closeEligibilityModal();
                // In a real app, you'd fetch updated data from the server
                renderRegisteredExams();
            }, 2000);
        } else {
            const error = await response.text();
            showEligibilityModal('Registration Failed', error || 'Failed to submit registration. Please try again.', 'error');
        }
    } catch (error) {
        console.error('Registration error:', error);
        showEligibilityModal('Error', 'Failed to submit registration. Please try again later.', 'error');
    }
}

// ========== END ELIGIBILITY CHECK & REGISTRATION ==========

function downloadAdmitCard(id) {
    alert(`Downloading Admit Card for Exam ID: ${id}`);
}

function logout() {
    if (confirm('Are you sure you want to logout?')) {
        window.location.href = 'index.html';
    }
}

function toggleProfileMenu() {
    // Basic toggle for profile menu if implemented
    alert("Profile menu toggle");
}
