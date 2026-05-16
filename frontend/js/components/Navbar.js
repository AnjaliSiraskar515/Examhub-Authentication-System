import { API } from '../utils/api.js';
import { showNotificationToast } from '../pages/Notifications.js';

export const Navbar = {
    _knownIds: new Set(),
    _pollInterval: null,

    render(targetId) {
        const container = document.getElementById(targetId);
        if (!container) return;

        container.innerHTML = `
            <div class="h-full px-6 flex items-center justify-between">
                <!-- Left: Mobile Toggle & Search -->
                <div class="flex items-center gap-4">
                    <button onclick="window.toggleSidebar()" class="lg:hidden p-2 text-gray-500 hover:text-indigo-600 transition-colors">
                        <i class="fas fa-bars text-xl"></i>
                    </button>
                    
                    <!-- Search Bar (Desktop) -->
                    <div class="hidden md:flex items-center relative group">
                        <i class="fas fa-search absolute left-3 text-gray-400 group-focus-within:text-indigo-500 transition-colors"></i>
                        <input type="text" placeholder="Search exams..." 
                            class="pl-10 pr-4 py-2 bg-gray-100 dark:bg-gray-700/50 border-none rounded-xl text-sm w-64 focus:ring-2 focus:ring-indigo-500 focus:bg-white dark:focus:bg-gray-800 transition-all text-gray-700 dark:text-gray-200 placeholder-gray-400">
                    </div>
                </div>

                <!-- Right: Actions -->
                <div class="flex items-center gap-3 sm:gap-4">
                    <!-- Theme Toggle -->
                    <button id="theme-btn" onclick="window.dispatchEvent(new CustomEvent('theme-toggle'))" 
                        class="w-10 h-10 flex items-center justify-center rounded-xl bg-gray-50 dark:bg-gray-700 text-gray-500 dark:text-yellow-400 hover:bg-gray-100 dark:hover:bg-gray-600 transition-all shadow-sm">
                        <i class="fas fa-moon dark:hidden"></i>
                        <i class="fas fa-sun hidden dark:block"></i>
                    </button>

                    <!-- Notifications Dropdown Trigger -->
                    <div class="relative">
                        <button id="notification-btn" class="relative w-10 h-10 flex items-center justify-center rounded-xl bg-gray-50 dark:bg-gray-700 text-gray-500 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-600 transition-all shadow-sm group">
                            <i class="fas fa-bell group-hover:animate-swing"></i>
                            <span id="notification-dot" class="absolute -top-1 -right-1 hidden min-w-[18px] h-[18px] bg-red-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center border-2 border-white dark:border-gray-800 px-0.5"></span>
                        </button>
                        <!-- Dropdown Panel -->
                        <div id="notification-dropdown" class="hidden absolute right-0 mt-2 w-96 bg-white dark:bg-gray-800 rounded-2xl shadow-2xl border border-gray-100 dark:border-gray-700 overflow-hidden z-50 transition-all">
                            <!-- Header -->
                            <div class="px-5 py-4 border-b border-gray-100 dark:border-gray-700 flex items-center justify-between bg-gradient-to-r from-indigo-50 to-purple-50 dark:from-indigo-900/20 dark:to-purple-900/20">
                                <div>
                                    <span class="text-sm font-bold text-gray-900 dark:text-white">Notifications</span>
                                    <p id="notif-unread-label" class="text-xs text-gray-500 dark:text-gray-400 mt-0.5"></p>
                                </div>
                                <button id="notification-mark-read" class="text-xs font-semibold text-indigo-600 hover:text-indigo-700 dark:text-indigo-400 bg-white dark:bg-gray-700 px-3 py-1.5 rounded-lg shadow-sm transition-colors">Mark all read</button>
                            </div>
                            <!-- List -->
                            <div id="notification-list" class="max-h-80 overflow-y-auto divide-y divide-gray-100 dark:divide-gray-700/60"></div>
                            <div id="notification-empty" class="px-5 py-10 text-center hidden">
                                <i class="fas fa-bell-slash text-3xl text-gray-300 dark:text-gray-600 mb-3"></i>
                                <p class="text-sm text-gray-500 dark:text-gray-400">No new notifications</p>
                            </div>
                            <!-- Footer -->
                            <div class="px-5 py-3 border-t border-gray-100 dark:border-gray-700 text-center">
                                <a href="#" id="notif-view-all" class="text-xs font-bold text-indigo-600 dark:text-indigo-400 hover:text-indigo-700 uppercase tracking-widest">View All Notifications</a>
                            </div>
                        </div>
                    </div>
                    
                    <div class="h-8 w-px bg-gray-200 dark:bg-gray-700 mx-1 hidden sm:block"></div>

                    <!-- Profile Status -->
                    <div id="nav-verif-container" class="flex items-center gap-2 cursor-pointer hover:opacity-80 transition-opacity">
                         <div class="hidden md:block text-right">
                             <p id="nav-verif-title" class="text-xs font-semibold text-gray-700 dark:text-gray-200">Pending</p>
                             <p id="nav-verif-subtitle" class="text-[10px] text-orange-500 font-bold tracking-wide uppercase">Verification</p>
                         </div>
                         <div id="nav-verif-dot" class="w-2 h-2 rounded-full bg-orange-500 animate-pulse"></div>
                    </div>
                </div>
            </div>
        `;

        this.setupNotifications();
    },

    setupNotifications() {
        const button = document.getElementById('notification-btn');
        const dropdown = document.getElementById('notification-dropdown');
        const list = document.getElementById('notification-list');
        const emptyState = document.getElementById('notification-empty');
        const markReadBtn = document.getElementById('notification-mark-read');
        const dot = document.getElementById('notification-dot');
        const unreadLabel = document.getElementById('notif-unread-label');
        const viewAllBtn = document.getElementById('notif-view-all');

        if (!button || !dropdown || !list) return;

        const normalizeNotification = (n) => {
            const id = n.id || n.notificationId || (n.title && n.createdAt ? `${n.title}-${n.createdAt}` : null) || 'unknown';
            return {
                id: String(id),
                title: n.title || 'Notification',
                message: n.message || '',
                createdAt: n.createdAt || n.timestamp || new Date().toISOString(),
                read: n.read || false,
                type: n.type || 'info'
            };
        };

        const getIconInfo = (title = '') => {
            const t = title.toLowerCase();
            if (t.includes('verified') || t.includes('accepted')) return { icon: 'fas fa-check-circle', bg: 'bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400' };
            if (t.includes('rejected') || t.includes('failed')) return { icon: 'fas fa-times-circle', bg: 'bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400' };
            if (t.includes('registered') || t.includes('successful')) return { icon: 'fas fa-file-signature', bg: 'bg-indigo-100 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400' };
            return { icon: 'fas fa-bell', bg: 'bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400' };
        };

        const formatTime = (dateStr) => {
            if (!dateStr) return '';
            try {
                const d = new Date(dateStr);
                const diffMins = Math.floor((Date.now() - d) / 60000);
                if (diffMins < 1) return 'Just now';
                if (diffMins < 60) return `${diffMins}m ago`;
                if (diffMins < 1440) return `${Math.floor(diffMins / 60)}h ago`;
                return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short' }) + ' ' + d.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: false });
            } catch { return ''; }
        };

        const renderDropdownList = (notifications) => {
            if (!notifications || notifications.length === 0) {
                list.innerHTML = '';
                emptyState.classList.remove('hidden');
                return;
            }
            emptyState.classList.add('hidden');
            list.innerHTML = notifications.slice(0, 6).map(n => {
                const item = normalizeNotification(n);
                const iconInfo = getIconInfo(item.title);
                return `
                    <div class="flex items-start gap-3 px-5 py-4 hover:bg-gray-50 dark:hover:bg-gray-700/40 transition-colors ${!item.read ? 'bg-indigo-50/60 dark:bg-indigo-900/10' : ''}">
                        <div class="relative shrink-0">
                            <div class="w-9 h-9 rounded-xl ${iconInfo.bg} flex items-center justify-center">
                                <i class="${iconInfo.icon} text-xs"></i>
                            </div>
                            ${!item.read ? '<span class="absolute -top-0.5 -right-0.5 w-2.5 h-2.5 bg-indigo-500 rounded-full border-2 border-white dark:border-gray-800"></span>' : ''}
                        </div>
                        <div class="flex-1 min-w-0">
                            <div class="flex items-start justify-between gap-1">
                                <p class="text-xs font-bold text-gray-900 dark:text-white leading-snug">${item.title}</p>
                                <span class="text-[10px] text-gray-400 shrink-0 whitespace-nowrap">${formatTime(item.createdAt)}</span>
                            </div>
                            <p class="text-xs text-gray-500 dark:text-gray-400 mt-0.5 leading-snug line-clamp-2">${item.message}</p>
                        </div>
                    </div>
                `;
            }).join('');
        };

        const loadNotifications = async (triggeredByUser = false) => {
            let notifications = [];
            try {
                const apiData = await API.getNotifications();
                if (Array.isArray(apiData)) notifications = apiData;
            } catch (e) { /* silent */ }

            const local = JSON.parse(localStorage.getItem('mock-notifications') || '[]');
            const readIds = new Set(JSON.parse(localStorage.getItem('read-notification-ids') || '[]'));
            const merged = new Map();

            // 1. API as base
            notifications.forEach(n => {
                const norm = normalizeNotification(n);
                if (readIds.has(norm.id)) norm.read = true;
                merged.set(norm.id, norm);
            });

            // 2. Local as overlay
            local.forEach(L => {
                const norm = normalizeNotification(L);
                const id = String(norm.id);
                if (readIds.has(id)) norm.read = true;
                if (merged.has(id)) {
                    if (norm.read) merged.get(id).read = true;
                } else {
                    merged.set(id, norm);
                }
            });

            const all = Array.from(merged.values()).sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

            const unread = all.filter(n => !n.read);

            // Detect NEW notifications since last poll and show toasts
            if (!triggeredByUser) {
                const newOnes = all.filter(n => !n.read && !this._knownIds.has(n.id));
                newOnes.forEach(n => {
                    showNotificationToast(n.title, n.message);
                });
            }

            // Track all IDs we've seen
            all.forEach(n => this._knownIds.add(n.id));

            // Update bell badge
            if (unread.length > 0) {
                dot.classList.remove('hidden');
                dot.textContent = unread.length > 9 ? '9+' : String(unread.length);
                if (unreadLabel) unreadLabel.textContent = `${unread.length} unread notification${unread.length !== 1 ? 's' : ''}`;
            } else {
                dot.classList.add('hidden');
                if (unreadLabel) unreadLabel.textContent = 'All caught up!';
            }

            // Update sidebar badge
            const sidebarBadge = document.querySelector('.notif-sidebar-badge');
            if (sidebarBadge) {
                if (unread.length > 0) {
                    sidebarBadge.textContent = unread.length > 9 ? '9+' : String(unread.length);
                    sidebarBadge.classList.remove('hidden');
                } else {
                    sidebarBadge.classList.add('hidden');
                }
            }

            renderDropdownList(all);
            return all;
        };

        // Toggle dropdown
        button.addEventListener('click', async (e) => {
            e.stopPropagation();
            dropdown.classList.toggle('hidden');
            if (!dropdown.classList.contains('hidden')) {
                await loadNotifications(true);
            }
        });

        // Close on outside click
        document.addEventListener('click', (e) => {
            if (!dropdown.contains(e.target) && !button.contains(e.target)) {
                dropdown.classList.add('hidden');
            }
        });

        // Mark all read
        markReadBtn.addEventListener('click', async () => {
            const all = await loadNotifications(true);
            const readIds = new Set(JSON.parse(localStorage.getItem('read-notification-ids') || '[]'));

            all.forEach(n => readIds.add(n.id));
            localStorage.setItem('read-notification-ids', JSON.stringify(Array.from(readIds)));

            dot.classList.add('hidden');
            dot.textContent = '0';
            if (unreadLabel) unreadLabel.textContent = 'All caught up!';

            const sidebarBadge = document.querySelector('.notif-sidebar-badge');
            if (sidebarBadge) { sidebarBadge.textContent = '0'; sidebarBadge.classList.add('hidden'); }

            await loadNotifications(true);
            window.dispatchEvent(new CustomEvent('notifications-cleared'));
        });

        // Navigate to full notifications page
        if (viewAllBtn) {
            viewAllBtn.addEventListener('click', (e) => {
                e.preventDefault();
                dropdown.classList.add('hidden');
                window.dispatchEvent(new CustomEvent('navigate', { detail: { page: 'notifications' } }));
            });
        }

        // Initial load
        loadNotifications(true);

        // Global update listener
        window.addEventListener('notifications-updated', (e) => {
            const count = e.detail.count;
            if (count > 0) {
                dot.classList.remove('hidden');
                dot.textContent = count > 9 ? '9+' : String(count);
                if (unreadLabel) unreadLabel.textContent = `${count} unread notification${count !== 1 ? 's' : ''}`;
            } else {
                dot.classList.add('hidden');
                if (unreadLabel) unreadLabel.textContent = 'All caught up!';
            }
        });

        // Verification Status Listener
        window.addEventListener('verification-status', (e) => {
            const verified = e.detail.verified;
            const title = document.getElementById('nav-verif-title');
            const subtitle = document.getElementById('nav-verif-subtitle');
            const verifDot = document.getElementById('nav-verif-dot');
            
            if (verified) {
                if (title) title.textContent = 'Status';
                if (subtitle) {
                    subtitle.textContent = 'Verified';
                    subtitle.className = 'text-[10px] text-green-500 font-bold tracking-wide uppercase';
                }
                if (verifDot) {
                    verifDot.className = 'w-2 h-2 rounded-full bg-green-500';
                }
            } else {
                if (title) title.textContent = 'Pending';
                if (subtitle) {
                    subtitle.textContent = 'Verification';
                    subtitle.className = 'text-[10px] text-orange-500 font-bold tracking-wide uppercase';
                }
                if (verifDot) {
                    verifDot.className = 'w-2 h-2 rounded-full bg-orange-500 animate-pulse';
                }
            }
        });

        // Poll every 30 seconds for new notifications
        if (this._pollInterval) clearInterval(this._pollInterval);
        this._pollInterval = setInterval(() => loadNotifications(false), 30000);
    }
};
