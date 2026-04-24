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
            <div id="registration-modal-container"></div>
        `;

        try {
            const token = localStorage.getItem('token');
            const headers = { 'Authorization': token ? `Bearer ${token}` : '' };
            // Fetch registrations, student profile, and exams concurrently
            const [regResponse, profileResponse, examsResponse] = await Promise.all([
                fetch('/api/student/registrations?studentId=1', { headers }),
                fetch('/api/profile/info', { headers }),
                fetch('/api/exam/all', { headers })
            ]);

            if (!regResponse.ok) {
                throw new Error("Failed to fetch registrations");
            }

            const myRegistrations = await regResponse.json();
            window.myRegistrationsData = myRegistrations;

            if (profileResponse.ok) {
                window.studentProfileInfo = await profileResponse.json();
            } else {
                window.studentProfileInfo = null;
            }

            if (examsResponse.ok) {
                window.examsData = await examsResponse.json();
            } else {
                window.examsData = [];
            }

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
                            
                            <!-- Header row -->
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
        
                            <!-- Main Content -->
                            <div class="mb-8">
                                <h3 class="text-2xl md:text-3xl font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-gray-900 to-gray-700 dark:from-white dark:to-gray-300 font-display mb-2 drop-shadow-sm group-hover:from-indigo-600 group-hover:to-purple-600 transition-all duration-300">${reg.examSession || 'Unnamed Session'}</h3>
                                <div class="flex items-start gap-2 mt-3">
                                    <i class="fas fa-book text-indigo-500 mt-1 drop-shadow-sm"></i>
                                    <p class="text-indigo-600 dark:text-indigo-400 font-semibold text-sm md:text-base leading-relaxed break-words">${subjects}</p>
                                </div>
                            </div>

                            <!-- Metadata Grid -->
                            <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4 mb-8 bg-gray-50/50 dark:bg-gray-900/30 p-5 rounded-2xl border border-gray-100/50 dark:border-gray-700/50 backdrop-blur-sm">
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
                                <button onclick="window.viewRegistrationForm(${reg.id})" class="px-6 py-2.5 bg-white dark:bg-gray-800 border border-gray-200 hover:border-gray-300 dark:border-gray-600 dark:hover:border-gray-500 hover:shadow-md text-gray-700 dark:text-gray-300 rounded-xl text-sm font-bold transition-all duration-200 flex items-center justify-center gap-2 transform active:scale-95">
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
                    <div id="registration-modal-container"></div>
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

    window.viewRegistrationForm = (regId) => {
        const reg = window.myRegistrationsData.find(r => r.id === regId);
        const profile = window.studentProfileInfo || {};
        const exam = window.examsData ? window.examsData.find(e => e.examId === reg.examId) : null;
        if (!reg) return;

        const modalOverlay = document.createElement('div');
        modalOverlay.id = 'reg-form-modal';
        modalOverlay.className = 'fixed inset-0 z-[100] bg-gray-50 overflow-y-auto flex flex-col';

        let statusBg, statusText, statusIcon, statusLabel, subtext;
        if (reg.registrationStatus === 'APPROVED') {
            statusBg = 'bg-[#e8f5e9] border-[#c8e6c9]';
            statusText = 'text-green-700';
            statusIcon = 'text-green-600';
            statusLabel = 'REGISTRATION APPROVED';
            subtext = 'Your application has been verified by the Controller of Examinations.';
        } else if (reg.registrationStatus === 'REJECTED') {
            statusBg = 'bg-red-50 border-red-200';
            statusText = 'text-red-700';
            statusIcon = 'text-red-600';
            statusLabel = 'REGISTRATION REJECTED';
            subtext = 'Your application was declined. Please check with the administration.';
        } else {
            statusBg = 'bg-blue-50 border-blue-200';
            statusText = 'text-blue-700';
            statusIcon = 'text-blue-600';
            statusLabel = 'APPLICATION PENDING';
            subtext = 'Your application is currently being processed.';
        }

        const subjectsCredits = [4, 4, 3, 3, 2, 2];
        const subjectsHtml = (reg.selectedSubjects || []).map((s, idx) => {
            const isLab = s.toLowerCase().includes('lab') || s.toLowerCase().includes('practical');
            const code = isLab ? `IT40${idx + 1}P` : `IT40${idx + 1}`;
            const creditValue = subjectsCredits[idx] !== undefined ? subjectsCredits[idx] : (isLab ? 2 : 4);
            return `
                <tr class="border-b border-gray-100 last:border-0 hover:bg-gray-50/50">
                    <td class="py-4 font-bold text-indigo-600">${code}</td>
                    <td class="py-4 text-gray-800 font-medium whitespace-nowrap">${s}</td>
                    <td class="py-4">
                        <span class="text-[10px] font-bold uppercase tracking-widest border border-gray-200 text-gray-500 px-3 py-1 rounded shadow-sm">${isLab ? 'PRACTICAL' : 'THEORY'}</span>
                    </td>
                    <td class="py-4 font-bold text-gray-700 text-center">${creditValue}</td>
                    <td class="py-4 text-right">
                        <span class="text-xs font-bold text-green-600 flex items-center justify-end gap-1"><i class="far fa-check-circle"></i> VERIFIED</span>
                    </td>
                </tr>
            `;
        }).join('');

        modalOverlay.innerHTML = `
            <!-- Top App Bar -->
            <div class="bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between sticky top-0 z-10 shadow-sm">
                <button onclick="document.getElementById('reg-form-modal').remove()" class="text-gray-600 hover:text-black transition-colors font-bold text-lg">
                    <i class="fas fa-chevron-left mr-2"></i> Exam Portal
                </button>
                <div class="flex items-center gap-4 text-sm font-bold">
                    <button onclick="window.print()" class="text-gray-500 hover:text-gray-800 flex items-center gap-2">
                        <i class="fas fa-print"></i> Print
                    </button>
                    <button onclick="alert('Downloading Document...')" class="bg-indigo-600 hover:bg-indigo-700 text-white px-5 py-2.5 rounded-lg flex items-center gap-2 shadow-sm transition-colors">
                        <i class="fas fa-download"></i> Download PDF
                    </button>
                </div>
            </div>

            <div class="p-6 md:p-8 max-w-7xl mx-auto w-full flex-grow space-y-6">
                <!-- Status Banner -->
                <div class="${statusBg} border rounded-2xl p-5 flex items-center justify-between shadow-sm">
                    <div class="flex items-center gap-4">
                        <div class="w-10 h-10 rounded-full border border-green-200 bg-white flex items-center justify-center ${statusIcon} shadow-sm text-lg">
                            <i class="fas fa-check"></i>
                        </div>
                        <div>
                            <h3 class="${statusText} font-bold tracking-tight">${statusLabel}</h3>
                            <p class="${statusText} opacity-80 text-sm font-medium">${subtext}</p>
                        </div>
                    </div>
                    <div class="bg-white border px-4 py-2 rounded-full text-xs font-bold text-green-700 flex items-center gap-2 shadow-sm">
                        <i class="far fa-check-circle"></i> Verified Application
                    </div>
                </div>

                <div class="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
                    <!-- Left Column -->
                    <div class="lg:col-span-8 flex flex-col gap-8">
                        
                        <!-- Information Card -->
                        <div class="bg-white rounded-[1.5rem] border border-gray-100 shadow-sm overflow-hidden flex flex-col">
                            <!-- Session Hero Header -->
                            <div class="bg-[#313380] text-white p-8 md:p-10">
                                <span class="text-[10px] font-black uppercase tracking-[0.2em] text-white/50 mb-2 block">Examination Session</span>
                                <h1 class="text-3xl lg:text-4xl font-black mb-6 tracking-tight">${exam ? exam.examName : (reg.examSession || 'Mid-Semester Examination - April 2026')}</h1>
                                <div class="flex items-center gap-6 text-sm font-medium text-white/80">
                                    <span class="flex items-center gap-2"><i class="far fa-calendar-alt"></i> 2025-2026</span>
                                    <span class="flex items-center gap-2"><i class="fas fa-map-marker-alt"></i> ${exam ? exam.location : (reg.institutionName || 'School of Engineering &amp; Technology, Main Campus')}</span>
                                </div>
                            </div>

                            <!-- Details -->
                            <div class="p-8 md:p-10 grid grid-cols-1 md:grid-cols-2 gap-x-12 gap-y-10">
                                <!-- Personal Details -->
                                <div>
                                    <h4 class="text-xs font-black text-gray-400 uppercase tracking-widest border-b border-gray-100 pb-3 mb-6 flex items-center gap-3">
                                        <i class="far fa-user text-gray-300 text-lg"></i> Personal Details
                                    </h4>
                                    <div class="space-y-6">
                                        <div>
                                            <span class="block text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1.5">Full Name</span>
                                            <span class="block text-gray-900 font-bold uppercase">${reg.fullName || ''}</span>
                                        </div>
                                        <div>
                                            <span class="block text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1.5">PRN Number</span>
                                            <span class="block text-gray-900 font-bold">${reg.prn || ''}</span>
                                        </div>
                                        <div>
                                            <span class="block text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1.5">Roll Number</span>
                                            <span class="block text-gray-900 font-bold">${profile.rollNumber || profile.roll || ''}</span>
                                        </div>
                                        <div>
                                            <span class="block text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1.5">University Email</span>
                                            <span class="block text-gray-900 font-bold lowercase">${profile.email || ''}</span>
                                        </div>
                                    </div>
                                </div>

                                <!-- Academic Details -->
                                <div>
                                    <h4 class="text-xs font-black text-gray-400 uppercase tracking-widest border-b border-gray-100 pb-3 mb-6 flex items-center gap-3">
                                        <i class="fas fa-book-open text-gray-300 text-lg"></i> Academic Details
                                    </h4>
                                    <div class="space-y-6">
                                        <div>
                                            <span class="block text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1.5">Program</span>
                                            <span class="block text-gray-900 font-bold">${profile.course || reg.course || ''}</span>
                                        </div>
                                        <div>
                                            <span class="block text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1.5">Branch</span>
                                            <span class="block text-gray-900 font-bold">${profile.branch || ''}</span>
                                        </div>
                                        <div>
                                            <span class="block text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1.5">Current Semester</span>
                                            <span class="block text-gray-900 font-bold">${profile.semester || profile.currentSemester || ''}</span>
                                        </div>
                                        <div>
                                            <span class="block text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1.5">Registration ID</span>
                                            <span class="block text-gray-900 font-bold uppercase">REG-${reg.id}</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Course Selection Table -->
                        <div class="bg-white rounded-[1.5rem] border border-gray-100 shadow-sm p-8 md:p-10 mb-8">
                            <div class="flex items-center justify-between mb-8">
                                <div class="flex items-center gap-3">
                                    <i class="far fa-file-alt text-indigo-700 text-2xl"></i>
                                    <h2 class="text-xl font-black text-[#0f172a]">Course Selection</h2>
                                </div>
                                <span class="bg-gray-100/80 text-gray-600 px-4 py-1.5 rounded-full text-xs font-bold">${(reg.selectedSubjects || []).length} Subjects Total</span>
                            </div>

                            <div class="overflow-x-auto">
                                <table class="w-full text-left text-sm">
                                    <thead class="text-[10px] font-black text-gray-400 uppercase tracking-widest border-b border-gray-100">
                                        <tr>
                                            <th class="pb-4 font-extrabold">Code</th>
                                            <th class="pb-4 font-extrabold">Subject Name</th>
                                            <th class="pb-4 font-extrabold">Type</th>
                                            <th class="pb-4 text-center font-extrabold">Credits</th>
                                            <th class="pb-4 text-right font-extrabold">Status</th>
                                        </tr>
                                    </thead>
                                    <tbody class="divide-y divide-gray-50">
                                        ${subjectsHtml}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>

                    <!-- Right Column -->
                    <div class="lg:col-span-4 flex flex-col gap-6">
                        
                        <!-- Payment Card -->
                        <div class="bg-white rounded-3xl border border-gray-100 p-8 shadow-sm">
                            <div class="flex items-center justify-between mb-10">
                                <div class="flex items-center gap-3 text-sm font-bold text-[#0f172a]">
                                    <i class="far fa-credit-card text-indigo-700 text-lg"></i> Payment
                                </div>
                                <span class="text-[10px] font-black text-green-700 bg-[#e8f5e9] px-3 py-1 rounded shadow-sm uppercase tracking-widest border border-[#c8e6c9]">CAPTURED</span>
                            </div>
                            
                            <div class="space-y-8">
                                <div class="flex justify-between items-center bg-gray-50/50 -mx-8 px-8 py-3">
                                    <span class="text-xs font-bold text-gray-500">Amount Paid</span>
                                    <span class="text-xl font-black text-gray-900">₹${reg.totalFee || '1,200.00'}</span>
                                </div>
                                
                                <div class="space-y-4">
                                    <div class="flex justify-between items-center">
                                        <span class="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Transaction ID</span>
                                        <span class="text-[11px] font-bold text-gray-600">TXN_${99210045231 + reg.id}</span>
                                    </div>
                                    <div class="flex justify-between items-center">
                                        <span class="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Date</span>
                                        <span class="text-xs font-bold text-gray-600">${reg.appliedDate || 'N/A'}</span>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Need Help Card -->
                        <div class="bg-[#f0f4ff] rounded-3xl p-8 border border-indigo-50 shadow-sm">
                            <div class="flex items-start gap-4">
                                <div class="w-12 h-12 bg-indigo-600 rounded-[14px] flex items-center justify-center text-white flex-shrink-0 shadow-sm text-xl">
                                    <i class="fas fa-exclamation"></i>
                                </div>
                                <div class="space-y-3">
                                    <h4 class="font-black text-indigo-900 tracking-tight">Need help?</h4>
                                    <p class="text-xs text-indigo-700 leading-relaxed font-medium pb-2 border-b border-indigo-200/50">
                                        If you find any discrepancy in the data above, please contact the IT Support desk or visit the Exam Controller's office before March 30.
                                    </p>
                                    <a href="#" class="inline-block text-sm font-bold text-indigo-700 hover:text-indigo-900 mt-2 underline decoration-indigo-300 underline-offset-4">Raise a Query</a>
                                </div>
                            </div>
                        </div>

                        <!-- Footer Text -->
                        <div class="px-4 text-center lg:text-right mt-4">
                            <p class="text-[10px] font-bold text-gray-400 italic mb-1">This is a computer generated document.</p>
                            <p class="text-[10px] font-bold text-gray-400 italic">Signature of authorized officer is digitally embedded.</p>
                        </div>
                    </div>
                </div>
            </div>
        `;

        document.getElementById('registration-modal-container').appendChild(modalOverlay);
    };

    fetchAndRender();
}
