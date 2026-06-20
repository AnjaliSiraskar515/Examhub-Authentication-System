export default function ExamRegistration() {
    const app = document.getElementById('page-container');
    if (!app) return;

    let exams = [];
    let registeredExamIds = [];
    let currentSelectedExam = null;
    let selectedSubjects = [];
    let studentProfile = {}; // holds real PRN, name, course etc from profile API

    const getToday = () => {
        return new Date().toISOString().split('T')[0];
    };

    const loadExams = async () => {
        try {
            app.innerHTML = `<div class="p-10 text-center"><i class="fas fa-spinner fa-spin text-4xl text-indigo-500"></i><p class="mt-4 text-gray-600">Loading Available Exams...</p></div>`;

            // ── 1. Load student profile (real PRN, name, course)
            const token = localStorage.getItem('token');
            try {
                const profileRes = await fetch('/api/profile/info', {
                    headers: { 'Authorization': token ? `Bearer ${token}` : '' }
                });
                if (profileRes.ok) {
                    studentProfile = await profileRes.json();
                }
            } catch (profErr) {
                console.warn('Could not fetch student profile for registration:', profErr);
            }

            // ── 1.5 Check Eligibility Gate
            try {
                const eligRes = await fetch('/api/student/exams/eligible', {
                    headers: { 'Authorization': token ? `Bearer ${token}` : '' }
                });

                if (eligRes.status === 403) {
                    const errData = await eligRes.json();
                    if (errData.status === 'BLOCKED') {
                        app.innerHTML = `
                            <div class="p-6 max-w-3xl mx-auto mt-12 animate-fade-in-up">
                                <div class="bg-red-50/90 backdrop-blur border border-red-200 p-10 rounded-3xl shadow-xl shadow-red-500/10 text-center relative overflow-hidden">
                                    <div class="absolute -top-10 -right-10 w-40 h-40 bg-red-400/20 rounded-full blur-3xl"></div>
                                    <div class="absolute -bottom-10 -left-10 w-40 h-40 bg-red-400/20 rounded-full blur-3xl"></div>
                                    
                                    <div class="w-24 h-24 bg-red-100 rounded-full flex justify-center items-center mx-auto mb-6 shadow-inner relative z-10">
                                        <i class="fas fa-user-lock text-red-500 text-5xl"></i>
                                    </div>
                                    <h3 class="text-3xl font-extrabold text-red-800 mb-4 font-display relative z-10">Access Blocked</h3>
                                    <p class="text-red-700 text-xl mb-8 font-medium relative z-10">${errData.reason}</p>
                                    
                                    <div class="p-5 bg-white/70 rounded-2xl border border-red-100 mb-8 text-sm text-red-700 font-medium max-w-lg mx-auto relative z-10">
                                        <i class="fas fa-info-circle text-red-500 mr-2 text-lg"></i> Please resolve this issue with the administration to unlock your exam registrations.
                                    </div>
                                    
                                    <button onclick="window.location.reload()" class="px-8 py-3.5 bg-red-600 hover:bg-red-700 text-white rounded-xl font-bold shadow-lg shadow-red-500/30 transition-all flex items-center justify-center mx-auto gap-2 relative z-10 active:scale-95">
                                        <i class="fas fa-sync-alt"></i> Refresh Status
                                    </button>
                                </div>
                            </div>
                        `;
                        return; // Halt execution and prevent loading exams
                    }
                }
            } catch (eligErr) {
                console.warn('Eligibility check failed or skipped', eligErr);
            }

            // ── 2. Fetch open exams
            const response = await fetch('/api/university/exams');
            if (!response.ok) throw new Error("Failed to fetch exams");
            const rawExams = await response.json();
            
            // Filter logic: Only show OPEN exams for student's course, department, and semester
            exams = rawExams.filter(ex => {
                if (ex.status !== 'OPEN') return false;
                
                // If profile is missing course/sem, default to showing the exam to avoid hiding valid exams
                if (!studentProfile.course || !studentProfile.semester) return true;

                const stuCourse = String(studentProfile.course).toLowerCase().trim();
                const stuDept = String(studentProfile.branch || studentProfile.department || '').toLowerCase().trim();
                const stuSemStr = String(studentProfile.semester).toLowerCase().replace(/\D/g, '');
                const stuSem = parseInt(stuSemStr) || 0;
                
                const exCourse = String(ex.course || '').toLowerCase().trim();
                const exDept = String(ex.department || '').toLowerCase().trim();
                const exSemStr = String(ex.semester || '').toLowerCase().replace(/\D/g, '');
                const exSem = parseInt(exSemStr) || 0;
                
                // Helper to check if two strings loosely match (handles B.Tech vs Bachelor of Tech, Comp Sci vs Comp Engg)
                const looseMatch = (str1, str2) => {
                    if (!str1 || !str2) return true;
                    const s1 = str1.toLowerCase().replace(/[^a-z0-9]/g, '');
                    const s2 = str2.toLowerCase().replace(/[^a-z0-9]/g, '');
                    if (s1.includes(s2) || s2.includes(s1)) return true;
                    if (s1.startsWith('btech') && s2.startsWith('bachelor')) return true;
                    if (s2.startsWith('btech') && s1.startsWith('bachelor')) return true;
                    if (s1.startsWith('mtech') && s2.startsWith('master')) return true;
                    if (s2.startsWith('mtech') && s1.startsWith('master')) return true;
                    if (s1.startsWith('comp') && s2.startsWith('comp')) return true;
                    if (s1.startsWith('elec') && s2.startsWith('elec')) return true;
                    return false;
                };

                // Course & Department Match (loose)
                if (!looseMatch(ex.course, studentProfile.course)) return false;
                if (!looseMatch(ex.department, studentProfile.branch || studentProfile.department)) return false;
                
                const isBacklog = (ex.examType || '').toLowerCase().includes('backlog') || 
                                  (ex.examType || '').toLowerCase().includes('supplementary') || 
                                  (ex.sessionName || '').toLowerCase().includes('supplementary') ||
                                  (ex.sessionName || '').toLowerCase().includes('backlog');

                if (isBacklog) {
                    // For backlog/supplementary, student must be in a higher or equal semester
                    if (stuSem > 0 && exSem > 0 && exSem > stuSem) return false;
                    return true;
                } else {
                    // For regular exams, semester must match exactly
                    if (stuSem > 0 && exSem > 0 && exSem !== stuSem) return false;
                    return true;
                }
            });

            // ── 3. Fetch THIS student's registrations (token-based — backend resolves actual user)
            try {
                const regResponse = await fetch(`/api/student/registrations`, {
                    headers: { 'Authorization': token ? `Bearer ${token}` : '' }
                });
                if (regResponse.ok) {
                    const myRegs = await regResponse.json();
                    registeredExamIds = myRegs.map(r => r.examId);
                }
            } catch (regError) {
                console.warn("Could not fetch user registrations", regError);
            }

            render();
        } catch (e) {
            console.error(e);
            app.innerHTML = `<div class="p-10 text-center text-red-500"><i class="fas fa-exclamation-triangle text-4xl mb-4"></i><p>Failed to load exams via API.</p></div>`;
        }
    };

    const checkEligibility = (exam) => {
        const today = getToday();
        const regEnd = exam.registrationWindow?.endDate;
        const lateEnd = exam.registrationWindow?.lateFeeDeadline || regEnd;

        if (!regEnd || today > lateEnd) {
            return { status: 'CLOSED', message: 'Registration Closed' };
        }

        // We can't perfectly check maxStudents without back-end registration count here, 
        // but frontend limits UI for CLOSED. 
        if (today > regEnd && today <= lateEnd) {
            return { status: 'LATE', message: 'Late Fee Applies' };
        }

        return { status: 'OPEN', message: 'Registration Open' };
    };

    const render = () => {
        app.innerHTML = `
            <div class="p-6 max-w-6xl mx-auto animate-fade-in-up">
                
                <!-- Main Listing View -->
                <div id="listing-view">
                    <div class="mb-8">
                        <h1 class="text-2xl font-bold text-gray-800 dark:text-white font-display">Available Exams</h1>
                        <p class="text-gray-600 dark:text-gray-400">Browse and register for your upcoming examinations based on official university scheduling.</p>
                    </div>

                    <div id="registrationAlertContainer" class="hidden mb-6"></div>

                    <!-- Exam Grid -->
                    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6" id="examGrid">
                    </div>
                </div>

                <!-- Registration Detail View -->
                <div id="registration-view" class="hidden max-w-5xl mx-auto">
                    <!-- Dynamic content rendered here -->
                </div>
            </div>
        `;

        updateGrid();
        attachGridListeners();
    };

    const renderExamCards = () => {
        if (exams.length === 0) {
            return `
                <div class="col-span-1 md:col-span-2 lg:col-span-3 py-16 text-center text-gray-400 glass-card rounded-2xl border border-gray-100 dark:border-gray-700">
                    <i class="fas fa-calendar-times text-5xl mb-4 opacity-30 text-indigo-500"></i>
                    <p class="text-xl font-bold text-gray-800 dark:text-white">No Exams Available</p>
                    <p class="mt-1 text-sm text-gray-500">The university has not published any exam sessions yet.<br>Please check back later.</p>
                </div>
            `;
        }

        return exams.map(exam => {
            const eligibility = checkEligibility(exam);
            const regEnd = exam.registrationWindow?.endDate || 'N/A';
            const exDate = exam.schedule?.examDate || 'N/A';
            const limit = exam.controls?.maxStudents ? `Seat Limit: ${exam.controls.maxStudents}` : 'No limit';

            let statusBadge = '';
            let actionHtml = '';

            if (registeredExamIds.includes(exam.id)) {
                statusBadge = `<span class="px-3 py-1 bg-green-100 text-green-700 text-[10px] font-black tracking-widest uppercase rounded-full shadow-sm flex items-center gap-1.5"><i class="fas fa-check-circle animate-pulse"></i> Registered</span>`;
                actionHtml = `
                    <div class="relative w-full text-center group/notice" onclick="
                        const notice = document.getElementById('notice-${exam.id}');
                        notice.classList.remove('opacity-0', 'translate-y-2', 'pointer-events-none');
                        setTimeout(() => notice.classList.add('opacity-0', 'translate-y-2', 'pointer-events-none'), 4000);
                    ">
                        <button class="w-full py-3.5 bg-gray-50/80 text-gray-400 rounded-2xl font-bold border border-gray-200/60 cursor-not-allowed flex justify-center items-center gap-2 pointer-events-none transition-all" disabled>
                            <i class="fas fa-lock"></i> Already Registered
                        </button>
                        <div id="notice-${exam.id}" class="absolute bottom-full left-0 w-full mb-3 p-3 bg-gray-900/90 backdrop-blur text-white text-[11px] leading-tight text-center font-bold rounded-xl shadow-xl opacity-0 translate-y-2 pointer-events-none transition-all duration-300 z-20 flex items-center justify-center gap-2">
                            <i class="fas fa-info-circle text-blue-400"></i> Check 'My Exams' section
                        </div>
                    </div>
                `;
            } else if (eligibility.status === 'CLOSED') {
                statusBadge = `<span class="px-3 py-1 bg-red-100/80 text-red-700 text-[10px] font-black tracking-widest uppercase rounded-full shadow-sm flex items-center gap-1.5"><i class="fas fa-times-circle"></i> ${eligibility.message}</span>`;
                actionHtml = `<button class="w-full py-3.5 bg-gray-100 text-gray-400 rounded-2xl font-bold cursor-not-allowed flex justify-center items-center gap-2" disabled><i class="fas fa-lock"></i> Window Closed</button>`;
            } else {
                statusBadge = eligibility.status === 'LATE'
                    ? `<span class="px-3 py-1 bg-gradient-to-r from-yellow-400 to-orange-400 text-white text-[10px] font-black tracking-widest uppercase rounded-full shadow-sm flex items-center gap-1.5"><i class="fas fa-exclamation-triangle"></i> ${eligibility.message}</span>`
                    : `<span class="px-3 py-1 bg-gradient-to-r from-blue-500 to-indigo-500 text-white text-[10px] font-black tracking-widest uppercase rounded-full shadow-md shadow-blue-500/20 flex items-center gap-1.5"><i class="fas fa-door-open"></i> ${eligibility.message}</span>`;

                actionHtml = `<button class="register-btn w-full py-3.5 bg-gradient-to-r from-indigo-600 to-indigo-500 hover:from-indigo-700 hover:to-indigo-600 text-white rounded-2xl font-bold shadow-lg shadow-indigo-500/30 hover:shadow-indigo-500/50 transition-all duration-300 flex justify-center items-center gap-2 transform active:scale-95 group/btn" data-id="${exam.id}"><span>Register Now</span><i class="fas fa-arrow-right text-indigo-200 group-hover/btn:translate-x-1 transition-transform"></i></button>`;
            }

            return `
                <div class="group relative bg-white dark:bg-gray-800 p-6 sm:p-7 rounded-[2rem] border border-gray-100 dark:border-gray-700 shadow-sm hover:shadow-2xl hover:-translate-y-1.5 transition-all duration-300 flex flex-col h-full overflow-hidden">
                    <!-- Subtle Glow Effect on Hover -->
                    <div class="absolute -top-32 -right-32 w-64 h-64 bg-gradient-to-br from-indigo-400/20 to-purple-400/20 rounded-full blur-3xl transform group-hover:scale-150 transition-transform duration-700 -z-10"></div>
                    
                    <!-- Header: Badge & Mode -->
                    <div class="flex justify-between items-start mb-6 relative z-10 gap-2">
                        <div>${statusBadge}</div>
                        <div class="bg-gray-50 dark:bg-gray-900/50 px-3 py-1.5 rounded-lg border border-gray-100 dark:border-gray-700/50 shrink-0">
                            <span class="text-[9px] font-black text-gray-500 dark:text-gray-400 uppercase tracking-widest flex items-center gap-1.5 block">
                                <i class="fas ${exam.mode === 'ONLINE' ? 'fa-globe text-blue-400' : 'fa-building text-gray-400'}"></i> ${exam.mode || 'N/A'} MODE
                            </span>
                        </div>
                    </div>

                    <!-- Title & Subtitle -->
                    <div class="mb-6 relative z-10 flex-grow">
                        <h3 class="text-xl sm:text-2xl font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-gray-900 to-gray-700 dark:from-white dark:to-gray-300 font-display mb-3 group-hover:from-indigo-600 group-hover:to-purple-600 transition-colors duration-300 leading-snug">${exam.sessionName || 'Unnamed Session'}</h3>
                        <div class="flex flex-wrap items-center gap-2 text-[12px] font-bold text-indigo-600 dark:text-indigo-400">
                            <span class="bg-indigo-50 dark:bg-indigo-900/40 border border-indigo-100 dark:border-indigo-800/60 px-2.5 py-1 rounded-md">${exam.course || 'N/A'}</span>
                            <span class="text-gray-300 dark:text-gray-600">•</span>
                            <span class="bg-indigo-50 dark:bg-indigo-900/40 border border-indigo-100 dark:border-indigo-800/60 px-2.5 py-1 rounded-md">${exam.department || 'N/A'}</span>
                            <span class="text-gray-300 dark:text-gray-600">•</span>
                            <span class="bg-indigo-50 dark:bg-indigo-900/40 border border-indigo-100 dark:border-indigo-800/60 px-2.5 py-1 rounded-md">Sem ${exam.semester || 'N/A'}</span>
                        </div>
                    </div>

                    <!-- Exam Details Grid -->
                    <div class="grid grid-cols-1 gap-2.5 mb-8 text-sm text-gray-600 dark:text-gray-400 bg-gray-50/80 dark:bg-gray-900/30 p-4.5 rounded-2xl border border-gray-100/50 dark:border-gray-700/50 relative z-10 p-4">
                        <div class="flex items-center justify-between">
                            <span class="text-[11px] font-bold text-gray-500 uppercase tracking-wide flex items-center gap-2"><i class="far fa-calendar-alt text-indigo-400"></i> Academic Year</span>
                            <strong class="text-gray-900 dark:text-gray-200">${exam.academicYear || 'N/A'}</strong>
                        </div>
                        <div class="flex items-center justify-between">
                            <span class="text-[11px] font-bold text-gray-500 uppercase tracking-wide flex items-center gap-2"><i class="fas fa-hourglass-half text-orange-400"></i> Registration Ends</span>
                            <strong class="text-gray-900 dark:text-gray-200">${regEnd}</strong>
                        </div>
                        <div class="flex items-center justify-between">
                            <span class="text-[11px] font-bold text-gray-500 uppercase tracking-wide flex items-center gap-2"><i class="fas fa-calendar-day text-blue-500"></i> Exam Date</span>
                            <strong class="text-gray-900 dark:text-gray-200">${exDate}</strong>
                        </div>
                        <div class="flex items-center justify-between mt-1.5 pt-2.5 border-t border-gray-200/50 dark:border-gray-700/50">
                            <span class="text-[10px] font-black uppercase tracking-widest text-gray-400 flex items-center gap-1.5"><i class="fas fa-users"></i> Capacity</span>
                            <span class="text-[11px] font-bold text-gray-500 bg-gray-200/60 dark:bg-gray-700 px-2 py-0.5 rounded">${limit}</span>
                        </div>
                    </div>

                    <!-- Footer Action -->
                    <div class="mt-auto relative z-10 pt-1">
                        ${actionHtml}
                    </div>
                </div>
            `;
        }).join('');
    };

    const updateGrid = () => {
        document.getElementById('examGrid').innerHTML = renderExamCards();
    };

    const attachGridListeners = () => {
        document.getElementById('examGrid').addEventListener('click', (e) => {
            const btn = e.target.closest('.register-btn');
            if (!btn) return;

            const examId = btn.getAttribute('data-id');
            currentSelectedExam = exams.find(ex => ex.id == Number(examId));
            selectedSubjects = [];

            document.getElementById('listing-view').classList.add('hidden');
            renderRegistrationView();
            document.getElementById('registration-view').classList.remove('hidden');
        });
    };

    let regCurrentStep = 1;

    const renderRegistrationView = () => {
        regCurrentStep = 1; // Reset to step 1
        const exam = currentSelectedExam;
        const regView = document.getElementById('registration-view');

        const today = getToday();
        const regEnd = exam.registrationWindow?.endDate;
        const lateFeeActive = today > regEnd;

        let subjectsHtml = '<p class="text-gray-500 text-sm">No subjects defined for this exam yet.</p>';
        if (exam.subjects && exam.subjects.length > 0) {
            subjectsHtml = exam.subjects.map(sub => `
                <div class="subject-item relative p-4 mb-3 border border-gray-100 rounded-xl hover:border-indigo-100 transition-colors flex items-center justify-between group">
                    <div>
                        <h4 class="font-bold text-gray-800">${sub.subjectName || sub.name}</h4>
                        <p class="text-xs text-gray-500 mt-0.5">Code: ${sub.subjectCode || sub.code} | Marks: ${sub.totalMarks || sub.marks} | Type: Regular</p>
                    </div>
                    <label class="relative inline-flex items-center cursor-pointer">
                        <input type="checkbox" class="sr-only peer subject-checkbox" data-id="${sub.id}" data-name="${sub.subjectName || sub.name}">
                        <div class="w-11 h-6 bg-gray-200 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-indigo-600"></div>
                    </label>
                </div>
            `).join('');
        }

        regView.innerHTML = `
            <!-- Header -->
            <div class="mb-6 flex items-center gap-4">
                <button id="backToListBtn" class="w-10 h-10 rounded-full bg-white border border-gray-200 text-gray-600 hover:text-indigo-600 hover:border-indigo-200 shadow-sm flex items-center justify-center transition-all">
                    <i class="fas fa-arrow-left"></i>
                </button>
                <div>
                    <h2 class="text-2xl font-bold text-gray-900">Register: ${exam.sessionName}</h2>
                    <p class="text-sm text-gray-500">Complete the 4-step wizard to register for your exam.</p>
                </div>
            </div>

            <!-- Horizontal Step Indicator -->
            <div class="mb-8 overflow-x-auto">
                <div class="flex items-center justify-between min-w-[600px] mb-2 px-2 relative">
                    <!-- Progress Bar Background -->
                    <div class="absolute left-0 top-1/2 transform -translate-y-1/2 w-full h-1 bg-gray-200" style="z-index: 0;"></div>
                    <!-- Active Progress Bar -->
                    <div id="regProgressBar" class="absolute left-0 top-1/2 transform -translate-y-1/2 h-1 bg-indigo-600 transition-all duration-300" style="width: 0%; z-index: 0;"></div>

                    <!-- Steps -->
                    <div class="relative z-10 flex flex-col items-center step-indicator opacity-100" id="reg-ind-1">
                        <div class="w-10 h-10 rounded-full flex items-center justify-center font-bold border-2 transition-all duration-300 reg-step-circle border-indigo-600 bg-indigo-600 text-white">1</div>
                        <span class="text-xs font-bold mt-2 text-indigo-600">Information</span>
                    </div>
                    <div class="relative z-10 flex flex-col items-center step-indicator opacity-50" id="reg-ind-2">
                        <div class="w-10 h-10 rounded-full flex items-center justify-center font-bold border-2 transition-all duration-300 reg-step-circle border-gray-300 bg-white text-gray-500">2</div>
                        <span class="text-xs font-bold mt-2 text-gray-500">Subjects</span>
                    </div>
                    <div class="relative z-10 flex flex-col items-center step-indicator opacity-50" id="reg-ind-3">
                        <div class="w-10 h-10 rounded-full flex items-center justify-center font-bold border-2 transition-all duration-300 reg-step-circle border-gray-300 bg-white text-gray-500">3</div>
                        <span class="text-xs font-bold mt-2 text-gray-500">Summary</span>
                    </div>
                    <div class="relative z-10 flex flex-col items-center step-indicator opacity-50" id="reg-ind-4">
                        <div class="w-10 h-10 rounded-full flex items-center justify-center font-bold border-2 transition-all duration-300 reg-step-circle border-gray-300 bg-white text-gray-500">4</div>
                        <span class="text-xs font-bold mt-2 text-gray-500">Confirm</span>
                    </div>
                </div>
            </div>

            <div class="bg-white p-6 md:p-8 rounded-2xl shadow-sm border border-gray-100 relative">
                
                <!-- STEP 1: Exam Info -->
                <div id="reg-step1" class="reg-step-content animate-fade-in-up">
                    <h3 class="text-xl font-bold text-gray-900 mb-6 border-b border-gray-100 pb-3"><i class="fas fa-info-circle text-indigo-500 mr-2"></i>1. Exam Information</h3>
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-6 text-sm mb-8">
                        <div class="bg-gray-50 p-4 rounded-xl">
                            <span class="block text-gray-500 mb-1 text-xs uppercase tracking-wider">Session Name</span>
                            <span class="font-bold text-gray-800 text-lg">${exam.sessionName}</span>
                        </div>
                        <div class="bg-gray-50 p-4 rounded-xl">
                            <span class="block text-gray-500 mb-1 text-xs uppercase tracking-wider">Academic Year</span>
                            <span class="font-bold text-gray-800 text-lg">${exam.academicYear}</span>
                        </div>
                        <div class="bg-gray-50 p-4 rounded-xl">
                            <span class="block text-gray-500 mb-1 text-xs uppercase tracking-wider">Course & Semester</span>
                            <span class="font-bold text-gray-800 text-lg">${exam.course} (Sem ${exam.semester})</span>
                        </div>
                        <div class="bg-gray-50 p-4 rounded-xl">
                            <span class="block text-gray-500 mb-1 text-xs uppercase tracking-wider">Mode</span>
                            <span class="font-bold text-gray-800 text-lg">${exam.mode}</span>
                        </div>
                        <div class="bg-gray-50 p-4 rounded-xl">
                            <span class="block text-gray-500 mb-1 text-xs uppercase tracking-wider">Exam Date</span>
                            <span class="font-bold text-gray-800 text-lg">${exam.schedule?.examDate || 'N/A'}</span>
                        </div>
                        <div class="bg-gray-50 p-4 rounded-xl border ${lateFeeActive ? 'border-yellow-300' : 'border-transparent'}">
                            <span class="block text-gray-500 mb-1 text-xs uppercase tracking-wider">Registration End Date</span>
                            <span class="font-bold text-gray-800 text-lg">${regEnd}</span>
                            ${lateFeeActive ? '<span class="text-xs text-yellow-600 block mt-1"><i class="fas fa-exclamation-triangle"></i> Late penalty active</span>' : ''}
                        </div>
                    </div>
                    
                    <!-- Personal Information Block -->
                    <h3 class="text-xl font-bold text-gray-900 mb-6 mt-10 border-b border-gray-100 pb-3">
                        <i class="fas fa-id-card text-indigo-500 mr-2"></i>Personal Information
                    </h3>
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm mb-4">
                        <div class="bg-gray-50/70 p-4 rounded-xl border border-gray-100/80">
                            <span class="block text-gray-400 mb-1.5 text-[10px] uppercase font-black tracking-widest">Full Name</span>
                            <span class="font-bold text-gray-800 text-base">${studentProfile.name || 'Not Provided'}</span>
                        </div>
                        <div class="bg-gray-50/70 p-4 rounded-xl border border-gray-100/80">
                            <span class="block text-gray-400 mb-1.5 text-[10px] uppercase font-black tracking-widest">PRN / Roll Number</span>
                            <span class="font-bold text-gray-800 text-base">${studentProfile.prn || studentProfile.rollNumber || 'Not Provided'}</span>
                        </div>
                        <div class="bg-gray-50/70 p-4 rounded-xl border border-gray-100/80">
                            <span class="block text-gray-400 mb-1.5 text-[10px] uppercase font-black tracking-widest">Email Address</span>
                            <span class="font-bold text-gray-800 text-base">${studentProfile.email || 'Not Provided'}</span>
                        </div>
                        <div class="bg-gray-50/70 p-4 rounded-xl border border-gray-100/80">
                            <span class="block text-gray-400 mb-1.5 text-[10px] uppercase font-black tracking-widest">Mobile Number</span>
                            <span class="font-bold text-gray-800 text-base">${studentProfile.phoneNumber || '+91 - Not Provided'}</span>
                        </div>
                        <div class="bg-gray-50/70 p-4 rounded-xl border border-gray-100/80 md:col-span-2">
                            <span class="block text-gray-400 mb-1.5 text-[10px] uppercase font-black tracking-widest">Institution & Course</span>
                            <span class="font-bold text-gray-800 text-base">${studentProfile.course || exam.course} | ${studentProfile.branch || exam.department} 
                                <span class="bg-indigo-100 text-indigo-700 text-xs px-2 py-0.5 rounded ml-2">Year: ${studentProfile.year || 'N/A'}</span>
                            </span>
                        </div>
                    </div>
                </div>

                <!-- STEP 2: Subjects -->
                <div id="reg-step2" class="hidden reg-step-content animate-fade-in-up">
                    <div class="flex justify-between items-end mb-6 border-b border-gray-100 pb-3">
                        <h3 class="text-xl font-bold text-gray-900"><i class="fas fa-list-check text-indigo-500 mr-2"></i>2. Subject Selection</h3>
                        <span class="text-xs bg-indigo-50 text-indigo-700 font-bold px-3 py-1.5 rounded-full">Select at least one</span>
                    </div>
                    <div class="subjects-list mb-4 max-h-[400px] overflow-y-auto pr-2 custom-scrollbar">
                        ${subjectsHtml}
                    </div>
                    <div class="text-right text-gray-600 font-medium text-sm">
                        Selected: <span id="subject-count-preview" class="text-indigo-600 font-bold">0</span>
                    </div>
                </div>

                <!-- STEP 3: Fee Summary -->
                <div id="reg-step3" class="hidden reg-step-content animate-fade-in-up">
                    <h3 class="text-xl font-bold text-gray-900 mb-6 border-b border-gray-100 pb-3"><i class="fas fa-receipt text-indigo-500 mr-2"></i>3. Fee Summary</h3>
                    
                    <div class="bg-gray-50 rounded-2xl p-6 border border-gray-100 max-w-2xl mx-auto">
                        <div id="fee-breakdown" class="space-y-4 mb-6">
                            <!-- Dynamics populated -->
                        </div>
                        
                        <div class="border-t-2 border-dashed border-gray-200 pt-4 mt-6">
                            <div class="flex justify-between items-center">
                                <span class="text-gray-600 font-bold">Total Amount to Pay</span>
                                <span class="text-4xl font-extrabold text-indigo-600" id="total-fee">₹0</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- STEP 4: Confirm -->
                <div id="reg-step4" class="hidden reg-step-content animate-fade-in-up">
                    <h3 class="text-xl font-bold text-gray-900 mb-6 border-b border-gray-100 pb-3"><i class="fas fa-shield-check text-indigo-500 mr-2"></i>4. Confirmation</h3>
                    
                    <div class="bg-indigo-50 p-6 rounded-2xl border border-indigo-100 max-w-2xl mx-auto mb-8 text-center">
                        <i class="fas fa-clipboard-check text-4xl text-indigo-400 mb-4"></i>
                        <h4 class="text-lg font-bold text-gray-900 mb-2">Ready to Submit</h4>
                        <p class="text-sm text-gray-600">You are about to register for <strong id="confirm-session-name" class="text-gray-900"></strong>.<br>A total of <strong id="confirm-subject-count" class="text-indigo-600"></strong> subjects selected.</p>
                        
                        <div class="mt-6 bg-white p-4 rounded-xl border border-gray-100 text-left">
                            <label class="flex items-start space-x-3 cursor-pointer">
                                <input type="checkbox" id="declaration" class="form-checkbox h-5 w-5 text-indigo-600 mt-0.5 rounded focus:ring-indigo-500">
                                <span class="text-sm text-gray-700">I declare that all subjects chosen are correct and I agree to the university examination rules and guidelines.</span>
                            </label>
                        </div>
                    </div>
                </div>

                <!-- Wizard Controls -->
                <div class="mt-8 pt-6 border-t border-gray-100 flex justify-between items-center">
                    <button id="regBackBtn" class="hidden px-6 py-2.5 rounded-xl border border-gray-300 text-gray-700 font-bold hover:bg-gray-50 transition-colors">
                        <i class="fas fa-arrow-left mr-2"></i> Back
                    </button>
                    <div class="ml-auto flex gap-3">
                        <button id="regNextBtn" class="px-8 py-2.5 rounded-xl bg-indigo-600 text-white font-bold shadow-md shadow-indigo-500/30 hover:bg-indigo-700 transition-colors">
                            Next <i class="fas fa-arrow-right ml-2"></i>
                        </button>
                        <button id="submitRegBtn" disabled class="hidden px-8 py-2.5 rounded-xl bg-green-600 hover:bg-green-700 disabled:bg-gray-400 disabled:shadow-none text-white font-bold shadow-md shadow-green-500/30 transition-all items-center gap-2">
                            <span>Submit Registration</span> <i class="fas fa-check-circle ml-1"></i>
                        </button>
                    </div>
                </div>

            </div>
        `;

        setupRegistrationLogic(lateFeeActive);
    };

    const setupRegistrationLogic = (lateFeeActive) => {
        const exam = currentSelectedExam;
        
        const isBacklog = (exam.examType || '').toLowerCase().includes('backlog') || 
                          (exam.examType || '').toLowerCase().includes('supplementary') || 
                          (exam.sessionName || '').toLowerCase().includes('supplementary') ||
                          (exam.sessionName || '').toLowerCase().includes('backlog');
                          
        const regFee = isBacklog ? (exam.feeStructure?.backlogFee || 0) : (exam.feeStructure?.regularFee || 0);
        const lateFee = exam.feeStructure?.lateFee || 0;

        document.getElementById('backToListBtn').addEventListener('click', () => {
            document.getElementById('registration-view').classList.add('hidden');
            document.getElementById('listing-view').classList.remove('hidden');
        });

        const checkboxes = document.querySelectorAll('.subject-checkbox');
        const declaration = document.getElementById('declaration');
        const breakdownDiv = document.getElementById('fee-breakdown');
        const totalFeeEl = document.getElementById('total-fee');
        const submitBtn = document.getElementById('submitRegBtn');
        const subjectCountPreview = document.getElementById('subject-count-preview');

        let overallTotal = 0;

        const calculateFee = () => {
            selectedSubjects = [];
            checkboxes.forEach(cb => {
                const subItem = cb.closest('.subject-item');
                if (cb.checked) {
                    subItem.classList.add('border-indigo-500', 'bg-indigo-50/20');
                    selectedSubjects.push(cb.getAttribute('data-name'));
                } else {
                    subItem.classList.remove('border-indigo-500', 'bg-indigo-50/20');
                }
            });

            if (subjectCountPreview) subjectCountPreview.textContent = selectedSubjects.length;

            const regTotal = selectedSubjects.length * regFee;
            let lateTotal = lateFeeActive && selectedSubjects.length > 0 ? lateFee : 0;
            overallTotal = regTotal + lateTotal;

            breakdownDiv.innerHTML = `
                <div class="flex justify-between text-gray-600 text-lg">
                    <span>Subjects Selected (${selectedSubjects.length} x ₹${regFee})</span>
                    <span class="font-bold text-gray-800">₹${regTotal}</span>
                </div>
                ${lateFeeActive && selectedSubjects.length > 0 ? `
                <div class="flex justify-between text-red-600 font-medium mt-4 pt-4 border-t border-red-100">
                    <span>Late Fee Penalty Applied</span>
                    <span>₹${lateTotal}</span>
                </div>` : ''}
            `;

            totalFeeEl.textContent = `₹${overallTotal}`;

            // Step 4 Verification Details
            document.getElementById('confirm-session-name').textContent = exam.sessionName;
            document.getElementById('confirm-subject-count').textContent = selectedSubjects.length;

            if (declaration.checked && selectedSubjects.length > 0) {
                submitBtn.disabled = false;
            } else {
                submitBtn.disabled = true;
            }
        };

        checkboxes.forEach(cb => cb.addEventListener('change', calculateFee));
        declaration.addEventListener('change', calculateFee);
        calculateFee();


        // WIZARD CONTROLS LOGIC
        const nextBtn = document.getElementById('regNextBtn');
        const backBtn = document.getElementById('regBackBtn');
        const progressBar = document.getElementById('regProgressBar');

        const updateWizardUI = () => {
            // Validate step transitions
            if (regCurrentStep === 3 && selectedSubjects.length === 0) {
                alert("Please select at least one subject to proceed.");
                regCurrentStep = 2; // block transition
                return;
            }

            // Update Progress Bar (0%, 33%, 66%, 100%)
            progressBar.style.width = `${(regCurrentStep - 1) * 33.33}%`;

            // Update Indicators
            for (let i = 1; i <= 4; i++) {
                const ind = document.getElementById(`reg-ind-${i}`);
                const circle = ind.querySelector('.reg-step-circle');
                const text = ind.querySelector('span');

                if (i < regCurrentStep) {
                    // Completed
                    ind.classList.remove('opacity-50');
                    circle.className = 'w-10 h-10 rounded-full flex items-center justify-center font-bold border-2 transition-all duration-300 reg-step-circle border-indigo-600 bg-indigo-600 text-white';
                    circle.innerHTML = '<i class="fas fa-check"></i>';
                    text.className = 'text-xs font-bold mt-2 text-indigo-600';
                } else if (i === regCurrentStep) {
                    // Current
                    ind.classList.remove('opacity-50');
                    circle.className = 'w-10 h-10 rounded-full flex items-center justify-center font-bold border-2 transition-all duration-300 reg-step-circle border-indigo-600 bg-white text-indigo-600';
                    circle.innerHTML = i;
                    text.className = 'text-xs font-bold mt-2 text-indigo-600';
                } else {
                    // Upcoming
                    ind.classList.add('opacity-50');
                    circle.className = 'w-10 h-10 rounded-full flex items-center justify-center font-bold border-2 transition-all duration-300 reg-step-circle border-gray-300 bg-white text-gray-500';
                    circle.innerHTML = i;
                    text.className = 'text-xs font-bold mt-2 text-gray-500';
                }
            }

            // Hide/Show Step Content
            document.querySelectorAll('.reg-step-content').forEach(el => el.classList.add('hidden'));
            document.getElementById(`reg-step${regCurrentStep}`).classList.remove('hidden');

            // Toggle Buttons
            if (regCurrentStep === 1) {
                backBtn.classList.add('hidden');
                nextBtn.classList.remove('hidden');
                submitBtn.classList.add('hidden');
            } else if (regCurrentStep === 4) {
                backBtn.classList.remove('hidden');
                nextBtn.classList.add('hidden');
                submitBtn.classList.remove('hidden');
                submitBtn.classList.add('flex');
            } else {
                backBtn.classList.remove('hidden');
                nextBtn.classList.remove('hidden');
                submitBtn.classList.add('hidden');
                submitBtn.classList.remove('flex');
            }
        };

        nextBtn.addEventListener('click', () => {
            if (regCurrentStep < 4) {
                regCurrentStep++;
                updateWizardUI();
            }
        });

        backBtn.addEventListener('click', () => {
            if (regCurrentStep > 1) {
                regCurrentStep--;
                updateWizardUI();
            }
        });

        updateWizardUI();

        submitBtn.addEventListener('click', async () => {
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i>Processing...';

            try {
                const payload = {
                    studentId: null, // overridden securely by backend using JWT
                    examId: exam.id,
                    prn: studentProfile.prn || studentProfile.rollNumber || `PRN_PENDING_${Date.now()}`,
                    fullName: studentProfile.name || 'Not Provided',
                    course: studentProfile.course || exam.course || 'Not Provided',
                    year: studentProfile.year || new Date().getFullYear().toString(),
                    examType: "UNIVERSITY",
                    examSession: exam.sessionName,
                    selectedSubjects: selectedSubjects,
                    declarationAccepted: true,
                    totalFee: overallTotal,
                    paymentStatus: 'PENDING'
                };

                const token = localStorage.getItem('token');

                const response = await fetch('/api/student/exams/register', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': token ? `Bearer ${token}` : ''
                    },
                    body: JSON.stringify(payload)
                });

                if (!response.ok) {
                    const errorText = await response.text();
                    // Parse "AlreadyRegistered" error nicely
                    let friendlyMsg = errorText;
                    try {
                        if (errorText && errorText.trim() !== '') {
                            const errJson = JSON.parse(errorText);
                            friendlyMsg = errJson.message || errJson.error || errorText;
                        }
                    } catch (_) { }

                    if (!friendlyMsg || friendlyMsg.trim() === '') {
                        if (response.status === 403) {
                            friendlyMsg = "Registration Blocked by Backend. Ensure your backend server was restarted to apply recent changes!";
                        } else {
                            friendlyMsg = `Server Error ${response.status}`;
                        }
                    }
                    throw new Error(friendlyMsg);
                }

                // Refresh registeredExamIds so the card immediately shows "Already Registered"
                try {
                    const token2 = localStorage.getItem('token');
                    const regRes = await fetch('/api/student/registrations', {
                        headers: { 'Authorization': token2 ? `Bearer ${token2}` : '' }
                    });
                    if (regRes.ok) {
                        const myRegs = await regRes.json();
                        registeredExamIds = myRegs.map(r => r.examId);
                    }
                } catch (_) { }

                // Navigate to My Exams
                window.dispatchEvent(new CustomEvent('navigate', { detail: { page: 'registered' } }));

            } catch (e) {
                console.error("Error registering:", e);
                alert("Registration failed: " + e.message);
            } finally {
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<span>Submit Registration</span> <i class="fas fa-check-circle ml-1"></i>';
            }
        });
    };

    loadExams();
}
