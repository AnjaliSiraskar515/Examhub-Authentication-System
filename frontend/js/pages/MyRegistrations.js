/*
 * js/pages/MyRegistrations.js
 * Student Dashboard - My Registrations View
 */

export default function MyRegistrations() {
    const app = document.getElementById('page-container');

    const fetchAndRender = async () => {
        // Show loading state
        app.innerHTML = `
            <div class="p-6 max-w-7xl mx-auto flex justify-center items-center h-64">
                <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
            </div>
        `;

        try {
            const token = localStorage.getItem('token');
            // Using studentId=1 for testing matching ExamRegistration.js payload
            const response = await fetch('http://localhost:8081/api/student/registrations?studentId=1', {
                headers: {
                    'Authorization': token ? `Bearer ${token}` : ''
                }
            });

            if (!response.ok) {
                throw new Error("Failed to fetch registrations");
            }

            const myRegistrations = await response.json();

            let content = '';

            if (myRegistrations.length === 0) {
                content = `
                    <div class="col-span-full py-16 text-center text-gray-400 glass-card rounded-2xl border border-gray-100 dark:border-gray-700 bg-white dark:bg-gray-800">
                        <i class="fas fa-clipboard-list text-5xl mb-4 opacity-30 text-indigo-500"></i>
                        <p class="text-xl font-bold text-gray-800 dark:text-white">No Registrations Found</p>
                        <p class="mt-1 text-sm text-gray-500">You have not registered for any exam sessions.</p>
                    </div>
                `;
            } else {
                const regCards = myRegistrations.map(reg => {
                    let statusLabel = '';
                    let statusColor = '';
                    let statusIcon = '';

                    if (reg.registrationStatus === 'APPROVED') {
                        statusLabel = 'APPROVED';
                        statusColor = 'bg-green-100 text-green-700 border-green-200 dark:bg-green-900/30 dark:text-green-400 dark:border-green-800';
                        statusIcon = 'fa-check-circle';
                    } else if (reg.registrationStatus === 'REJECTED') {
                        statusLabel = 'REJECTED';
                        statusColor = 'bg-red-100 text-red-700 border-red-200 dark:bg-red-900/30 dark:text-red-400 dark:border-red-800';
                        statusIcon = 'fa-times-circle';
                    } else if (reg.registrationStatus === 'APPLIED') {
                        statusLabel = 'PENDING APPROVAL';
                        statusColor = 'bg-blue-100 text-blue-700 border-blue-200 dark:bg-blue-900/30 dark:text-blue-400 dark:border-blue-800';
                        statusIcon = 'fa-info-circle';
                    } else {
                        statusLabel = 'UNKNOWN';
                        statusColor = 'bg-gray-100 text-gray-700 border-gray-200 dark:bg-gray-900/30 dark:text-gray-400 dark:border-gray-800';
                        statusIcon = 'fa-clock';
                    }

                    const subjects = reg.selectedSubjects && reg.selectedSubjects.length > 0
                        ? reg.selectedSubjects.join(', ')
                        : 'No subjects recorded';

                    return `
                        <div class="group relative bg-white dark:bg-gray-800 rounded-3xl p-6 md:p-8 shadow-sm hover:shadow-2xl transition-all duration-300 border border-gray-100 dark:border-gray-700 overflow-hidden transform hover:-translate-y-1">
                            <!-- Gradient Accent Background -->
                            <div class="absolute top-0 right-0 w-64 h-64 bg-gradient-to-br from-${reg.registrationStatus === 'APPROVED' ? 'green' : 'indigo'}-400/10 to-transparent rounded-full blur-3xl -z-10 transform group-hover:scale-125 transition-transform duration-700"></div>
                            
                            <!-- Header row: Course/Tags & Status -->
                            <div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-6">
                                <div class="flex flex-wrap items-center gap-2 md:gap-3">
                                    <span class="px-4 py-1.5 bg-gradient-to-r from-gray-100 to-gray-50 dark:from-gray-700 dark:to-gray-800 text-gray-700 dark:text-gray-200 text-xs font-black tracking-wider uppercase rounded-xl shadow-sm border border-gray-200 dark:border-gray-600">${reg.course || 'Course'}</span>
                                    <div class="flex items-center gap-2 text-[11px] font-bold text-gray-500 uppercase tracking-widest bg-gray-50 dark:bg-gray-900/50 px-3 py-1.5 rounded-lg border border-gray-100 dark:border-gray-700/50">
                                        <i class="fas fa-fingerprint text-indigo-400"></i> ${reg.prn}
                                    </div>
                                    <div class="flex items-center gap-2 text-[11px] font-bold text-gray-500 uppercase tracking-widest bg-gray-50 dark:bg-gray-900/50 px-3 py-1.5 rounded-lg border border-gray-100 dark:border-gray-700/50">
                                        <i class="fas fa-barcode text-indigo-400"></i> REG: EXM-${reg.id}
                                    </div>
                                </div>
                                <div class="flex flex-col items-start md:items-end gap-1.5">
                                    <span class="px-4 py-2 rounded-xl font-bold text-xs uppercase tracking-wide flex items-center gap-2 shadow-sm ${statusColor} border group-hover:shadow-md transition-shadow">
                                        <i class="fas ${statusIcon} animate-pulse"></i> ${statusLabel}
                                    </span>
                                </div>
                            </div>
            
                            <!-- Main Content: Titles & Subjects -->
                            <div class="mb-8">
                                <h3 class="text-2xl md:text-3xl font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-gray-900 to-gray-600 dark:from-white dark:to-gray-300 font-display mb-2 drop-shadow-sm group-hover:from-indigo-600 group-hover:to-purple-600 transition-all duration-300">${reg.examSession || 'Unnamed Session'}</h3>
                                <div class="flex items-start gap-2 mt-3">
                                    <i class="fas fa-book text-indigo-500 mt-1 drop-shadow-sm"></i>
                                    <p class="text-indigo-600 dark:text-indigo-400 font-semibold text-sm md:text-base leading-relaxed break-words">${subjects}</p>
                                </div>
                            </div>

                            <!-- Detailed Metadata Grid -->
                            <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4 mb-8 bg-gray-50/50 dark:bg-gray-900/30 p-5 rounded-2xl border border-gray-100/50 dark:border-gray-700/50 backdrop-blur-sm group-hover:bg-gray-50 dark:group-hover:bg-gray-800 transition-colors">
                                <div class="flex flex-col gap-1">
                                    <span class="text-[10px] font-black uppercase tracking-widest text-gray-400 flex items-center gap-1.5"><i class="far fa-calendar-alt text-indigo-400"></i> Applied Date</span>
                                    <strong class="text-gray-800 dark:text-gray-200 font-medium">${reg.appliedDate || 'N/A'}</strong>
                                </div>
                                <div class="flex flex-col gap-1">
                                    <span class="text-[10px] font-black uppercase tracking-widest text-gray-400 flex items-center gap-1.5"><i class="fas fa-layer-group text-indigo-400"></i> Exam Type</span>
                                    <strong class="text-gray-800 dark:text-gray-200 font-medium">${reg.examType || 'Regular'}</strong>
                                </div>
                                <div class="flex flex-col gap-1">
                                    <span class="text-[10px] font-black uppercase tracking-widest text-gray-400 flex items-center gap-1.5"><i class="fas fa-wallet text-indigo-400"></i> Payment Status</span>
                                    <strong class="text-gray-800 dark:text-gray-200 font-medium flex items-center gap-2">
                                        ₹${reg.totalFee || '0'} 
                                        <span class="text-[10px] px-2 py-0.5 rounded uppercase ${reg.paymentStatus === 'PAID' ? 'bg-green-100 text-green-700' : 'bg-orange-100 text-orange-700'}">${reg.paymentStatus || 'PENDING'}</span>
                                    </strong>
                                </div>
                            </div>
                            
                            <!-- Action Buttons -->
                            <div class="flex flex-col sm:flex-row justify-end gap-3 pt-6 border-t border-gray-100 dark:border-gray-700/50 relative z-10">
                                <button onclick="alert('Viewing detailed information...')" class="px-6 py-2.5 bg-white dark:bg-gray-800 border border-gray-200 hover:border-gray-300 dark:border-gray-600 dark:hover:border-gray-500 hover:shadow-md text-gray-700 dark:text-gray-300 rounded-xl text-sm font-bold transition-all duration-200 flex items-center justify-center gap-2 transform active:scale-95">
                                    <i class="fas fa-eye text-gray-400"></i> View Form
                                </button>
                                <button onclick="alert('Downloading Hall Ticket...')" class="px-6 py-2.5 bg-gradient-to-r ${reg.registrationStatus === 'APPROVED' ? 'from-indigo-600 to-indigo-500 hover:from-indigo-700 hover:to-indigo-600 text-white shadow-indigo-500/25 shadow-lg' : 'from-indigo-100 to-indigo-50 dark:from-indigo-900/30 dark:to-indigo-800/30 text-indigo-400 dark:text-indigo-600 cursor-not-allowed border border-indigo-200 dark:border-indigo-800/50'} rounded-xl text-sm font-bold transition-all duration-200 flex items-center justify-center gap-2 transform active:scale-95" ${reg.registrationStatus !== 'APPROVED' ? 'disabled' : ''}>
                                    <i class="fas fa-file-download ${reg.registrationStatus === 'APPROVED' ? 'text-indigo-200' : ''}"></i> Hall Ticket
                                </button>
                            </div>
                        </div>
                    `;
                }).join('');

                content = `
                    <div class="space-y-6 animate-fade-in-up md:grid md:grid-cols-2 md:space-y-0 gap-6" style="animation-delay: 200ms">
                        ${regCards}
                    </div>
                `;
            }

            app.innerHTML = `
                <div class="p-6 max-w-7xl mx-auto animate-fade-in-up">
                    <div class="mb-8">
                        <h1 class="text-2xl font-bold text-gray-800 dark:text-white font-display">My Registrations</h1>
                        <p class="text-gray-600 dark:text-gray-400">Track and manage your university examination enrollments.</p>
                    </div>
                    ${content}
                </div>
            `;
        } catch (error) {
            console.error("Error loading registrations:", error);
            app.innerHTML = `
                <div class="p-6 max-w-7xl mx-auto text-center">
                    <div class="inline-block p-4 rounded-xl bg-red-50 text-red-600 border border-red-200">
                        <i class="fas fa-exclamation-triangle text-2xl mb-2"></i>
                        <p class="font-bold">Failed to load registrations.</p>
                        <p class="text-sm mt-1">${error.message}</p>
                    </div>
                </div>
            `;
        }
    };

    fetchAndRender();
}
