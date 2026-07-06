import { StatCard } from '../components/StatCard.js';

import { API } from '../utils/api.js';

export const DashboardHome = {
    stats: {
        totalRegistered: 0,
        pendingVerifications: 0,
        approvedExams: 0,
        upcomingExams: 0
    },
    upcomingExamsList: [],

    async init() {
        // Fetch real stats from API
        try {
            const stats = await API.getStudentStats();
            this.stats = stats;
        } catch (error) {
            console.error('Failed to load dashboard data:', error);
            // Keep default stats
        }
        
        // Fetch real upcoming schedule
        try {
            const token = localStorage.getItem('token') || localStorage.getItem('jwtToken');
            if (token) {
                const regsResponse = await fetch('/api/student/registrations', { headers: { 'Authorization': `Bearer ${token}` } });
                const regs = await regsResponse.ok ? await regsResponse.json() : [];
                
                const examsResponse = await fetch('/api/university/exams', { headers: { 'Authorization': `Bearer ${token}` } });
                const exams = await examsResponse.ok ? await examsResponse.json() : [];

                const approvedRegs = regs.filter(r => r.registrationStatus === 'APPROVED');
                const today = new Date();
                today.setHours(0, 0, 0, 0); // compare from start of today

                const allMapped = approvedRegs.map(r => {
                    const exam = exams.find(e => e.id === r.examId);
                    const fmtTime = t => t ? (parseInt(t.split(':')[0]) % 12 || 12) + ':' + t.split(':')[1] + ' ' + (parseInt(t.split(':')[0]) >= 12 ? 'PM' : 'AM') : 'TBD';
                    const sTime = exam && exam.schedule && exam.schedule.startTime ? fmtTime(exam.schedule.startTime) : 'TBD';
                    const eTime = exam && exam.schedule && exam.schedule.endTime ? fmtTime(exam.schedule.endTime) : 'TBD';
                    let calcETime = 'TBD';
                    if (exam && exam.schedule && exam.schedule.startTime) {
                        try {
                            const duration = exam.durationMinutes || 180;
                            const [h, m] = exam.schedule.startTime.split(':').map(Number);
                            const start = new Date();
                            start.setHours(h, m, 0);
                            const end = new Date(start.getTime() + duration * 60000);
                            const formatOpts = { hour12: true, hour: '2-digit', minute: '2-digit' };
                            calcETime = end.toLocaleTimeString('en-US', formatOpts);
                        } catch (e) {}
                    }
                    if (eTime !== 'TBD') calcETime = eTime;

                    const examDate = exam && exam.schedule && exam.schedule.examDate ? exam.schedule.examDate : null;
                    const examStatus = exam ? (exam.status || '') : '';

                    return {
                        title: exam ? exam.sessionName : (r.examSession || 'Unnamed Exam'),
                        date: examDate || 'TBD',
                        time: sTime !== 'TBD' ? `${sTime} to ${calcETime}` : 'TBD',
                        venue: exam && exam.centerName ? exam.centerName : (r.institutionName || 'Dhole Patil College of Engineering'),
                        _examDate: examDate,
                        _examStatus: examStatus
                    };
                });

                // Only show exams that are not completed AND whose date is today or in the future OR status is OPEN/LIVE
                this.upcomingExamsList = allMapped.filter(item => {
                    if (item._examStatus === 'COMPLETED') return false;
                    
                    if (item._examDate && item._examDate !== 'TBD') {
                        const d = new Date(item._examDate);
                        d.setHours(0, 0, 0, 0);
                        if (d < today) return false;
                    }
                    return true;
                });
            }
        } catch (error) {
            console.error('Failed to load upcoming exams:', error);
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
                    <button id="generate-report-btn" class="px-4 py-2 bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-200 rounded-xl shadow-sm hover:shadow-md transition-all text-sm font-medium border border-gray-100 dark:border-gray-700">
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
                    ${this.upcomingExamsList.length > 0 
                        ? this.upcomingExamsList.map(exam => this.renderExamItem(
                            exam.title || 'Untitled Exam', 
                            exam.date || 'TBD', 
                            exam.time || 'TBD', 
                            exam.venue || 'TBD'
                          )).join('')
                        : '<div class="text-center text-gray-500 py-4"><i class="fas fa-calendar-times text-2xl mb-2 opacity-50"></i><p>No upcoming exams scheduled.</p></div>'
                    }
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

        // Handle Report Generation
        const reportBtn = document.getElementById('generate-report-btn');
        if (reportBtn) {
            reportBtn.addEventListener('click', async () => {
                const originalHtml = reportBtn.innerHTML;
                reportBtn.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i> Generating...';
                reportBtn.disabled = true;

                try {
                    const profile = await API.getProfile() || {};
                    const userId = localStorage.getItem('userId');
                    let regs = [];
                    if (userId) {
                        regs = await API.getRegistrations(userId) || [];
                    }

                    const reportWindow = window.open('', '_blank');
                    const stats = DashboardHome.stats;
                    
                    const html = `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Student Academic Report - ${profile.name || window.currentStudentName || 'Student'}</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Outfit:wght@600;700&display=swap');
        body { font-family: 'Inter', sans-serif; background-color: #f3f4f6; color: #1f2937; margin: 0; padding: 2rem; }
        .font-display { font-family: 'Outfit', sans-serif; }
        
        @media print {
            body { background-color: white; padding: 0; }
            .no-print { display: none !important; }
            .print-container { box-shadow: none !important; border: none !important; padding: 0 !important; max-width: 100% !important; }
            .page-break { page-break-before: always; }
        }
        
        .status-badge { display: inline-flex; align-items: center; padding: 0.25rem 0.75rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 600; }
        .status-approved { background-color: #d1fae5; color: #065f46; border: 1px solid #34d399; }
        .status-pending { background-color: #fef3c7; color: #92400e; border: 1px solid #fbbf24; }
        .status-rejected { background-color: #fee2e2; color: #991b1b; border: 1px solid #f87171; }
    </style>
</head>
<body class="antialiased">
    <div class="print-container max-w-4xl mx-auto bg-white p-10 rounded-3xl shadow-xl border border-gray-100 relative overflow-hidden">
        
        <!-- Decorative Header Background -->
        <div class="absolute top-0 left-0 w-full h-32 bg-gradient-to-r from-indigo-600 to-purple-600 opacity-10 pointer-events-none"></div>

        <div class="no-print flex justify-between mb-6 relative z-10">
            <button onclick="window.close()" class="px-5 py-2.5 bg-gray-100 text-gray-700 rounded-xl font-bold shadow-sm hover:bg-gray-200 transition-all flex items-center gap-2 border border-gray-200">
                <i class="fas fa-arrow-left"></i> Go Back
            </button>
            <button onclick="window.print()" class="px-5 py-2.5 bg-indigo-600 text-white rounded-xl font-bold shadow-lg shadow-indigo-200 hover:bg-indigo-700 hover:-translate-y-0.5 transition-all flex items-center gap-2">
                <i class="fas fa-print"></i> Print Official Report
            </button>
        </div>
        
        <!-- Header -->
        <div class="text-center border-b-2 border-gray-100 pb-8 mb-8 relative z-10">
            <img src="/logo.png" alt="ExamHub Logo" class="w-16 h-16 object-contain rounded-2xl mx-auto shadow-sm mb-4 bg-white p-1">
            <h1 class="text-3xl font-display font-bold text-gray-900 mb-2">ExamHub Academic Report</h1>
            <p class="text-sm font-medium text-gray-500 uppercase tracking-widest">Official Student Record</p>
            <p class="text-xs text-gray-400 mt-2">Generated on: ${new Date().toLocaleString('en-IN', { dateStyle: 'full', timeStyle: 'short' })}</p>
        </div>

        <!-- Student Profile Section -->
        <div class="mb-10">
            <h2 class="text-lg font-display font-bold text-gray-900 border-b border-gray-200 pb-2 mb-6 flex items-center gap-2">
                <i class="fas fa-user-graduate text-indigo-500"></i> Student Profile
            </h2>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div class="bg-gray-50/50 p-4 rounded-2xl border border-gray-100">
                    <p class="text-[10px] uppercase font-bold text-gray-500 tracking-wider mb-1">Full Name</p>
                    <p class="font-semibold text-gray-900">${profile.name || window.currentStudentName || 'N/A'}</p>
                </div>
                <div class="bg-gray-50/50 p-4 rounded-2xl border border-gray-100">
                    <p class="text-[10px] uppercase font-bold text-gray-500 tracking-wider mb-1">Registration Number (PRN)</p>
                    <p class="font-semibold text-gray-900">${profile.prn || profile.rollNumber || 'N/A'}</p>
                </div>
                <div class="bg-gray-50/50 p-4 rounded-2xl border border-gray-100">
                    <p class="text-[10px] uppercase font-bold text-gray-500 tracking-wider mb-1">Course & Department</p>
                    <p class="font-semibold text-gray-900">${[profile.course, profile.department].filter(Boolean).join(' - ') || 'N/A'}</p>
                </div>
                <div class="bg-gray-50/50 p-4 rounded-2xl border border-gray-100">
                    <p class="text-[10px] uppercase font-bold text-gray-500 tracking-wider mb-1">Current Academic Year</p>
                    <p class="font-semibold text-gray-900">${profile.year || 'N/A'}</p>
                </div>
                <div class="bg-gray-50/50 p-4 rounded-2xl border border-gray-100 md:col-span-2">
                    <p class="text-[10px] uppercase font-bold text-gray-500 tracking-wider mb-1">Email Address</p>
                    <p class="font-semibold text-gray-900">${profile.email || 'N/A'}</p>
                </div>
            </div>
        </div>

        <!-- Academic Statistics -->
        <div class="mb-12">
            <h2 class="text-lg font-display font-bold text-gray-900 border-b border-gray-200 pb-2 mb-6 flex items-center gap-2">
                <i class="fas fa-chart-pie text-indigo-500"></i> Overall Statistics
            </h2>
            <div class="grid grid-cols-2 md:grid-cols-4 gap-4 text-center">
                <div class="p-5 rounded-2xl bg-indigo-50 border border-indigo-100">
                    <div class="text-3xl font-display font-bold text-indigo-700 mb-1">${stats.totalRegistered || 0}</div>
                    <div class="text-[10px] uppercase font-bold text-indigo-600 tracking-wider">Total Registered</div>
                </div>
                <div class="p-5 rounded-2xl bg-emerald-50 border border-emerald-100">
                    <div class="text-3xl font-display font-bold text-emerald-700 mb-1">${stats.approvedExams || 0}</div>
                    <div class="text-[10px] uppercase font-bold text-emerald-600 tracking-wider">Approved Exams</div>
                </div>
                <div class="p-5 rounded-2xl bg-amber-50 border border-amber-100">
                    <div class="text-3xl font-display font-bold text-amber-700 mb-1">${stats.pendingVerifications || 0}</div>
                    <div class="text-[10px] uppercase font-bold text-amber-600 tracking-wider">Pending Verifications</div>
                </div>
                <div class="p-5 rounded-2xl bg-rose-50 border border-rose-100">
                    <div class="text-3xl font-display font-bold text-rose-700 mb-1">${stats.upcomingExams || 0}</div>
                    <div class="text-[10px] uppercase font-bold text-rose-600 tracking-wider">Upcoming Exams</div>
                </div>
            </div>
        </div>

        <!-- Registered Exams Timeline/Table -->
        <div class="page-break">
            <h2 class="text-lg font-display font-bold text-gray-900 border-b border-gray-200 pb-2 mb-6 flex items-center gap-2">
                <i class="fas fa-calendar-check text-indigo-500"></i> Exam Registrations History
            </h2>
            
            ${regs.length > 0 ? `
            <div class="overflow-hidden border border-gray-200 rounded-2xl">
                <table class="w-full text-left border-collapse">
                    <thead>
                        <tr class="bg-gray-50 border-b border-gray-200">
                            <th class="py-4 px-5 text-xs font-bold text-gray-500 uppercase tracking-wider">Exam Details</th>
                            <th class="py-4 px-5 text-xs font-bold text-gray-500 uppercase tracking-wider">Date & Time</th>
                            <th class="py-4 px-5 text-xs font-bold text-gray-500 uppercase tracking-wider">Venue</th>
                            <th class="py-4 px-5 text-xs font-bold text-gray-500 uppercase tracking-wider">Status</th>
                        </tr>
                    </thead>
                    <tbody class="divide-y divide-gray-100">
                        ${regs.map(r => {
                            const currentStatus = r.registrationStatus || r.status || 'PENDING';
                            const isApproved = currentStatus === 'APPROVED' || currentStatus === 'VERIFIED';
                            const isRejected = currentStatus === 'REJECTED';
                            const badgeClass = isApproved ? 'status-approved' : (isRejected ? 'status-rejected' : 'status-pending');
                            const iconClass = isApproved ? 'fa-check-circle' : (isRejected ? 'fa-times-circle' : 'fa-clock');
                            return `
                            <tr class="hover:bg-gray-50/50 transition-colors">
                                <td class="py-4 px-5">
                                    <div class="font-bold text-gray-900">${r.examSession || r.examName || r.exam?.examName || r.exam?.title || 'Unknown Exam'}</div>
                                    <div class="text-xs text-gray-500 mt-0.5">${r.course || r.exam?.course || profile.course || 'General'} • ${r.exam?.semester || profile.semester || 'Semester N/A'}</div>
                                </td>
                                <td class="py-4 px-5">
                                    <div class="font-semibold text-gray-800">${r.appliedDate || r.examDate || r.exam?.examDate || r.exam?.date || 'Date TBD'}</div>
                                    <div class="text-xs text-gray-500 mt-0.5"><i class="far fa-clock mr-1"></i>${r.time || r.exam?.time || 'Time TBD'}</div>
                                </td>
                                <td class="py-4 px-5 text-sm font-medium text-gray-700">
                                    ${r.institutionName || r.venue || r.exam?.venue || 'TBA'}
                                </td>
                                <td class="py-4 px-5">
                                    <span class="status-badge ${badgeClass}">
                                        <i class="fas ${iconClass} mr-1.5"></i> ${currentStatus}
                                    </span>
                                </td>
                            </tr>
                        `}).join('')}
                    </tbody>
                </table>
            </div>
            ` : `
            <div class="text-center py-10 bg-gray-50 rounded-2xl border border-gray-200 border-dashed">
                <div class="w-16 h-16 bg-gray-100 text-gray-400 rounded-full flex items-center justify-center text-2xl mx-auto mb-3">
                    <i class="fas fa-folder-open"></i>
                </div>
                <p class="font-bold text-gray-900">No Registrations Found</p>
                <p class="text-sm text-gray-500 mt-1">You haven't registered for any exams yet.</p>
            </div>
            `}
        </div>

        <!-- Footer -->
        <div class="mt-16 pt-8 border-t-2 border-gray-100 text-center">
            <div class="mb-4">
                <div class="inline-block border-b border-gray-400 w-48 mb-2"></div>
                <p class="text-xs font-bold text-gray-600 uppercase tracking-widest">Authorized Signature</p>
            </div>
            <p class="text-xs text-gray-500">This report is automatically generated by the ExamHub System and is valid for official academic tracking.</p>
            <p class="text-xs text-gray-400 mt-1">Ref ID: EH-RPT-${Math.floor(Math.random() * 900000) + 100000}-${Date.now().toString().slice(-6)}</p>
        </div>
    </div>
</body>
</html>
                    `;
                    
                    reportWindow.document.write(html);
                    reportWindow.document.close();

                } catch (error) {
                    console.error("Failed to generate report", error);
                    alert("Failed to generate report. Please try again.");
                } finally {
                    reportBtn.innerHTML = originalHtml;
                    reportBtn.disabled = false;
                }
            });
        }
    }
};
