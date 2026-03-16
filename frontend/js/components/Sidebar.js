export const Sidebar = {
    render(targetId) {
        const container = document.getElementById(targetId);
        if (!container) return;

        container.innerHTML = `
            <div class="flex flex-col h-full border-r border-gray-200 dark:border-gray-700">
                <!-- Logo Area -->
                <div class="h-16 flex items-center px-6 border-b border-gray-200 dark:border-gray-700">
                    <div class="flex items-center gap-3">
                        <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white font-bold shadow-lg shadow-indigo-500/30">
                            EH
                        </div>
                        <span class="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-gray-900 to-gray-600 dark:from-white dark:to-gray-300">
                            ExamHub
                        </span>
                    </div>
                </div>

                <!-- Navigation -->
                <nav class="flex-1 px-3 py-6 space-y-1 overflow-y-auto custom-scrollbar">
                    ${this.renderNavItem('dashboard', 'Dashboard', 'fas fa-th-large', true)}
                    ${this.renderNavItem('exams', 'Available Exams', 'fas fa-globe')}
                    ${this.renderNavItem('registered', 'My Exams', 'fas fa-file-signature')}
                    ${this.renderNavItem('profile', 'Profile', 'fas fa-user-circle')}
                    ${this.renderNavItem('face-verification', 'Face Verification', 'fas fa-user-check')}
                    ${this.renderNavItem('notifications', 'Notifications', 'fas fa-bell', false, 0, true)}
                    
                    <div class="pt-4 mt-4 border-t border-gray-200 dark:border-gray-700">
                        <span class="px-4 text-xs font-semibold text-gray-400 uppercase tracking-wider">Account</span>
                        <div class="mt-2 text-red-500"> <!-- Red override for Logout -->
                             ${this.renderNavItem('logout', 'Logout', 'fas fa-sign-out-alt')}
                        </div>
                    </div>
                </nav>

                <!-- User Mini Profile (Bottom) -->
                <div class="p-4 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50">
                    <div class="flex items-center gap-3">
                        <img src="https://ui-avatars.com/api/?name=John+Doe&background=random" class="w-9 h-9 rounded-full border-2 border-white dark:border-gray-600 shadow-sm">
                        <div class="flex-1 min-w-0">
                            <p class="text-sm font-medium text-gray-900 dark:text-white truncate">John Student</p>
                            <p class="text-xs text-gray-500 dark:text-gray-400 truncate">Final Year - CS</p>
                        </div>
                    </div>
                </div>
            </div>
        `;

        this.attachListeners();
    },

    renderNavItem(id, label, icon, active = false, badge = 0, dynamic = false) {
        // Note: Used 'group' class for hover effects on children
        let badgeHtml = '';
        if (dynamic) {
            // Dynamic badge — starts hidden, updated by Notifications.js
            badgeHtml = `<span class="notif-sidebar-badge inline-flex items-center justify-center px-2 py-0.5 text-xs font-bold leading-none text-red-100 bg-red-500 rounded-full shadow-sm hidden">0</span>`;
        } else if (badge > 0) {
            badgeHtml = `<span class="inline-flex items-center justify-center px-2 py-0.5 text-xs font-bold leading-none text-red-100 bg-red-500 rounded-full shadow-sm">${badge}</span>`;
        }
        return `
            <a href="#" data-page="${id}" class="nav-item group flex items-center px-3 py-2.5 text-sm font-medium rounded-xl transition-all duration-200 ${active
                ? 'bg-indigo-50 dark:bg-indigo-900/20 text-indigo-600 dark:text-indigo-400 shadow-sm'
                : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700/50 hover:text-gray-900 dark:hover:text-white'
            }">
                <i class="${icon} w-5 h-5 text-center mr-3 transition-colors ${active ? 'text-indigo-600 dark:text-indigo-400' : 'text-gray-400 group-hover:text-gray-600 dark:group-hover:text-gray-300'
            }"></i>
                <span class="flex-1">${label}</span>
                ${badgeHtml}
            </a>
        `;
    },

    attachListeners() {
        document.querySelectorAll('.nav-item').forEach(item => {
            item.addEventListener('click', (e) => {
                e.preventDefault();
                const page = item.dataset.page;

                if (page === 'logout') {
                    if (confirm("Are you sure?")) {
                        localStorage.clear();
                        window.location.href = 'index.html';
                    }
                    return;
                }

                // Visual Update Only (Real routing handled by main.js logic later)
                document.querySelectorAll('.nav-item').forEach(nav => {
                    nav.classList.remove('bg-indigo-50', 'dark:bg-indigo-900/20', 'text-indigo-600', 'dark:text-indigo-400', 'shadow-sm');
                    nav.classList.add('text-gray-600', 'dark:text-gray-400');
                    // Reset Icons
                    const icon = nav.querySelector('i');
                    icon.classList.remove('text-indigo-600', 'dark:text-indigo-400');
                    icon.classList.add('text-gray-400');
                });

                item.classList.add('bg-indigo-50', 'dark:bg-indigo-900/20', 'text-indigo-600', 'dark:text-indigo-400', 'shadow-sm');
                item.classList.remove('text-gray-600', 'dark:text-gray-400');
                const activeIcon = item.querySelector('i');
                activeIcon.classList.remove('text-gray-400');
                activeIcon.classList.add('text-indigo-600', 'dark:text-indigo-400');

                // Trigger navigation event
                window.dispatchEvent(new CustomEvent('navigate', { detail: { page } }));

                // Close Mobile Sidebar
                if (window.innerWidth < 1024) {
                    window.toggleSidebar();
                }
            });
        });
    }
};
