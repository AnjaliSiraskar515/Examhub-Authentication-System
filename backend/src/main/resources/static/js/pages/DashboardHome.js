import { StatCard } from '../components/StatCard.js';

import { API } from '../utils/api.js';

export const DashboardHome = {
    stats: {
        totalRegistered: 0,
        pendingVerifications: 0,
        approvedExams: 0,
        upcomingExams: 0
    },

    async init() {
        // Fetch real stats from API
        try {
            const stats = await API.getStudentStats();
            this.stats = stats;
        } catch (error) {
            console.error('Failed to load dashboard data:', error);
            // Keep default stats
        }
    },

    render() {
        return `
            <!-- Welcome Section -->
            <div class="mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4 animate-fade-in-up">
                <div>
                    <h1 class="text-2xl font-bold text-gray-900 dark:text-white font-display">
                        Welcome back, ${(window.currentStudentName || 'Student').split(' ')[0]}! 👋
                    </h1>
                    <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
                        Here's what's happening with your exams today.
                    </p>
                </div>
                <div class="flex gap-3">
                    <button class="px-4 py-2 bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-200 rounded-xl shadow-sm hover:shadow-md transition-all text-sm font-medium border border-gray-100 dark:border-gray-700">
                        <i class="fas fa-download mr-2"></i> Report
                    </button>
                    <button onclick="window.dispatchEvent(new CustomEvent('navigate', { detail: { page: 'exams' } }))" class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl shadow-lg shadow-indigo-500/30 transition-all text-sm font-medium">
                        <i class="fas fa-plus mr-2"></i> Register New
                    </button>
                </div>
            </div>

            <!-- Stats Grid -->
            <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6 mb-8">
                ${StatCard.render('Total Registered', this.stats.totalRegistered, 'fas fa-book', 'text-indigo-600', 0)}
                ${StatCard.render('Pending Verifications', this.stats.pendingVerifications, 'fas fa-clock', 'text-amber-500', 100)}
                ${StatCard.render('Approved Exams', this.stats.approvedExams, 'fas fa-check-circle', 'text-emerald-500', 200)}
                ${StatCard.render('Upcoming Exams', this.stats.upcomingExams, 'fas fa-calendar-alt', 'text-rose-500', 300)}
            </div>

            <!-- Charts & Content Grid Removed -->

            <!-- Upcoming Exams List -->
            <div class="glass-card p-6 rounded-2xl animate-fade-in-up" style="animation-delay: 600ms">
                <div class="flex items-center justify-between mb-6">
                    <h3 class="text-lg font-bold text-gray-900 dark:text-white font-display">Upcoming Schedule</h3>
                    <a href="#" class="text-indigo-600 hover:text-indigo-700 text-sm font-medium">View All</a>
                </div>
                
                <div class="space-y-4">
                    ${this.renderExamItem('Advanced Java Programming', '10 Mar, 2026', '10:00 AM', 'Hall A')}
                    ${this.renderExamItem('Database Management Systems', '12 Mar, 2026', '02:00 PM', 'Lab 3')}
                    ${this.renderExamItem('Software Engineering', '15 Mar, 2026', '10:00 AM', 'Hall B')}
                </div>
            </div>
        `;
    },

    renderExamItem(title, date, time, venue) {
        return `
            <div class="flex items-center p-4 rounded-xl bg-gray-50 dark:bg-gray-800/50 border border-gray-100 dark:border-gray-700/50 hover:bg-white dark:hover:bg-gray-700 transition-colors group cursor-pointer">
                <div class="w-12 h-12 rounded-xl bg-indigo-100 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400 flex items-center justify-center font-bold text-lg shadow-sm">
                    ${date.split(' ')[0]}
                </div>
                <div class="ml-4 flex-1">
                    <h4 class="text-sm font-bold text-gray-900 dark:text-white group-hover:text-indigo-600 transition-colors">${title}</h4>
                    <div class="flex items-center gap-4 mt-1 text-xs text-gray-500 dark:text-gray-400">
                        <span><i class="far fa-calendar mr-1"></i> ${date}</span>
                        <span><i class="far fa-clock mr-1"></i> ${time}</span>
                    </div>
                </div>
                <div class="text-right">
                    <span class="px-3 py-1 rounded-full text-xs font-semibold bg-indigo-50 dark:bg-indigo-900/20 text-indigo-600 dark:text-indigo-400 border border-indigo-100 dark:border-indigo-800">
                        ${venue}
                    </span>
                </div>
            </div>
        `;
    },

    afterRender() {
        // Initialize Counters
        StatCard.animateCounters();


    }
};
