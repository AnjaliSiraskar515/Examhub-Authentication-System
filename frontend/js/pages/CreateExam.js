/* js/pages/CreateExam.js */

export default function CreateExam(passedExamId = null) {
    const app = document.getElementById('app');
    if (!app) return;

    let currentStep = 1;
    let examId = passedExamId;
    let subjectsCount = 0;

    // Auth-aware fetch helper
    const authFetch = (url, options = {}) => {
        const token = localStorage.getItem('token');
        const headers = {
            'Content-Type': 'application/json',
            ...(options.headers || {}),
            ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        };
        return fetch(url, { ...options, headers });
    };

    /**
     * Helper to parse source-prefixed IDs (e.g. UNIV_1003, LEGACY_4)
     * and route to the correct API endpoint.
     */
    const resolveExamApi = (id) => {
        if (!id) return null;
        const idStr = String(id);
        if (idStr.startsWith('LEGACY_')) {
            return {
                source: 'LEGACY',
                numericId: idStr.substring(7),
                url: `http://localhost:8080/api/exam/${idStr.substring(7)}`
            };
        } else if (idStr.startsWith('UNIV_')) {
            return {
                source: 'UNIVERSITY',
                numericId: idStr.substring(5),
                url: `http://localhost:8080/api/university/exams/${idStr.substring(5)}`
            };
        }
        // Fallback for plain IDs (assumed University context in this wizard)
        return {
            source: 'UNIVERSITY',
            numericId: idStr,
            url: `http://localhost:8080/api/university/exams/${idStr}`
        };
    };

    const render = () => {
        app.innerHTML = `
            <div class="p-6 max-w-5xl mx-auto animate-fade-in-up">
                <!-- Back Button -->
                <div class="mb-4">
                    <button id="backToExamsBtn" type="button" class="inline-flex items-center gap-2 text-sm font-semibold text-indigo-600 hover:text-indigo-800 dark:text-indigo-400 dark:hover:text-indigo-200 transition-colors bg-indigo-50 hover:bg-indigo-100 dark:bg-indigo-900/30 dark:hover:bg-indigo-800/50 px-4 py-2 rounded-lg border border-indigo-200 dark:border-indigo-700 shadow-sm">
                        <i class="fas fa-arrow-left"></i> Back to Exams
                    </button>
                </div>
                <div class="mb-8 text-center">
                    <h1 class="text-3xl font-extrabold text-gray-900 dark:text-white mb-3">University Exam Creation Wizard</h1>
                    <p class="text-gray-600 dark:text-gray-400 max-w-2xl mx-auto">Follow this multi-step process to configure academic details, schedules, and registration rules for a new examination cycle.</p>
                </div>

                <!-- Toast Notification -->
                <div id="toastNotification" class="hidden fixed top-5 right-5 z-50 transition-all transform translate-y-[-100%] opacity-0 bg-green-50 text-green-800 border-l-4 border-green-500 shadow-xl rounded py-3 px-6 flex items-center gap-3">
                    <i class="fas fa-check-circle text-xl text-green-500"></i>
                    <div>
                        <h4 id="toastTitle" class="font-bold text-sm mb-0">Success</h4>
                        <p id="toastMessage" class="mb-0 text-xs opacity-90"></p>
                    </div>
                </div>

                <!-- Step Indicator -->
                <div class="mb-10 px-4 md:px-0">
                    <div class="flex flex-col md:flex-row justify-between relative">
                        <!-- Progress Line -->
                        <div class="hidden md:block absolute top-[50%] left-0 w-full h-[3px] bg-gray-200 dark:bg-gray-700 -z-10 translate-y-[-50%]"></div>
                        <div id="progressLine" class="hidden md:block absolute top-[50%] left-0 h-[3px] bg-indigo-600 dark:bg-indigo-500 -z-10 translate-y-[-50%] transition-all duration-500 w-[0%]"></div>
                        
                        <!-- Step Dots -->
                        <div class="flex items-center gap-3 md:flex-col md:items-center relative z-10 step-indicator" data-step="1">
                            <div class="step-circle w-10 h-10 rounded-full bg-indigo-600 text-white font-bold flex items-center justify-center border-4 border-white dark:border-gray-900 shadow-md">1</div>
                            <span class="text-xs font-bold text-indigo-700 dark:text-indigo-400 mt-2">Session Info</span>
                        </div>
                        <div class="flex items-center gap-3 md:flex-col md:items-center relative z-10 step-indicator opacity-50 transition-all duration-300" data-step="2">
                            <div class="step-circle w-10 h-10 rounded-full bg-gray-200 text-gray-500 dark:bg-gray-700 dark:text-gray-400 font-bold flex items-center justify-center border-4 border-white dark:border-gray-900 shadow-sm transition-colors duration-500">2</div>
                            <span class="text-xs font-bold text-gray-500 dark:text-gray-400 mt-2 transition-colors duration-500">Subjects</span>
                        </div>
                        <div class="flex items-center gap-3 md:flex-col md:items-center relative z-10 step-indicator opacity-50 transition-all duration-300" data-step="3">
                            <div class="step-circle w-10 h-10 rounded-full bg-gray-200 text-gray-500 dark:bg-gray-700 dark:text-gray-400 font-bold flex items-center justify-center border-4 border-white dark:border-gray-900 shadow-sm transition-colors duration-500">3</div>
                            <span class="text-xs font-bold text-gray-500 dark:text-gray-400 mt-2 transition-colors duration-500">Schedule</span>
                        </div>
                        <div class="flex items-center gap-3 md:flex-col md:items-center relative z-10 step-indicator opacity-50 transition-all duration-300" data-step="4">
                            <div class="step-circle w-10 h-10 rounded-full bg-gray-200 text-gray-500 dark:bg-gray-700 dark:text-gray-400 font-bold flex items-center justify-center border-4 border-white dark:border-gray-900 shadow-sm transition-colors duration-500">4</div>
                            <span class="text-xs font-bold text-gray-500 dark:text-gray-400 mt-2 transition-colors duration-500">Details & Fees</span>
                        </div>
                    </div>
                </div>

                <form id="wizardForm" class="bg-white dark:bg-gray-800 p-8 rounded-2xl shadow-xl border border-gray-100 dark:border-gray-700 transition-all duration-300 relative overflow-hidden">
                    
                    <!-- Loading Overlay -->
                    <div id="loadingOverlay" class="hidden absolute inset-0 bg-white/80 dark:bg-gray-800/80 backdrop-blur-sm z-50 flex flex-col items-center justify-center">
                        <div class="spinner-border text-indigo-600 w-[3rem] h-[3rem]" role="status"></div>
                        <p class="mt-4 font-bold text-indigo-700 dark:text-indigo-400">Saving Configuration...</p>
                    </div>

                    <!-- STEP 1: Session Information -->
                    <div id="step1" class="step-content block animate-fade-in">
                        <h3 class="text-xl font-bold text-gray-800 dark:text-white border-b-2 border-indigo-100 dark:border-gray-700 pb-3 mb-6"><i class="fas fa-info-circle text-indigo-500 mr-2"></i>1. Session Information</h3>
                        
                        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <div>
                                <label class="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-1">Exam Session Name <span class="text-red-500">*</span></label>
                                <input type="text" id="sessionName" name="sessionName" class="w-full bg-gray-50 dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-indigo-500 focus:border-indigo-500 p-2.5" placeholder="e.g. Winter 2026" required>
                            </div>
                            <div>
                                <label class="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-1">Academic Year <span class="text-red-500">*</span></label>
                                <select id="academicYear" name="academicYear" class="w-full bg-gray-50 dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-indigo-500 focus:border-indigo-500 p-2.5" required>
                                    <option value="">Select Year</option>
                                    <option value="2024-25">2024-25</option>
                                    <option value="2025-26">2025-26</option>
                                    <option value="2026-27">2026-27</option>
                                </select>
                            </div>
                            <div>
                                <label class="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-1">Exam Type <span class="text-red-500">*</span></label>
                                <select id="examType" name="examType" class="w-full bg-gray-50 dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-indigo-500 focus:border-indigo-500 p-2.5" required>
                                    <option value="">Select</option>
                                    <option value="REGULAR">Regular</option>
                                    <option value="BACKLOG">Backlog</option>
                                    <option value="BOTH">Both</option>
                                </select>
                            </div>
                            <div>
                                <label class="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-1">Test Mode <span class="text-red-500">*</span></label>
                                <select id="mode" name="mode" class="w-full bg-gray-50 dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-indigo-500 focus:border-indigo-500 p-2.5" required>
                                    <option value="">Select Mode</option>
                                    <option value="OFFLINE">Offline</option>
                                    <option value="ONLINE">Online</option>
                                    <option value="HYBRID">Hybrid</option>
                                </select>
                            </div>
                            <div>
                                <label class="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-1">Course <span class="text-red-500">*</span></label>
                                <select id="course" name="course" class="w-full bg-gray-50 dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-indigo-500 focus:border-indigo-500 p-2.5" required>
                                    <option value="">Select Course</option>
                                    <option value="B.Tech">B.Tech</option>
                                    <option value="B.E">B.E</option>
                                    <option value="M.Tech">M.Tech</option>
                                    <option value="BCA">BCA</option>
                                    <option value="MCA">MCA</option>
                                    <option value="MBA">MBA</option>
                                    <option value="B.Sc">B.Sc</option>
                                </select>
                            </div>
                            <div>
                                <label class="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-1">Department <span class="text-red-500">*</span></label>
                                <select id="department" name="department" class="w-full bg-gray-50 dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-indigo-500 focus:border-indigo-500 p-2.5" required>
                                    <option value="">Select Department</option>
                                    <!-- Dynamic API options -->
                                </select>
                            </div>
                            <div>
                                <label class="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-1">Semester <span class="text-red-500">*</span></label>
                                <select id="semester" name="semester" class="w-full bg-gray-50 dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-indigo-500 focus:border-indigo-500 p-2.5" required>
                                    <option value="">Select Semester</option>
                                    <option value="1">1st Semester</option>
                                    <option value="2">2nd Semester</option>
                                    <option value="3">3rd Semester</option>
                                    <option value="4">4th Semester</option>
                                    <option value="5">5th Semester</option>
                                    <option value="6">6th Semester</option>
                                    <option value="7">7th Semester</option>
                                    <option value="8">8th Semester</option>
                                </select>
                            </div>
                            <div>
                                <label class="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-1">Assign Supervisor(s) <span class="text-red-500">*</span></label>
                                <div class="relative">
                                    <div id="supDropdownBtn" class="w-full bg-gray-50 dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-indigo-500 focus:border-indigo-500 p-2.5 flex justify-between items-center cursor-pointer">
                                        <span id="supDropdownText" class="truncate">Select Supervisor(s)</span>
                                        <svg class="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path></svg>
                                    </div>
                                    <div id="supDropdownMenu" class="hidden absolute z-10 w-full mt-1 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg shadow-lg p-3 max-h-48 overflow-y-auto space-y-2">
                                        <div id="supervisorIdsContainer">
                                            <!-- Dynamic API options as checkboxes -->
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- STEP 2: Subject Configuration -->
                    <div id="step2" class="step-content hidden animate-fade-in">
                        <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center border-b-2 border-indigo-100 dark:border-gray-700 pb-3 mb-6 gap-3">
                            <h3 class="text-xl font-bold text-gray-800 dark:text-white m-0"><i class="fas fa-book text-indigo-500 mr-2"></i>2. Subject Configuration</h3>
                            <button type="button" id="addSubjectBtn" class="bg-indigo-50 hover:bg-indigo-100 text-indigo-700 dark:bg-indigo-900/50 dark:text-indigo-300 dark:hover:bg-indigo-800/50 px-4 py-2 rounded-lg font-bold transition-colors text-sm border border-indigo-200 dark:border-indigo-800 flex items-center shadow-sm">
                                <i class="fas fa-plus mr-2"></i> Add Subject
                            </button>
                        </div>
                        <div id="subjectsContainer" class="space-y-4">
                            <!-- Dynamic Subjects Rendered Here -->
                        </div>
                        <p id="noSubjectWarning" class="text-red-500 text-sm hidden mt-3 font-semibold"><i class="fas fa-exclamation-triangle mr-1"></i> Please configure at least one subject to proceed.</p>
                    </div>

                    <!-- STEP 3: Registration & Schedule -->
                    <div id="step3" class="step-content hidden animate-fade-in">
                        <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
                            <!-- Registration Window -->
                            <div class="bg-gray-50 dark:bg-gray-700/30 p-6 rounded-xl border border-gray-100 dark:border-gray-700">
                                <h3 class="text-lg font-bold text-gray-800 dark:text-white border-b border-gray-200 dark:border-gray-600 pb-2 mb-4"><i class="fas fa-calendar-alt text-indigo-500 mr-2"></i>3a. Registration Window</h3>
                                <div class="space-y-4">
                                    <div class="grid grid-cols-2 gap-4">
                                        <div>
                                            <label class="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-1">Start Date <span class="text-red-500">*</span></label>
                                            <input type="date" id="regStartDate" name="regStartDate" class="w-full bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-indigo-500 focus:border-indigo-500 p-2.5">
                                        </div>
                                        <div>
                                            <label class="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-1">End Date <span class="text-red-500">*</span></label>
                                            <input type="date" id="regEndDate" name="regEndDate" class="w-full bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-indigo-500 focus:border-indigo-500 p-2.5">
                                        </div>
                                    </div>
                                    <div>
                                        <label class="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-1">Late Fee Deadline <span class="text-red-500">*</span></label>
                                        <input type="date" id="lateFeeDeadline" name="lateFeeDeadline" class="w-full bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-indigo-500 focus:border-indigo-500 p-2.5">
                                    </div>
                                </div>
                            </div>

                            <!-- Master Exam Schedule -->
                            <div class="bg-gray-50 dark:bg-gray-700/30 p-6 rounded-xl border border-gray-100 dark:border-gray-700">
                                <h3 class="text-lg font-bold text-gray-800 dark:text-white border-b border-gray-200 dark:border-gray-600 pb-2 mb-4"><i class="fas fa-clock text-indigo-500 mr-2"></i>3b. Exam Schedule</h3>
                                <div class="space-y-4">
                                    <div>
                                        <label class="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-1">Master Exam Date</label>
                                        <input type="date" id="examDate" name="examDate" class="w-full bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-indigo-500 focus:border-indigo-500 p-2.5">
                                    </div>
                                    <div class="grid grid-cols-2 gap-4">
                                        <div>
                                            <label class="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-1">Start Time</label>
                                            <input type="time" id="startTime" name="startTime" class="w-full bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-indigo-500 focus:border-indigo-500 p-2.5">
                                        </div>
                                        <div>
                                            <label class="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-1">End Time</label>
                                            <input type="time" id="endTime" name="endTime" class="w-full bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-indigo-500 focus:border-indigo-500 p-2.5">
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- STEP 4: Fee & Control Settings -->
                    <div id="step4" class="step-content hidden animate-fade-in">
                        <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
                            <!-- Fee Structure -->
                            <div>
                                <h3 class="text-xl font-bold text-gray-800 dark:text-white border-b-2 border-indigo-100 dark:border-gray-700 pb-3 mb-6"><i class="fas fa-rupee-sign text-indigo-500 mr-2"></i>4a. Fee Structure</h3>
                                <div class="space-y-5">
                                    <div>
                                        <label class="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-1">Regular Fee (Per Subject) <span class="text-red-500">*</span></label>
                                        <div class="relative">
                                            <div class="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
                                                <span class="text-gray-500 dark:text-gray-400 font-bold">₹</span>
                                            </div>
                                            <input type="number" id="regularFee" name="regularFee" class="w-full bg-gray-50 dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-indigo-500 focus:border-indigo-500 block pl-10 p-2.5" placeholder="500">
                                        </div>
                                    </div>
                                    <div>
                                        <label class="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-1">Backlog Fee (Per Subject) <span class="text-red-500">*</span></label>
                                        <div class="relative">
                                            <div class="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
                                                <span class="text-gray-500 dark:text-gray-400 font-bold">₹</span>
                                            </div>
                                            <input type="number" id="backlogFee" name="backlogFee" class="w-full bg-gray-50 dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-indigo-500 focus:border-indigo-500 block pl-10 p-2.5" placeholder="300">
                                        </div>
                                    </div>
                                    <div>
                                        <label class="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-1">Late Penalty Fee <span class="text-red-500">*</span></label>
                                        <div class="relative">
                                            <div class="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
                                                <span class="text-gray-500 dark:text-gray-400 font-bold">₹</span>
                                            </div>
                                            <input type="number" id="lateFee" name="lateFee" class="w-full bg-gray-50 dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-indigo-500 focus:border-indigo-500 block pl-10 p-2.5" placeholder="1000">
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <!-- Control Settings -->
                            <div class="bg-indigo-50/50 dark:bg-gray-700/50 p-6 rounded-2xl border border-indigo-100 dark:border-gray-600 relative overflow-hidden">
                                <div class="absolute top-0 right-0 w-32 h-32 bg-indigo-100 dark:bg-gray-600 rounded-bl-full -z-10 opacity-50"></div>
                                <h3 class="text-xl font-bold text-gray-800 dark:text-white border-b border-indigo-200 dark:border-gray-500 pb-3 mb-6"><i class="fas fa-sliders-h text-indigo-500 mr-2"></i>4b. Control Settings</h3>
                                
                                <div class="space-y-6 z-10 relative">
                                    <div>
                                        <label class="block text-sm font-bold text-gray-700 dark:text-gray-300 mb-1">Maximum Students Capacity</label>
                                        <input type="number" id="maxStudents" name="maxStudents" class="w-full bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-indigo-500 focus:border-indigo-500 p-2.5" placeholder="Leave empty for unlimited">
                                        <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">Registration will gracefully auto-close when full.</p>
                                    </div>
                                    
                                    <label class="inline-flex items-center cursor-pointer w-full p-4 rounded-xl border border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-800 transition-colors hover:border-indigo-300 shadow-sm">
                                        <input type="checkbox" id="requireFaceVerification" name="requireFaceVerification" class="sr-only peer" checked>
                                        <div class="relative w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-indigo-300 dark:peer-focus:ring-indigo-800 rounded-full peer dark:bg-gray-600 peer-checked:after:translate-x-full rtl:peer-checked:after:-translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:start-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-indigo-600"></div>
                                        <div class="ms-3 flex-1 flex flex-col">
                                            <span class="text-sm font-bold text-gray-800 dark:text-gray-200">Require Face Verification</span>
                                            <span class="text-xs text-gray-500">Enable Biometric AI checks for Registration.</span>
                                        </div>
                                    </label>

                                    <label class="inline-flex items-center cursor-pointer w-full p-4 rounded-xl border border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-800 transition-colors hover:border-indigo-300 shadow-sm">
                                        <input type="checkbox" id="allowEditAfterPublish" name="allowEditAfterPublish" class="sr-only peer">
                                        <div class="relative w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-indigo-300 dark:peer-focus:ring-indigo-800 rounded-full peer dark:bg-gray-600 peer-checked:after:translate-x-full rtl:peer-checked:after:-translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:start-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-indigo-600"></div>
                                        <div class="ms-3 flex-1 flex flex-col">
                                            <span class="text-sm font-bold text-gray-800 dark:text-gray-200">Allow Global Edits After Publish</span>
                                            <span class="text-xs text-gray-500">Unsafe - Can disrupt active student sessions.</span>
                                        </div>
                                    </label>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Wizard Navigation Buttons -->
                    <div class="flex justify-between mt-10 pt-6 border-t border-gray-100 dark:border-gray-700 items-center">
                        <button type="button" id="prevBtn" class="!hidden bg-white text-gray-800 hover:bg-gray-50 border border-gray-300 font-bold rounded-lg text-sm px-6 py-3 transition-colors dark:bg-gray-800 dark:text-white dark:border-gray-600 dark:hover:bg-gray-700 shadow-sm">
                            <i class="fas fa-arrow-left mr-2"></i> Back
                        </button>
                        
                        <div class="flex gap-3 ml-auto">
                            <button type="button" id="draftBtn" class="text-indigo-600 hover:text-indigo-800 bg-indigo-50 hover:bg-indigo-100 border border-indigo-200 font-bold rounded-lg text-sm px-5 py-3 transition-colors dark:bg-indigo-900/40 dark:text-indigo-400 dark:border-indigo-800/50 shadow-sm">
                                <i class="fas fa-save mr-2"></i> Save Draft
                            </button>
                            
                            <button type="button" id="nextBtn" class="text-white bg-indigo-600 hover:bg-indigo-700 focus:ring-4 focus:ring-indigo-300 font-bold rounded-lg text-sm px-8 py-3 dark:bg-indigo-500 dark:hover:bg-indigo-600 focus:outline-none dark:focus:ring-indigo-800 transition-all shadow-md flex items-center disabled:opacity-50 disabled:cursor-not-allowed">
                                Next <i class="fas fa-arrow-right ml-2 mt-0.5"></i>
                            </button>

                            <button type="button" id="publishBtn" class="!hidden text-white bg-green-600 hover:bg-green-700 focus:ring-4 focus:ring-green-300 font-bold rounded-lg text-sm px-8 py-3 dark:bg-green-500 dark:hover:bg-green-600 focus:outline-none dark:focus:ring-green-800 transition-all shadow-md shadow-green-500/30 flex items-center">
                                Publish Exam <i class="fas fa-rocket ml-2 mt-0.5"></i>
                            </button>
                        </div>
                    </div>
                </form>
            </div>
        `;

        attachCoreListeners();
        addSubjectRow(); // Default row
        updateStepUI();
        loadSupervisors(); // Fetch supervisors
        loadDepartments(); // Fetch departments dynamically
    };

    const addSubjectRow = (subjectData = null, isCompulsory = false) => {
        subjectsCount++;
        const id = Date.now() + Math.random().toString(36).substr(2, 5);

        const subName = subjectData ? subjectData.name : '';
        const subCode = subjectData ? subjectData.code : '';
        const isBacklogRow = subjectData && !isCompulsory;
        
        let headerHtml = '';
        if (isBacklogRow) {
            headerHtml = `
            <div class="w-full flex justify-between items-center bg-gray-100 dark:bg-gray-700/50 p-2 rounded-t-lg mb-4 border-b border-gray-200 dark:border-gray-600">
                <label class="flex items-center space-x-3 cursor-pointer font-bold text-indigo-700 dark:text-indigo-400 w-full">
                    <input type="checkbox" class="w-5 h-5 text-indigo-600 rounded border-gray-300 focus:ring-indigo-500 backlog-subject-toggle">
                    <span>Include Backlog Subject: ${subName}</span>
                </label>
            </div>
            `;
        }

        const html = `
            <div class="subject-row ${isBacklogRow ? 'opacity-60 ring-2 ring-transparent transition-all' : ''} bg-white dark:bg-gray-800/80 border border-gray-200 dark:border-gray-600 rounded-xl p-5 flex flex-wrap lg:flex-nowrap gap-4 relative transition-all shadow-sm hover:shadow group animate-fade-in-up" data-id="${id}" data-db-id="${subjectData ? subjectData.id : ''}">
                ${headerHtml}
                <div class="flex-grow w-full lg:w-auto ${isBacklogRow ? '' : 'mt-2'}">
                    <label class="block text-xs font-bold text-gray-600 dark:text-gray-400 uppercase tracking-wider mb-2">Subject Name *</label>
                    <input type="text" class="sub-name w-full bg-gray-50 dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-indigo-500 focus:border-indigo-500 p-2.5" required placeholder="e.g. Data Structures" value="${subName}" ${isCompulsory ? 'readonly' : ''}>
                </div>
                <div class="w-full sm:w-1/2 lg:w-32 ${isBacklogRow ? '' : 'mt-2'}">
                    <label class="block text-xs font-bold text-gray-600 dark:text-gray-400 uppercase tracking-wider mb-2">Code *</label>
                    <input type="text" class="sub-code w-full bg-gray-50 dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-indigo-500 focus:border-indigo-500 p-2.5 font-mono" required placeholder="CS201" value="${subCode}" ${isCompulsory ? 'readonly' : ''}>
                </div>
                <div class="w-full sm:w-1/2 lg:w-32 ${isBacklogRow ? '' : 'mt-2'}">
                    <label class="block text-xs font-bold text-gray-600 dark:text-gray-400 uppercase tracking-wider mb-2">Paper *</label>
                    <input type="text" class="sub-paper w-full bg-gray-50 dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-indigo-500 focus:border-indigo-500 p-2.5 font-mono" required placeholder="P201" value="${subCode ? 'P'+subCode : ''}">
                </div>
                <div class="w-1/3 sm:w-1/4 lg:w-24 ${isBacklogRow ? '' : 'mt-2'}">
                    <label class="block text-xs font-bold text-gray-600 dark:text-gray-400 uppercase tracking-wider mb-2">Total *</label>
                    <input type="number" class="sub-total w-full bg-gray-50 dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-indigo-500 focus:border-indigo-500 p-2.5 text-center" required value="100">
                </div>
                <div class="w-1/3 sm:w-1/4 lg:w-24 ${isBacklogRow ? '' : 'mt-2'}">
                    <label class="block text-xs font-bold text-gray-600 dark:text-gray-400 uppercase tracking-wider mb-2">Pass *</label>
                    <input type="number" class="sub-pass w-full bg-gray-50 dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-indigo-500 focus:border-indigo-500 p-2.5 text-center" required value="40">
                </div>
                <div class="w-1/3 sm:w-1/4 lg:w-28 ${isBacklogRow ? '' : 'mt-2'}">
                    <label class="block text-xs font-bold text-gray-600 dark:text-gray-400 uppercase tracking-wider mb-2">Mins *</label>
                    <input type="number" class="sub-dur w-full bg-gray-50 dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-indigo-500 focus:border-indigo-500 p-2.5 text-center" required value="180">
                </div>
                
                ${!subjectData ? `
                <button type="button" class="remove-sub-btn absolute -top-3 -right-3 w-8 h-8 bg-red-100 text-red-600 hover:bg-red-600 hover:text-white rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-all shadow-sm border border-red-200">
                    <i class="fas fa-times"></i>
                </button>
                ` : `<div class="absolute -top-3 -right-3 w-8 h-8 bg-green-100 text-green-600 rounded-full flex items-center justify-center shadow-sm border border-green-200" title="Auto Fetched from DB"><i class="fas fa-check"></i></div>`}
            </div>
        `;

        document.getElementById('subjectsContainer').insertAdjacentHTML('beforeend', html);
    };

    const attachCoreListeners = () => {
        // Back to Exams
        const backBtn = document.getElementById('backToExamsBtn');
        if (backBtn) {
            backBtn.addEventListener('click', () => {
                // Navigate back to the exams section in the dashboard
                const examsNavLink = document.querySelector('.nav-link[data-target="exams-section"]');
                if (examsNavLink) {
                    examsNavLink.click();
                } else {
                    // Fallback: reload the exams section manually
                    document.querySelectorAll('.tab-content').forEach(c => c.classList.add('hidden'));
                    const examsSection = document.getElementById('exams-section');
                    if (examsSection) {
                        examsSection.classList.remove('hidden');
                        if (typeof loadExams === 'function') loadExams();
                    }
                }
            });
        }

        // Step Navigation
        const nextBtn = document.getElementById('nextBtn');
        const prevBtn = document.getElementById('prevBtn');
        const draftBtn = document.getElementById('draftBtn');
        const publishBtn = document.getElementById('publishBtn');

        nextBtn.addEventListener('click', () => {
            if (validateStep(currentStep)) {
                currentStep++;
                updateStepUI();
                window.scrollTo({ top: 0, behavior: 'smooth' });
            }
        });

        prevBtn.addEventListener('click', () => {
            if (currentStep > 1) {
                currentStep--;
                updateStepUI();
                window.scrollTo({ top: 0, behavior: 'smooth' });
            }
        });

        draftBtn.addEventListener('click', async () => {
            if (validateStep(currentStep, false)) { // Soft validation for draft
                await saveExam('DRAFT');
            }
        });

        publishBtn.addEventListener('click', async () => {
            if (validateStep(currentStep)) { // Hard validation for publish
                await saveExam('OPEN');
            }
        });

        // Subject Addition
        document.getElementById('addSubjectBtn').addEventListener('click', () => {
            document.getElementById('noSubjectWarning').classList.add('hidden');
            addSubjectRow();
        });

        // Subject Removal and Toggle (Delegated)
        document.getElementById('subjectsContainer').addEventListener('click', (e) => {
            const btn = e.target.closest('.remove-sub-btn');
            if (btn) {
                const row = btn.closest('.subject-row');
                row.remove();
            }
        });

        document.getElementById('subjectsContainer').addEventListener('change', (e) => {
            if (e.target.classList.contains('backlog-subject-toggle')) {
                const row = e.target.closest('.subject-row');
                if (e.target.checked) {
                    row.classList.remove('opacity-60');
                    row.classList.add('ring-indigo-400');
                } else {
                    row.classList.add('opacity-60');
                    row.classList.remove('ring-indigo-400');
                }
            }
        });

        // Live Validation listeners
        const inputs = document.querySelectorAll('#wizardForm input[required], #wizardForm select[required]');
        inputs.forEach(input => {
            input.addEventListener('input', () => {
                input.classList.remove('border-red-500');
            });
        });

        // Subject auto-load when Course + Department + Semester all have values
        const triggerSubjectLoad = async () => {
            const course = document.getElementById('course')?.value?.trim();
            const deptId = document.getElementById('department')?.value?.trim();
            const semester = document.getElementById('semester')?.value?.trim();
            const examType = document.getElementById('examType')?.value?.trim();
            const container = document.getElementById('subjectsContainer');
            if (!container) return;

            if (course && deptId && semester && examType) {
                container.innerHTML = '<div class="text-center p-6"><i class="fas fa-spinner fa-spin text-indigo-500 mr-2"></i> Fetching Subjects...</div>';
                try {
                    let url = `http://localhost:8080/api/admin/subjects${examType === 'BACKLOG' ? '/backlogged' : ''}?course=${encodeURIComponent(course)}&semester=${semester}`;
                    if (deptId) url += `&departmentId=${deptId}`;
                    const res = await authFetch(url);
                    const subjects = res.ok ? await res.json() : [];

                    container.innerHTML = '';

                    if (subjects.length === 0) {
                        document.getElementById('noSubjectWarning').classList.remove('hidden');
                        document.getElementById('noSubjectWarning').innerHTML = '<i class="fas fa-exclamation-triangle mr-1"></i> No structured subjects found for this combination. Please add manual subjects or configure them in Manage Subjects.';
                    } else {
                        document.getElementById('noSubjectWarning').classList.add('hidden');
                        const isCompulsory = examType === 'REGULAR' || examType === 'BOTH';
                        subjects.forEach(s => {
                            addSubjectRow(s, isCompulsory);
                        });
                    }
                } catch (e) {
                    container.innerHTML = '<div class="text-red-500 font-bold p-4 text-center">Failed to load subjects from the server.</div>';
                }
            }
        };

        ['course', 'department', 'semester', 'examType'].forEach(id => {
            document.getElementById(id)?.addEventListener('change', triggerSubjectLoad);
        });
    };

    const validateStep = (stepNumber, enforceStrict = true) => {
        let isValid = true;
        const currentStepEl = document.getElementById(`step${stepNumber}`);

        if (!currentStepEl) return true;

        if (enforceStrict) {
            // Check required fields
            const requiredFields = currentStepEl.querySelectorAll('input[required], select[required]');
            requiredFields.forEach(field => {
                // Skip validation if inside an unchecked backlog row
                const toggle = field.closest('.subject-row')?.querySelector('.backlog-subject-toggle');
                if (toggle && !toggle.checked) return;

                if (!field.value.trim()) {
                    field.classList.add('border-red-500');
                    isValid = false;
                } else {
                    field.classList.remove('border-red-500');
                }
            });

            // Special logic for Subjects Step
            if (stepNumber === 2) {
                const examType = document.getElementById('examType')?.value;
                const rows = document.querySelectorAll('.subject-row');
                
                let selectedSubjects = [];
                rows.forEach(r => {
                    const toggle = r.querySelector('.backlog-subject-toggle');
                    if (toggle) {
                        if (toggle.checked) selectedSubjects.push(r);
                    } else {
                        selectedSubjects.push(r);
                    }
                });

                if (examType === 'REGULAR' && rows.length === 0) {
                    showToast("No subjects available to create a REGULAR exam.", false);
                    document.getElementById('noSubjectWarning').classList.remove('hidden');
                    isValid = false;
                } else if (examType === 'BACKLOG' && selectedSubjects.length === 0) {
                    showToast("Select at least one backlog subject to create a BACKLOG exam.", false);
                    document.getElementById('noSubjectWarning').classList.remove('hidden');
                    document.getElementById('noSubjectWarning').innerHTML = '<i class="fas fa-exclamation-triangle mr-1"></i> Please select at least one backlog subject.';
                    isValid = false;
                }
            }
        }

        return isValid;
    };

    const updateStepUI = () => {
        // Hide all steps
        document.querySelectorAll('.step-content').forEach(el => {
            el.classList.add('hidden');
        });

        // Show current step
        const currentEl = document.getElementById(`step${currentStep}`);
        if (currentEl) currentEl.classList.remove('hidden');

        // Update Indicators
        const indicators = document.querySelectorAll('.step-indicator');
        indicators.forEach((ind, index) => {
            const stepNum = index + 1;
            const circle = ind.querySelector('.step-circle');
            const span = ind.querySelector('span');

            ind.classList.remove('opacity-50');

            if (stepNum < currentStep) {
                // Completed
                circle.classList.remove('bg-gray-200', 'text-gray-500', 'border-indigo-600', 'bg-indigo-600', 'text-indigo-600', 'dark:bg-indigo-900', 'dark:bg-gray-700');
                circle.classList.add('bg-green-500', 'text-white', 'border-white');
                circle.innerHTML = '<i class="fas fa-check"></i>';
                span.classList.add('text-green-600', 'dark:text-green-400');
                span.classList.remove('text-indigo-700', 'text-gray-500', 'dark:text-indigo-400');
            } else if (stepNum === currentStep) {
                // Current
                circle.innerHTML = stepNum;
                circle.classList.remove('bg-gray-200', 'text-gray-500', 'bg-green-500', 'dark:bg-gray-700');
                circle.classList.add('bg-indigo-600', 'text-white', 'border-white');
                span.classList.add('text-indigo-700', 'dark:text-indigo-400');
                span.classList.remove('text-green-600', 'text-gray-500');
            } else {
                // Upcoming
                circle.innerHTML = stepNum;
                ind.classList.add('opacity-50');
                circle.classList.remove('bg-indigo-600', 'text-white', 'bg-green-500');
                circle.classList.add('bg-gray-200', 'text-gray-500', 'dark:bg-gray-700', 'dark:text-gray-400');
                span.classList.remove('text-indigo-700', 'text-green-600', 'dark:text-indigo-400');
                span.classList.add('text-gray-500', 'dark:text-gray-400');
            }
        });

        // Update Progress Bar Line
        const progressLine = document.getElementById('progressLine');
        if (progressLine) {
            const percentages = ['0%', '33%', '66%', '100%'];
            progressLine.style.width = percentages[currentStep - 1] || '0%';
        }

        // Button Visibility
        const prevBtn = document.getElementById('prevBtn');
        const nextBtn = document.getElementById('nextBtn');
        const draftBtn = document.getElementById('draftBtn');
        const publishBtn = document.getElementById('publishBtn');

        if (currentStep === 1) {
            prevBtn.classList.add('!hidden');
        } else {
            prevBtn.classList.remove('!hidden');
        }

        if (currentStep === 4) {
            nextBtn.classList.add('hidden');
            publishBtn.classList.remove('!hidden');
        } else {
            nextBtn.classList.remove('hidden');
            publishBtn.classList.add('!hidden');
        }
    };

    const showLoading = (show) => {
        const overlay = document.getElementById('loadingOverlay');
        if (show) overlay.classList.remove('hidden');
        else overlay.classList.add('hidden');
    };

    const showToast = (message, isSuccess = true) => {
        const toast = document.getElementById('toastNotification');
        const title = document.getElementById('toastTitle');
        const msg = document.getElementById('toastMessage');
        const icon = toast.querySelector('i');

        if (isSuccess) {
            toast.className = 'fixed top-5 right-5 z-50 transition-all duration-300 transform translate-y-0 opacity-100 bg-green-50 text-green-800 border-l-4 border-green-500 shadow-xl rounded py-3 px-6 flex items-center gap-3';
            icon.className = 'fas fa-check-circle text-xl text-green-500';
            title.innerText = 'Success';
        } else {
            toast.className = 'fixed top-5 right-5 z-50 transition-all duration-300 transform translate-y-0 opacity-100 bg-red-50 text-red-800 border-l-4 border-red-500 shadow-xl rounded py-3 px-6 flex items-center gap-3';
            icon.className = 'fas fa-exclamation-circle text-xl text-red-500';
            title.innerText = 'Error';
        }

        msg.innerText = message;

        setTimeout(() => {
            toast.classList.remove('translate-y-0', 'opacity-100');
            toast.classList.add('translate-y-[-100%]', 'opacity-0');
        }, 4000);
    };

    const buildPayload = (status) => {
        // Build Subjects Array
        const subjectRows = document.querySelectorAll('.subject-row');
        const subjectsArr = [];
        const subjectIds = [];

        Array.from(subjectRows).forEach(row => {
            const toggle = row.querySelector('.backlog-subject-toggle');
            if (toggle && !toggle.checked) return;

            const dbId = row.getAttribute('data-db-id');
            if (dbId) subjectIds.push(parseInt(dbId));

            subjectsArr.push({
                subjectName: row.querySelector('.sub-name').value,
                subjectCode: row.querySelector('.sub-code').value,
                paperCode: row.querySelector('.sub-paper').value,
                totalMarks: parseInt(row.querySelector('.sub-total').value) || 100,
                passingMarks: parseInt(row.querySelector('.sub-pass').value) || 40,
                duration: parseInt(row.querySelector('.sub-dur').value) || 180
            });
        });

        // Retrieve values securely handling optional/empty inputs
        const getVal = (id) => document.getElementById(id) ? document.getElementById(id).value : null;
        const getFloat = (id) => { const v = getVal(id); return v ? parseFloat(v) : null; };
        const getInt = (id) => { const v = getVal(id); return v ? parseInt(v) : null; };
        const getChecked = (id) => document.getElementById(id) ? document.getElementById(id).checked : false;

        return {
            sessionName: getVal('sessionName'),
            academicYear: getVal('academicYear'),
            examType: getVal('examType'),
            mode: getVal('mode'),
            course: getVal('course'),
            department: document.getElementById('department') && document.getElementById('department').selectedIndex > 0 ? document.getElementById('department').options[document.getElementById('department').selectedIndex].text : null,
            semester: getVal('semester'),
            collegeId: window.CollegeContext?.selectedCollegeId || null,

            supervisorId: Array.from(document.querySelectorAll('input[name="supervisorIds[]"]:checked')).length > 0 ? parseInt(Array.from(document.querySelectorAll('input[name="supervisorIds[]"]:checked'))[0].value) : null,
            supervisorName: Array.from(document.querySelectorAll('input[name="supervisorIds[]"]:checked')).length > 0 ? Array.from(document.querySelectorAll('input[name="supervisorIds[]"]:checked'))[0].getAttribute('data-text') : null,
            supervisorIds: Array.from(document.querySelectorAll('input[name="supervisorIds[]"]:checked')).map(cb => parseInt(cb.value)),

            // No examSubjectId needed, mapping handled via subjects array directly

            subjectIds: subjectIds,
            subjects: subjectsArr,

            registrationWindow: {
                startDate: getVal('regStartDate'),
                endDate: getVal('regEndDate'),
                lateFeeDeadline: getVal('lateFeeDeadline')
            },

            schedule: {
                examDate: getVal('examDate'),
                startTime: getVal('startTime') ? getVal('startTime') + ':00' : null,
                endTime: getVal('endTime') ? getVal('endTime') + ':00' : null
            },

            feeStructure: {
                regularFee: getFloat('regularFee'),
                backlogFee: getFloat('backlogFee'),
                lateFee: getFloat('lateFee')
            },

            controls: {
                maxStudents: getInt('maxStudents'),
                requireFaceVerification: getChecked('requireFaceVerification'),
                allowEditAfterPublish: getChecked('allowEditAfterPublish')
            },

            status: status
        };
    };

    const saveExam = async (statusOverride) => {
        const payload = buildPayload(statusOverride);
        showLoading(true);

        try {
            let url = 'http://localhost:8080/api/university/exams';
            let method = 'POST';

            if (examId) {
                const api = resolveExamApi(examId);
                // Update specific REST requirement
                if (statusOverride === 'OPEN') {
                    // Update exact details first
                    await authFetch(api.url, {
                        method: 'PUT',
                        body: JSON.stringify(payload)
                    });

                    // Then Patch Publish
                    url = `${api.url}/publish`;
                    method = 'PATCH';
                } else {
                    url = api.url;
                    method = 'PUT';
                }
            }

            const response = await authFetch(url, {
                method: method,
                body: method !== 'PATCH' ? JSON.stringify(payload) : null
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const data = await response.json();
            examId = data.id; // Store in local state

            showToast(statusOverride === 'DRAFT' ? "Exam Draft Saved Successfully." : "Exam Published & Live Successfully!");

            if (statusOverride === 'OPEN') {
                // Successful end of workflow. Reset everything.
                setTimeout(() => {
                    document.getElementById('wizardForm').reset();
                    examId = null;
                    currentStep = 1;
                    document.getElementById('subjectsContainer').innerHTML = '';
                    addSubjectRow();
                    updateStepUI();
                }, 2000);
            }

        } catch (error) {
            console.error("Exam save error:", error);
            showToast("Failed to save Exam. Please ensure Backend is running.", false);
        } finally {
            showLoading(false);
        }
    };

    const loadExistingData = async () => {
        if (!examId) return;
        showLoading(true);
        try {
            const api = resolveExamApi(examId);
            const res = await authFetch(api.url);
            if (!res.ok) throw new Error(`Failed to fetch exam (${res.status})`);
            const data = await res.json();

            const setVal = (id, val) => { const el = document.getElementById(id); if (el && val !== null) el.value = val; };
            const setChecked = (id, val) => { const el = document.getElementById(id); if (el) el.checked = val; };

            setVal('sessionName', data.sessionName);
            setVal('academicYear', data.academicYear);
            setVal('examType', data.examType);
            setVal('mode', data.mode);
            setVal('course', data.course);
            setVal('department', data.department);
            setVal('semester', data.semester);

            if (data.supervisorIds && data.supervisorIds.length > 0) {
                setTimeout(() => {
                    data.supervisorIds.forEach(id => {
                        const cb = document.getElementById('sup_' + id);
                        if (cb) { cb.checked = true; cb.dispatchEvent(new Event('change')); }
                    });
                }, 500);
            } else if (data.supervisorId) {
                setTimeout(() => {
                    const cb = document.getElementById('sup_' + data.supervisorId);
                    if (cb) { cb.checked = true; cb.dispatchEvent(new Event('change')); }
                }, 500);
            }

            if (data.registrationWindow) {
                setVal('regStartDate', data.registrationWindow.startDate);
                setVal('regEndDate', data.registrationWindow.endDate);
                setVal('lateFeeDeadline', data.registrationWindow.lateFeeDeadline);
            }

            if (data.schedule) {
                setVal('examDate', data.schedule.examDate);
                if (data.schedule.startTime) setVal('startTime', data.schedule.startTime.substring(0, 5));
                if (data.schedule.endTime) setVal('endTime', data.schedule.endTime.substring(0, 5));
            }

            if (data.feeStructure) {
                setVal('regularFee', data.feeStructure.regularFee);
                setVal('backlogFee', data.feeStructure.backlogFee);
                setVal('lateFee', data.feeStructure.lateFee);
            }

            if (data.controls) {
                setVal('maxStudents', data.controls.maxStudents);
                setChecked('requireFaceVerification', data.controls.requireFaceVerification);
                setChecked('allowEditAfterPublish', data.controls.allowEditAfterPublish);
            }

            if (data.subjects && data.subjects.length > 0) {
                document.getElementById('subjectsContainer').innerHTML = '';
                data.subjects.forEach(sub => {
                    addSubjectRow();
                    const group = document.querySelector('.subject-row:last-child');
                    group.querySelector('.sub-name').value = sub.subjectName || '';
                    group.querySelector('.sub-code').value = sub.subjectCode || '';
                    group.querySelector('.sub-paper').value = sub.paperCode || '';
                    group.querySelector('.sub-total').value = sub.totalMarks || '';
                    group.querySelector('.sub-pass').value = sub.passingMarks || '';
                    group.querySelector('.sub-dur').value = sub.duration || '';
                });
            }
        } catch (e) {
            console.error(e);
            showToast("Failed to load existing exam data", false);
        } finally {
            showLoading(false);
        }
    };

    const loadSupervisors = async () => {
        try {
            let collegeId = window.CollegeContext?.selectedCollegeId || sessionStorage.getItem('selectedCollegeId') || 1;
            const res = await authFetch(`/api/university/${collegeId}/staff`);
            if (res.ok) {
                const staff = await res.json();
                const container = document.getElementById('supervisorIdsContainer');
                if (container) {
                    container.innerHTML = '';
                    staff.forEach(person => {
                        const val = person.staffId || person.id || person.userId;
                        const labelText = person.name + (person.supervisorType ? ` (${person.supervisorType})` : '');
                        const div = document.createElement('div');
                        div.className = "flex items-center mb-2";
                        div.innerHTML = `
                            <input type="checkbox" id="sup_${val}" name="supervisorIds[]" value="${val}" data-text="${labelText}" class="w-4 h-4 text-indigo-600 bg-gray-100 border-gray-300 rounded focus:ring-indigo-500 sup-checkbox">
                            <label for="sup_${val}" class="ml-2 text-sm font-medium text-gray-900 dark:text-gray-300 cursor-pointer w-full">${labelText}</label>
                        `;
                        container.appendChild(div);
                    });
                    
                    const updateText = () => {
                        const checked = Array.from(document.querySelectorAll('.sup-checkbox:checked'));
                        const textSpan = document.getElementById('supDropdownText');
                        if (textSpan) {
                            if (checked.length === 0) textSpan.textContent = 'Select Supervisor(s)';
                            else if (checked.length === 1) textSpan.textContent = checked[0].getAttribute('data-text');
                            else textSpan.textContent = `${checked.length} selected`;
                        }
                    };
                    document.querySelectorAll('.sup-checkbox').forEach(cb => cb.addEventListener('change', updateText));
                    
                    const btn = document.getElementById('supDropdownBtn');
                    const menu = document.getElementById('supDropdownMenu');
                    if (btn && menu) {
                        const newBtn = btn.cloneNode(true);
                        btn.parentNode.replaceChild(newBtn, btn);
                        
                        newBtn.addEventListener('click', (e) => {
                            menu.classList.toggle('hidden');
                            e.stopPropagation();
                        });
                        
                        document.addEventListener('click', (e) => {
                            if (!newBtn.contains(e.target) && !menu.contains(e.target)) {
                                menu.classList.add('hidden');
                            }
                        });
                    }
                }
            }
        } catch (e) {
            console.error('Failed to load supervisors', e);
        }
    };

    const loadDepartments = async () => {
        try {
            const collegeId = window.CollegeContext?.selectedCollegeId || sessionStorage.getItem('selectedCollegeId') || 1;
            if (!collegeId) return;
            const res = await authFetch(`/api/admin/departments?collegeId=${collegeId}`);
            if (res.ok) {
                const depts = await res.json();
                const dropdown = document.getElementById('department');
                dropdown.innerHTML = '<option value="">Select Department</option>';
                
                if (depts.length === 0) {
                    // Fallback to default departments if DB is empty
                    const defaults = ['Computer Science', 'Information Technology', 'Electronics & Communication', 'Mechanical Engineering', 'Civil Engineering', 'Electrical Engineering'];
                    defaults.forEach((name, index) => {
                        const opt = document.createElement('option');
                        opt.value = index + 1; // Arbitrary ID, as payload uses text
                        opt.textContent = name;
                        dropdown.appendChild(opt);
                    });
                } else {
                    depts.forEach(d => {
                        const opt = document.createElement('option');
                        opt.value = d.id;
                        opt.textContent = d.name;
                        dropdown.appendChild(opt);
                    });
                }
            }
        } catch (e) {
            console.error('Failed to load departments', e);
        }
    };

    render();
    if (examId) loadExistingData();
}
