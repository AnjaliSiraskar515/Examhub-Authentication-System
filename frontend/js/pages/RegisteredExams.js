export const RegisteredExams = {
    registrations: [],

    // Hardcode dummy exams to match ExamRegistration.js context
    examsData: [
        { id: 1, name: "End Semester Theory", subject: "Data Structures", date: "2026-03-15", fee: 500, type: "Offline", university: "Pune University" },
        { id: 2, name: "Mid Term Evaluation", subject: "Web Development", date: "2026-03-20", fee: 500, type: "Online", university: "Mumbai University" },
        { id: 3, name: "Final Practical", subject: "Database Systems", date: "2026-03-25", fee: 500, type: "Offline", university: "Delhi University" }
    ],

    async init() {
        const statuses = JSON.parse(localStorage.getItem('studentExamStatuses') || '{}');
        this.registrations = Object.keys(statuses).map(id => {
            const exam = this.examsData.find(e => e.id == id);
            return exam ? {
                ...exam,
                status: statuses[id],
                regId: 'EXM-' + id + '-98471'
            } : null;
        }).filter(e => e !== null);
    },

    render() {
        return `
            <div class="mb-8 animate-fade-in-up">
                <h1 class="text-2xl font-bold text-gray-900 dark:text-white font-display">My Registered Exams</h1>
                <p class="text-gray-500 dark:text-gray-400 mt-1">Track your exam status, payment, and edit registration info.</p>
            </div>

            <div class="space-y-6 animate-fade-in-up" style="animation-delay: 200ms">
                ${this.registrations.length > 0
                ? this.registrations.map(reg => this.renderExamCard(reg)).join('')
                : '<div class="glass-card p-12 text-center text-gray-400"><i class="fas fa-inbox text-4xl mb-4 opacity-50"></i><p>No registered exams yet. Go to Available Exams to register.</p></div>'
            }
            </div>
        `;
    },

    renderExamCard(reg) {
        return `
            <div class="glass-card p-6 md:p-8 rounded-2xl border-l-4 border-yellow-500 relative overflow-hidden">
                <div class="flex flex-col md:flex-row justify-between items-start md:items-center mb-6 gap-4">
                    <div>
                        <div class="flex items-center gap-3 mb-2">
                            <span class="px-3 py-1 bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-300 text-xs font-bold rounded-lg">${reg.university}</span>
                            <span class="text-xs font-bold text-gray-500 uppercase tracking-widest">ID: ${reg.regId}</span>
                        </div>
                        <h3 class="text-xl font-bold text-gray-900 dark:text-white font-display">${reg.name}</h3>
                        <p class="text-indigo-600 dark:text-indigo-400 font-medium">${reg.subject}</p>
                    </div>
                    
                    <div class="flex flex-col items-end gap-2 text-right">
                        <span class="px-4 py-2 rounded-xl bg-yellow-50 dark:bg-yellow-900/20 text-yellow-600 dark:text-yellow-400 font-bold text-sm border border-yellow-200 dark:border-yellow-800 flex items-center gap-2">
                            <i class="fas fa-clock"></i> Pending Payment
                        </span>
                        <span class="text-sm font-bold text-gray-700 dark:text-gray-300">Amount Due: ₹${reg.fee} (Offline)</span>
                    </div>
                </div>

                <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6 text-sm text-gray-600 dark:text-gray-400 bg-gray-50 dark:bg-gray-800/50 p-4 rounded-xl">
                    <div class="flex items-center gap-2"><i class="far fa-calendar text-center w-4"></i> Exam Date: <strong class="text-gray-900 dark:text-gray-200">${reg.date}</strong></div>
                    <div class="flex items-center gap-2"><i class="fas fa-laptop text-center w-4"></i> Type: <strong class="text-gray-900 dark:text-gray-200">${reg.type}</strong></div>
                </div>

                <div class="flex justify-end gap-3 mt-4 pt-4 border-t border-gray-100 dark:border-gray-700">
                    <button onclick="alert('Viewing detailed information...')" class="px-4 py-2 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg text-sm font-medium transition-all flex items-center gap-2">
                        <i class="fas fa-eye"></i> View Details
                    </button>
                    <button onclick="alert('Opening Edit Form...\\nYou can modify your student or academic info here.')" class="px-4 py-2 bg-indigo-50 dark:bg-indigo-900/20 hover:bg-indigo-100 dark:hover:bg-indigo-900/40 text-indigo-600 dark:text-indigo-400 border border-indigo-200 dark:border-indigo-800 rounded-lg text-sm font-medium transition-all flex items-center gap-2">
                        <i class="fas fa-edit"></i> Edit Info
                    </button>
                </div>
            </div>
        `;
    },

    afterRender() {
        // No special logic needed yet
    }
};
