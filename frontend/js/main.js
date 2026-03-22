import { ThemeManager } from './utils/theme.js';
import { Sidebar } from './components/Sidebar.js';
import { Navbar } from './components/Navbar.js';
import { DashboardHome } from './pages/DashboardHome.js';
import MyRegistrations from './pages/MyRegistrations.js';
import { Profile } from './pages/Profile.js';
import { Notifications } from './pages/Notifications.js';
import { StudentFaceVerification } from './pages/StudentFaceVerification.js';
import { StudentBiometricVerification } from './pages/StudentBiometricVerification.js';
import CreateExam from './pages/CreateExam.js';
import ExamRegistration from './pages/ExamRegistration.js';

document.addEventListener('DOMContentLoaded', () => {
    // 0. Set up mock student session for testing (if no session exists)
    if (!localStorage.getItem('token')) {
        console.log('🔧 Initializing MOCK student session for testing...');
        localStorage.setItem('userId', 'S12345');
        localStorage.setItem('role', 'STUDENT');
        localStorage.setItem('token', 'mock-token-xyz');
        localStorage.setItem('userProfile', JSON.stringify({
            name: 'Demo Student',
            email: 'demo@student.com',
            rollNumber: 'S12345',
            course: 'B.Tech',
            year: '2026'
        }));
        console.log('✅ Mock session created:', {
            userId: 'S12345',
            role: 'STUDENT',
            token: 'mock-token-xyz'
        });
    }

    // 1. Initialize Theme
    ThemeManager.init();

    // 2. Render Core Layout
    const app = document.getElementById('app');

    app.innerHTML = `
        <div class="flex h-screen bg-gray-50 dark:bg-gray-900 transition-colors duration-300">
            <aside id="sidebar-container" class="fixed inset-y-0 left-0 z-50 w-64 bg-white dark:bg-gray-800 shadow-lg transform -translate-x-full lg:translate-x-0 transition-transform duration-300 ease-in-out">
                <!-- Sidebar Component Injected Here -->
            </aside>
            
            <div class="flex-1 flex flex-col lg:ml-64 overflow-hidden relative"> 
                <!-- Overlay for mobile sidebar -->
                <div id="sidebar-overlay" class="fixed inset-0 bg-black bg-opacity-50 z-40 hidden lg:hidden glass-overlay"></div>

                <header id="navbar-container" class="h-16 bg-white/80 dark:bg-gray-800/80 backdrop-blur-md shadow-sm z-30 sticky top-0 border-b border-gray-200 dark:border-gray-700">
                     <!-- Navbar Component Injected Here -->
                </header>

                <main id="main-content" class="flex-1 overflow-x-hidden overflow-y-auto bg-gray-50 dark:bg-gray-900 p-6 scroll-smooth">
                    <!-- Dynamic Page Content Injected Here -->
                    <div id="page-container" class="max-w-7xl mx-auto">
                        <!-- Default to Dashboard Content for now (Loading State) -->
                        <div class="flex items-center justify-center h-64">
                            <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
                        </div>
                    </div>
                </main>
            </div>
        </div>
    `;

    // 3. Initialize Components
    Sidebar.render('sidebar-container');
    Navbar.render('navbar-container');

    // 3b. Load real user profile and inject into sidebar
    loadAndInjectUserProfile();

    // 4. Setup Global Event Listeners
    setupGlobalEvents();

    // 5. Initial Router Load
    loadPage('dashboard');

    // Listen for navigation events
    window.addEventListener('navigate', (e) => {
        loadPage(e.detail.page);
    });
});

async function loadPage(pageId) {
    const container = document.getElementById('page-container');

    // Clear previous content to prevent memory leaks if components had listeners (basic)
    container.innerHTML = '';

    const routes = {
        'dashboard': DashboardHome,
        // Override the 'exams' string key so the sidebar immediately loads the new module
        'exams': ExamRegistration,
        'exam-registration': ExamRegistration, // Fallback if data-page is updated
        'create-exam': CreateExam,             // For University "Create Exam"
        'registered': MyRegistrations,
        'profile': Profile,
        'notifications': Notifications,
        'face-verification': StudentFaceVerification,
        'biometric-verification': StudentBiometricVerification
    };

    const component = routes[pageId];

    if (component) {
        // Pre-load content for pages with init method
        if (component.init) {
            await component.init();
        }

        // Handle string-returning render() pattern vs function-execution pattern
        if (typeof component.render === 'function') {
            container.innerHTML = component.render();
            if (component.afterRender) component.afterRender();
        } else if (typeof component === 'function') {
            // WORKAROUND: The new page modules target document.getElementById('app') directly.
            // Executing them "as is" would overwrite the entire SPA sidebar/navbar.
            // We temporarily mock document.getElementById to redirect 'app' to 'page-container'.

            const originalGetElementById = document.getElementById;
            document.getElementById = function (id) {
                if (id === 'app') {
                    return originalGetElementById.call(document, 'page-container');
                }
                return originalGetElementById.call(document, id);
            };

            // Execute the new component
            component();

            // Restore the original function
            document.getElementById = originalGetElementById;

            if (component.afterRender) component.afterRender();
        }

        if (pageId === 'dashboard') {
            await enforceStudentVerificationGate();
        }
    } else {
        // Fallback for missing pages
        container.innerHTML = `
            <div class="flex flex-col items-center justify-center py-20 animate-fade-in-up">
                <div class="w-16 h-16 bg-indigo-50 dark:bg-indigo-900/20 rounded-full flex items-center justify-center mb-4">
                     <i class="fas fa-code text-2xl text-indigo-500"></i>
                </div>
                <h2 class="text-2xl font-bold text-gray-800 dark:text-white">Page Under Construction</h2>
                <p class="text-gray-500 dark:text-gray-400 mt-2">The '${pageId}' module is coming soon.</p>
                <button onclick="window.dispatchEvent(new CustomEvent('navigate', { detail: { page: 'dashboard' } }))" class="mt-6 text-indigo-600 font-medium hover:underline">Go Back Home</button>
            </div>
        `;
    }
}

async function loadAndInjectUserProfile() {
    try {
        const token = localStorage.getItem('token') || localStorage.getItem('jwtToken');
        if (!token || token === 'mock-token-xyz') return;

        const response = await fetch('http://localhost:8080/api/profile/info', {
            headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
        });
        if (!response.ok) return;

        const profile = await response.json();
        const name = profile.name || 'Student';
        const year = profile.year || '';
        const dept = profile.department || profile.major || '';
        const courseLine = [year, dept].filter(Boolean).join(' - ') || 'Student';
        const avatarUrl = profile.passportPhotoPath
            ? `http://localhost:8080/uploads/${profile.passportPhotoPath}`
            : `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&background=4f46e5&color=fff`;

        // Store globally for DashboardHome welcome message
        window.currentStudentName = name;

        // Update sidebar elements
        const nameEl = document.getElementById('sidebar-user-name');
        const courseEl = document.getElementById('sidebar-user-course');
        const avatarEl = document.getElementById('sidebar-user-avatar');
        if (nameEl) nameEl.textContent = name;
        if (courseEl) courseEl.textContent = courseLine;
        if (avatarEl) avatarEl.src = avatarUrl;

    } catch (error) {
        console.warn('Could not load user profile for sidebar:', error);
    }
}

function setupGlobalEvents() {
    // Mobile Sidebar Logic
    const sidebar = document.getElementById('sidebar-container');
    const overlay = document.getElementById('sidebar-overlay');

    window.toggleSidebar = () => {
        sidebar.classList.toggle('-translate-x-full');
        overlay.classList.toggle('hidden');
    };

    overlay.addEventListener('click', () => {
        sidebar.classList.add('-translate-x-full');
        overlay.classList.add('hidden');
    });

    // Listen for Theme Toggle Custom Event
    window.addEventListener('theme-toggle', () => {
        ThemeManager.toggle();
    });
}

async function enforceStudentVerificationGate() {
    const role = (localStorage.getItem('role') || '').toUpperCase();
    if (role !== 'STUDENT') return;

    const userId = localStorage.getItem('userId');
    if (!userId) return;

    const token = localStorage.getItem('token') || localStorage.getItem('jwtToken');
    const headers = token ? { 'Authorization': `Bearer ${token}` } : {};

    try {
        const response = await fetch(`http://localhost:8080/api/student-profile/${userId}`, { headers });
        if (!response.ok) return;

        const profile = await response.json();
        const verified = !!profile.verified;
        const locked = !!profile.profileLocked;

        if (verified && locked) {
            removeVerificationBanner();
            toggleRegisterButton(false);
            toggleMyExamsAccess(false);
            return;
        }

        showVerificationBanner();
        toggleRegisterButton(true);
        toggleMyExamsAccess(true);
    } catch (error) {
        console.warn('Verification gate check failed:', error);
    }
}

function showVerificationBanner() {
    const container = document.getElementById('page-container');
    if (!container || document.getElementById('verification-warning-banner')) return;

    const banner = document.createElement('div');
    banner.id = 'verification-warning-banner';
    banner.className = 'mb-6 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-amber-800';
    banner.textContent = 'Please complete face verification before proceeding.';
    container.prepend(banner);
}

function removeVerificationBanner() {
    const banner = document.getElementById('verification-warning-banner');
    if (banner) banner.remove();
}

function toggleRegisterButton(disabled) {
    const container = document.getElementById('page-container');
    if (!container) return;

    const buttons = Array.from(container.querySelectorAll('button'));
    const registerButton = buttons.find(btn => btn.textContent.trim().includes('Register New'));
    if (!registerButton) return;

    registerButton.disabled = disabled;
    registerButton.classList.toggle('opacity-60', disabled);
    registerButton.classList.toggle('cursor-not-allowed', disabled);
}

function toggleMyExamsAccess(disabled) {
    const navItem = document.querySelector('.nav-item[data-page="registered"]');
    if (!navItem) return;

    if (disabled) {
        navItem.classList.add('pointer-events-none', 'opacity-50');
    } else {
        navItem.classList.remove('pointer-events-none', 'opacity-50');
    }
}
