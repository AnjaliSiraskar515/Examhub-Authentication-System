import { API } from '../utils/api.js';

// ─── Toast Pop-up System ────────────────────────────────────────────────────
export function showNotificationToast(title, message) {
    const existing = document.getElementById('notif-toast-container');
    const container = existing || (() => {
        const el = document.createElement('div');
        el.id = 'notif-toast-container';
        document.body.appendChild(el);
        return el;
    })();

    const toast = document.createElement('div');
    toast.className = 'notif-toast';
    toast.innerHTML = `
        <div class="w-10 h-10 rounded-xl bg-indigo-100 dark:bg-indigo-900/40 text-indigo-600 flex items-center justify-center shrink-0">
            <i class="fas fa-bell text-sm"></i>
        </div>
        <div class="flex-1 min-w-0">
            <p class="text-sm font-bold text-gray-900 dark:text-white">${title}</p>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-0.5 line-clamp-2">${message}</p>
            <span class="text-[10px] text-indigo-400 font-medium mt-1 block">Just now</span>
        </div>
        <button onclick="this.closest('.notif-toast').remove()" class="text-gray-400 hover:text-gray-600 shrink-0 mt-0.5">
            <i class="fas fa-times text-xs"></i>
        </button>
    `;
    container.appendChild(toast);
    requestAnimationFrame(() => requestAnimationFrame(() => toast.classList.add('show')));
    setTimeout(() => { toast.classList.remove('show'); setTimeout(() => toast.remove(), 500); }, 5000);
}

// ─── Notifications Page ─────────────────────────────────────────────────────
export const Notifications = {
    notifications: [],
    VISIBLE_COUNT: 10,

    async init() {
        let apiNotifications = [];
        try {
            const rawApi = await API.getNotifications();
            apiNotifications = Array.isArray(rawApi) ? rawApi : [];
        } catch (error) {
            console.warn('Failed to load backend notifications:', error);
        }

        const localNotifications = JSON.parse(localStorage.getItem('mock-notifications') || '[]');
        const readIds = new Set(JSON.parse(localStorage.getItem('read-notification-ids') || '[]'));

        const mergedMap = new Map();

        // 1. Load API notifications
        apiNotifications.forEach(n => {
            const norm = this.normalizeNotification(n);
            if (readIds.has(norm.id)) norm.read = true;
            mergedMap.set(String(norm.id), norm);
        });

        // 2. Overlay with local state
        localNotifications.forEach(n => {
            const norm = this.normalizeNotification(n);
            const id = String(norm.id);
            if (readIds.has(id)) norm.read = true;
            if (mergedMap.has(id)) {
                if (norm.read) mergedMap.get(id).read = true;
            } else {
                mergedMap.set(id, norm);
            }
        });

        const mergedArray = Array.from(mergedMap.values());
        mergedArray.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
        this.notifications = mergedArray;
    },

    render() {
        const unreadCount = this.notifications.filter(n => !n.read).length;
        this._lastUnreadCount = unreadCount; // Store for badge sync
        const visible = this.notifications.slice(0, this.VISIBLE_COUNT);
        const hasMore = this.notifications.length > this.VISIBLE_COUNT;

        return `
            <div class="notif-page animate-fade-in-up">
                <div class="notif-card">
                    <!-- Header -->
                    <div class="notif-card-header">
                        <div>
                            <h2 class="text-xl font-bold text-gray-900 dark:text-white" style="font-family:'Outfit',sans-serif">Notifications</h2>
                            <p class="text-xs text-slate-500 dark:text-gray-400 mt-1">
                                You have <span id="unread-text" class="font-bold text-indigo-600 dark:text-indigo-400">${unreadCount} unread</span> notifications
                            </p>
                        </div>
                        <button id="mark-all-read-btn" class="notif-mark-read-btn ${unreadCount === 0 ? 'opacity-0 pointer-events-none' : ''}">
                            Mark all read
                        </button>
                    </div>

                    <!-- List container -->
                    <div id="notification-list">
                        ${this.renderListHtml(visible)}
                    </div>

                    <!-- Footer -->
                    <div class="notif-footer">
                        ${hasMore ? `
                            <button id="load-more-btn" class="notif-load-more-btn">VIEW OLDER NOTIFICATIONS</button>
                        ` : `
                            <span class="notif-all-caught">ALL CAUGHT UP</span>
                        `}
                    </div>
                </div>
            </div>
        `;
    },

    renderListHtml(notifications) {
        if (!notifications || notifications.length === 0) {
            return `<div class="p-12 text-center text-slate-400">No notifications found</div>`;
        }

        return notifications.map(n => {
            const item = this.normalizeNotification(n);
            const iconInfo = this.getIconForType(item.title);
            const isUnread = !item.read;

            return `
                <div class="notification-card group flex items-start gap-5 px-8 py-6 hover:bg-indigo-50/30 dark:hover:bg-indigo-900/10 cursor-pointer transition-all ${!isUnread ? 'opacity-75' : ''}" 
                     data-id="${item.id}" onclick="window.Notifications.handleItemClick(this)">
                    <div class="relative">
                        <div class="w-12 h-12 rounded-2xl flex items-center justify-center transition-all duration-300 
                             ${isUnread ? 'bg-indigo-100 text-indigo-600 group-hover:bg-indigo-600 group-hover:text-white' : 'bg-slate-100 text-slate-400 dark:bg-gray-700 dark:text-gray-500'}">
                            <i data-lucide="${iconInfo.icon}" class="w-6 h-6"></i>
                        </div>
                        <div class="status-dot absolute -top-1 -right-1 w-3 h-3 bg-indigo-500 border-2 border-white dark:border-gray-800 rounded-full ${!isUnread ? 'hidden' : ''}"></div>
                    </div>
                    <div class="flex-1">
                        <div class="flex justify-between items-start">
                            <h3 class="font-bold text-slate-800 dark:text-gray-200 group-hover:text-indigo-900 dark:group-hover:text-indigo-400">${item.title}</h3>
                            <span class="text-[11px] font-medium text-slate-400 dark:text-gray-500">${this.formatTime(item.createdAt)}</span>
                        </div>
                        <p class="text-slate-600 dark:text-gray-400 text-sm mt-1 leading-relaxed">
                            ${this.formatMessage(item.message)}
                        </p>
                    </div>
                </div>
            `;
        }).join('');
    },

    getIconForType(title) {
        const t = title.toLowerCase();
        if (t.includes('verified') || t.includes('accepted')) return { icon: 'badge-check' };
        if (t.includes('successful') || t.includes('registered')) return { icon: 'clipboard-check' };
        if (t.includes('exam')) return { icon: 'file-check' };
        return { icon: 'bell' };
    },

    formatMessage(msg) {
        if (!msg) return '';
        // Bold exam IDs or specific terms
        return msg.replace(/(Exam ID \d+)/gi, '<span class="px-1.5 py-0.5 bg-slate-100 dark:bg-gray-700 rounded font-mono text-xs font-bold text-slate-700 dark:text-gray-300 uppercase">$1</span>')
            .replace(/\*\*(.*?)\*\*/g, '<span class="text-slate-900 dark:text-white font-semibold">$1</span>');
    },

    formatTime(dateStr) {
        if (!dateStr) return '';
        try {
            const d = new Date(dateStr);
            const date = d.toLocaleDateString('en-GB', { day: '2-digit', month: '2-digit', year: 'numeric' });
            const time = d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
            return `${date}, ${time}`;
        } catch { return ''; }
    },

    normalizeNotification(n) {
        const id = n.id || n.notificationId || (n.title && n.createdAt ? `${n.title}-${n.createdAt}` : null) || 'unknown';
        return {
            id: String(id),
            title: n.title || 'Notification',
            message: n.message || '',
            createdAt: n.createdAt || n.timestamp || new Date().toISOString(),
            read: n.read || false
        };
    },

    afterRender() {
        if (window.lucide) {
            window.lucide.createIcons();
        }

        const markAllBtn = document.getElementById('mark-all-read-btn');
        if (markAllBtn) {
            markAllBtn.onclick = () => this.markAllAsRead();
        }

        const loadMoreBtn = document.getElementById('load-more-btn');
        if (loadMoreBtn) {
            loadMoreBtn.onclick = () => {
                this.VISIBLE_COUNT += 10;
                const container = document.getElementById('notification-list');
                if (container) {
                    container.innerHTML = this.renderListHtml(this.notifications.slice(0, this.VISIBLE_COUNT));
                    if (window.lucide) window.lucide.createIcons();
                }
                if (this.VISIBLE_COUNT >= this.notifications.length) {
                    loadMoreBtn.parentElement.innerHTML = '<span class="text-xs font-bold text-slate-400 uppercase tracking-widest">All caught up</span>';
                }
            };
        }

        this.updateGlobalBadges();
    },

    async handleItemClick(element) {
        const id = element.dataset.id;
        const normId = String(id);

        const notif = this.notifications.find(n => n.id === normId);
        if (notif && !notif.read) {
            notif.read = true;

            const dot = element.querySelector('.status-dot');
            if (dot) dot.classList.add('hidden');
            element.classList.remove('hover:bg-indigo-50/30');
            element.classList.add('opacity-75');
            const iconBox = element.querySelector('.w-12');
            if (iconBox) {
                iconBox.classList.remove('bg-indigo-100', 'text-indigo-600', 'group-hover:bg-indigo-600', 'group-hover:text-white');
                iconBox.classList.add('bg-slate-100', 'text-slate-400', 'dark:bg-gray-700', 'dark:text-gray-500');
            }

            const readIds = new Set(JSON.parse(localStorage.getItem('read-notification-ids') || '[]'));
            readIds.add(normId);
            localStorage.setItem('read-notification-ids', JSON.stringify(Array.from(readIds)));

            this.updateGlobalBadges();
        }
    },

    markAllAsRead() {
        const readIds = new Set(JSON.parse(localStorage.getItem('read-notification-ids') || '[]'));
        this.notifications.forEach(n => {
            n.read = true;
            readIds.add(n.id);
        });
        localStorage.setItem('read-notification-ids', JSON.stringify(Array.from(readIds)));

        const container = document.getElementById('notification-list');
        if (container) {
            container.innerHTML = this.renderListHtml(this.notifications.slice(0, this.VISIBLE_COUNT));
            if (window.lucide) window.lucide.createIcons();
        }
        this.updateGlobalBadges();
    },

    updateGlobalBadges() {
        const unreadCount = this.notifications.filter(n => !n.read).length;

        const unreadText = document.getElementById('unread-text');
        if (unreadText) {
            unreadText.innerText = unreadCount === 0 ? 'No unread' : `${unreadCount} unread`;
        }

        const markAllBtn = document.getElementById('mark-all-read-btn');
        if (markAllBtn) {
            markAllBtn.classList.toggle('opacity-0', unreadCount === 0);
            markAllBtn.classList.toggle('pointer-events-none', unreadCount === 0);
        }

        const sidebarBadge = document.querySelector('.notif-sidebar-badge');
        if (sidebarBadge) {
            sidebarBadge.innerText = unreadCount;
            sidebarBadge.classList.toggle('hidden', unreadCount === 0);
        }

        window.dispatchEvent(new CustomEvent('notifications-updated', { detail: { count: unreadCount } }));
    }
};

// Expose handleItemClick to global for onclick
window.Notifications = Notifications;
