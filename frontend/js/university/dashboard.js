// Global Config
const API_BASE_URL = 'http://localhost:8080/api/university';
const UNIVERSITY_ID = 1; // Hardcoded for demo

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
    notifications: []
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

    // Default load
    loadExams();
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

            // Data Load based on tab
            if (targetId === 'exams-section') {
                loadExams();
                updateExamStats(); // Update stats when switching to exams tab
            }
            if (targetId === 'registrations-section') loadRegistrations();
            if (targetId === 'analytics-section') loadAnalytics();
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
        const response = await fetch(`${API_BASE_URL}/stats`);
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
        const response = await fetch(`${API_BASE_URL}/exams/`);
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
