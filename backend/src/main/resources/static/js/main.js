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
    // 0. Redirect to login if no session exists
    if (!localStorage.getItem('token') || localStorage.getItem('token') === 'mock-token-xyz') {
        console.log('🔒 No valid session found. Redirecting to login...');
        localStorage.clear();
        window.location.href = 'index.html';
        return;
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

        const response = await fetch('/api/profile/info', {
            headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
        });
        if (!response.ok) return;

        const profile = await response.json();
        const name = profile.name || 'Student';
        const year = profile.year || '';
        const dept = profile.department || profile.major || '';
        const courseLine = [year, dept].filter(Boolean).join(' - ') || 'Student';
        const avatarUrl = profile.passportPhotoPath
            ? `/uploads/${profile.passportPhotoPath}`
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

        // Force password change on first login
        if (profile.firstLogin === true) {
            showForcePasswordChangeModal();
        }

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

    const token = localStorage.getItem('token') || localStorage.getItem('jwtToken');
    if (!token || token === 'mock-token-xyz') return;

    let studentProfileId = localStorage.getItem('userId');
    try {
        const infoRes = await fetch('/api/profile/info', {
            headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
        });
        if (infoRes.ok) {
            const info = await infoRes.json();
            if (info.id != null) {
                studentProfileId = String(info.id);
                localStorage.setItem('userId', studentProfileId);
            }
        }
    } catch (e) {
        console.warn('Could not resolve student id from profile info:', e);
    }

    if (!studentProfileId) return;

    try {
        let verified = false;

        try {
            const bioResponse = await fetch('/api/student-profile/biometric/status', {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (bioResponse.ok) {
                const bioData = await bioResponse.json();
                if (bioData.success && bioData.enrolled) {
                    verified = true;
                }
            }
        } catch (e) {
            console.warn('Could not fetch biometric status:', e);
        }

        window.dispatchEvent(new CustomEvent('verification-status', { detail: { verified } }));

        if (verified) {
            removeVerificationBanner();
            toggleRegisterButton(false);
            toggleMyExamsAccess(false);
            return;
        }

        removeVerificationBanner();
        toggleRegisterButton(true);
        toggleMyExamsAccess(true);
    } catch (error) {
        console.warn('Verification gate check failed:', error);
    }
}

function showVerificationBanner() {
    // Banner removed as per requirement
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

// First Login Password Change Modal
function showForcePasswordChangeModal() {
    if (document.getElementById('force-password-modal')) return;

    const modalHtml = `
        <div id="force-password-modal" class="fixed inset-0 z-[200] bg-black/80 backdrop-blur-sm flex items-center justify-center">
            <div class="bg-white dark:bg-gray-800 rounded-3xl p-8 max-w-md w-full mx-4 shadow-2xl relative">
                <div class="text-center mb-6">
                    <div class="w-16 h-16 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center text-3xl mx-auto mb-4">
                        <i class="fas fa-key"></i>
                    </div>
                    <h2 class="text-2xl font-bold text-gray-900 dark:text-white">Security Update Required</h2>
                    <p class="text-sm text-gray-500 mt-2">As this is your first time logging in, you must change your default password to continue.</p>
                </div>
                
                <form id="force-password-form" class="space-y-4">
                    <div>
                        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Current Password (PRN)</label>
                        <input type="password" id="force-current-password" required
                            class="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-gray-600 bg-gray-50 dark:bg-gray-700 focus:ring-2 focus:ring-blue-500 outline-none">
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">New Password</label>
                        <input type="password" id="force-new-password" required minlength="6"
                            class="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-gray-600 bg-gray-50 dark:bg-gray-700 focus:ring-2 focus:ring-blue-500 outline-none">
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Confirm New Password</label>
                        <input type="password" id="force-confirm-password" required minlength="6"
                            class="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-gray-600 bg-gray-50 dark:bg-gray-700 focus:ring-2 focus:ring-blue-500 outline-none">
                    </div>
                    
                    <div id="force-password-alert" class="hidden text-sm p-3 rounded-xl font-medium"></div>
                    
                    <button type="submit" id="force-password-submit"
                        class="w-full py-3 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-xl shadow-lg transition-all mt-4">
                        Update Password & Continue
                    </button>
                </form>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHtml);

    document.getElementById('force-password-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const currentPassword = document.getElementById('force-current-password').value;
        const newPassword = document.getElementById('force-new-password').value;
        const confirmPassword = document.getElementById('force-confirm-password').value;
        const alertBox = document.getElementById('force-password-alert');
        const submitBtn = document.getElementById('force-password-submit');

        if (newPassword !== confirmPassword) {
            alertBox.textContent = "New passwords do not match!";
            alertBox.className = "text-sm p-3 rounded-xl font-medium bg-red-50 text-red-600 block";
            return;
        }

        submitBtn.disabled = true;
        submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Updating...';

        try {
            const token = localStorage.getItem('token') || localStorage.getItem('jwtToken');
            const res = await fetch('/api/profile/change-password', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + token
                },
                body: JSON.stringify({ currentPassword, newPassword })
            });

            const data = await res.text();
            
            if (res.ok) {
                alertBox.textContent = "Password updated successfully!";
                alertBox.className = "text-sm p-3 rounded-xl font-medium bg-green-50 text-green-600 block";
                
                // Clear the firstLogin flag via an update API call if necessary, 
                // but usually the backend should clear it upon password change.
                
                setTimeout(() => {
                    // The old token is now invalid (tokenVersion incremented).
                    // Clear session and redirect to login for fresh authentication.
                    localStorage.clear();
                    window.location.href = 'index.html?msg=password_changed';
                }, 1500);
            } else {
                alertBox.textContent = data || "Failed to update password.";
                alertBox.className = "text-sm p-3 rounded-xl font-medium bg-red-50 text-red-600 block";
            }
        } catch (err) {
            alertBox.textContent = "Network error. Try again.";
            alertBox.className = "text-sm p-3 rounded-xl font-medium bg-red-50 text-red-600 block";
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerText = "Update Password & Continue";
        }
    });
}
