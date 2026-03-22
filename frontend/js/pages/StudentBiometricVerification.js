export const StudentBiometricVerification = {
    render() {
        return `
            <div class="max-w-3xl mx-auto">
                <div class="glass-card p-6 md:p-8 rounded-2xl animate-fade-in-up">
                    <h1 class="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white font-display text-center">
                        Biometric Enrollment
                    </h1>
                    
                    <div class="mt-8 text-center space-y-6">
                        <div id="enrolled-status-badge" class="inline-flex items-center px-4 py-2 mb-2 rounded-full text-sm font-semibold shadow-sm bg-gray-100 text-gray-700">
                            <i class="fas fa-circle-notch fa-spin mr-2"></i> Checking Status...
                        </div>

                        <div class="w-32 h-32 mx-auto bg-gray-50 dark:bg-gray-800 rounded-full flex items-center justify-center border-4 border-gray-100 dark:border-gray-700 shadow-inner relative overflow-hidden group">
                            <i id="fingerprint-icon" class="fas fa-fingerprint text-6xl text-gray-300 dark:text-gray-600 transition-colors duration-300"></i>
                            <div id="scan-line" class="absolute top-0 left-0 w-full h-1 bg-indigo-500 shadow-[0_0_15px_rgba(99,102,241,1)] hidden"></div>
                        </div>

                        <div>
                            <p class="text-gray-600 dark:text-gray-400 max-w-md mx-auto">
                                To secure your exam registrations, please enroll your fingerprint. Click the button below to simulate scanning your biometric data.
                            </p>
                            <p class="mt-4 text-xs font-semibold text-gray-500 dark:text-gray-400">
                                Note: Students can enroll their biometric data only once. Contact an administrator if a reset is required.
                            </p>
                        </div>

                        <div>
                            <button id="scan-btn" class="px-8 py-3 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold shadow-lg shadow-indigo-500/30 transition-all flex items-center gap-2 mx-auto disabled:opacity-50 disabled:cursor-not-allowed">
                                <i class="fas fa-fingerprint"></i> Scan Fingerprint
                            </button>
                            <div id="scan-loading" class="mt-4 text-sm text-indigo-600 font-medium hidden">
                                <i class="fas fa-circle-notch fa-spin mr-2"></i> Scanning in progress...
                            </div>
                        </div>

                        <div id="scan-result" class="hidden mt-6 p-4 rounded-xl text-sm font-medium"></div>
                    </div>
                </div>
            </div>
        `;
    },

    afterRender() {
        const scanBtn = document.getElementById('scan-btn');
        const scanLoading = document.getElementById('scan-loading');
        const scanResult = document.getElementById('scan-result');
        const fpIcon = document.getElementById('fingerprint-icon');
        const scanLine = document.getElementById('scan-line');

        const showResult = (success, message) => {
            scanResult.classList.remove('hidden');
            scanResult.classList.toggle('bg-green-50', success);
            scanResult.classList.toggle('text-green-700', success);
            scanResult.classList.toggle('border', success);
            scanResult.classList.toggle('border-green-200', success);
            
            scanResult.classList.toggle('bg-red-50', !success);
            scanResult.classList.toggle('text-red-700', !success);
            scanResult.classList.toggle('border-red-200', !success);
            
            scanResult.textContent = message;
        };

        const checkStatus = async () => {
            const statusBadge = document.getElementById('enrolled-status-badge');
            try {
                const token = localStorage.getItem('token');
                if (!token) return;
                const response = await fetch('http://localhost:8080/api/student-profile/biometric/status', {
                    headers: { 'Authorization': `Bearer ${token}` }
                });

                if (response.status === 401 || response.status === 403) {
                    console.error("401/403 Unauthorized: Token may be empty or invalid.");
                    statusBadge.className = 'inline-flex items-center px-4 py-2 mb-2 rounded-full text-sm font-semibold shadow-sm bg-red-100 text-red-800';
                    statusBadge.innerHTML = '❌ Not Logged In';
                    return;
                }

                const data = await response.json();
                
                if (data.success && data.enrolled) {
                    fpIcon.classList.remove('text-gray-300', 'dark:text-gray-600');
                    fpIcon.classList.add('text-green-500');
                    showResult(true, "Your biometric fingerprint is currently enrolled.");
                    
                    // Lock enrollment
                    scanBtn.disabled = true;
                    scanBtn.classList.add('hidden');
                    
                    statusBadge.className = 'inline-flex items-center px-4 py-2 mb-2 rounded-full text-sm font-semibold shadow-sm bg-green-100 text-green-800';
                    statusBadge.innerHTML = '✅ Enrolled';
                } else {
                    scanBtn.innerHTML = '<i class="fas fa-fingerprint"></i> Scan Fingerprint';
                    scanBtn.classList.remove('hidden');
                    scanBtn.disabled = false;
                    statusBadge.className = 'inline-flex items-center px-4 py-2 mb-2 rounded-full text-sm font-semibold shadow-sm bg-red-100 text-red-800';
                    statusBadge.innerHTML = '❌ Not Enrolled';
                }
            } catch (e) {
                console.error("Failed to fetch biometric status", e);
                statusBadge.className = 'inline-flex items-center px-4 py-2 mb-2 rounded-full text-sm font-semibold shadow-sm bg-red-100 text-red-800';
                statusBadge.innerHTML = '❌ Status Error';
            }
        };
        
        checkStatus();

        // Seamless Session Transfer for localhost WebAuthn requirements
        (function() {
            const params = new URLSearchParams(window.location.search);
            const sessionData = params.get('sessionTransfer');
            if (sessionData) {
                try {
                    const session = JSON.parse(atob(sessionData));
                    if (session.token) localStorage.setItem('token', session.token);
                    if (session.role) localStorage.setItem('role', session.role);
                    if (session.userId) localStorage.setItem('userId', session.userId);
                    window.history.replaceState({}, document.title, window.location.pathname);
                    window.location.reload(); // Hard reload to apply token immediately
                } catch(e) {
                    console.error("Failed to parse session transfer data:", e);
                }
            }
        })();

        if (scanBtn) {
            let isScanning = false;

            const startScan = async () => {
                if (isScanning || scanBtn.disabled) return;
                isScanning = true;
                
                scanBtn.classList.add('hidden');
                scanBtn.disabled = true;
                scanResult.classList.add('hidden');
                
                fpIcon.classList.remove('text-gray-300', 'dark:text-gray-600', 'text-red-500', 'text-green-500');
                fpIcon.classList.add('text-indigo-500');
                
                scanLine.classList.remove('hidden');
                scanLine.style.animation = 'scan 2s infinite ease-in-out alternate';
                if (!document.getElementById('scan-keyframes')) {
                    const style = document.createElement('style');
                    style.id = 'scan-keyframes';
                    style.innerHTML = `@keyframes scan { 0% { top: 0; } 100% { top: 100%; } }`;
                    document.head.appendChild(style);
                }
                
                scanLoading.classList.remove('hidden');
                scanLoading.innerHTML = '<i class="fas fa-fingerprint animate-pulse mr-2"></i> Scanning...';

                setTimeout(async () => {
                    scanLoading.innerHTML = '<i class="fas fa-cog fa-spin mr-2"></i> Matching...';

                    if (Math.random() < 0.15) {
                        finishScan(false, "Scanner Error: Poor scan quality. Try again.");
                        return;
                    }

                    try {
                        let userId = localStorage.getItem('userId');
                        if (!userId) {
                            const token = localStorage.getItem('token');
                            if (token) {
                                const payload = JSON.parse(atob(token.split('.')[1]));
                                userId = payload.sub;
                            } else {
                                userId = "UNKNOWN";
                            }
                        }

                        const fingerprintData = "PHYSICAL_MINUTIAE_" + userId;
                        const token = localStorage.getItem('token');
                        
                        const response = await fetch('http://localhost:8080/api/student-profile/biometric/enroll', {
                            method: 'POST',
                            headers: { 
                                'Content-Type': 'application/json',
                                'Authorization': token ? `Bearer ${token}` : ''
                            },
                            body: JSON.stringify({ fingerprint: fingerprintData })
                        });

                        if (response.status === 401 || response.status === 403) {
                            throw new Error("Session expired or unauthorized. Please re-login.");
                        }

                        const data = await response.json();

                        if (response.ok && data.success) {
                            finishScan(true, data.message || "Fingerprint successfully enrolled!");
                            const statusBadge = document.getElementById('enrolled-status-badge');
                            if (statusBadge) {
                                statusBadge.className = 'inline-flex items-center px-4 py-2 mb-2 rounded-full text-sm font-semibold shadow-sm bg-green-100 text-green-800';
                                statusBadge.innerHTML = '✅ Enrolled';
                            }
                        } else {
                            throw new Error(data.message || "Failed to enroll biometric on server.");
                        }
                    } catch (err) {
                        finishScan(false, err.message);
                    }
                }, 2500);
            };

            const finishScan = (success, message) => {
                isScanning = false;
                scanLine.classList.add('hidden');
                scanLoading.classList.add('hidden');
                scanBtn.classList.remove('hidden');
                
                if (success) {
                    fpIcon.classList.remove('text-indigo-500', 'text-red-500');
                    fpIcon.classList.add('text-green-500');
                    showResult(true, message);
                    scanBtn.innerHTML = '<i class="fas fa-fingerprint"></i> Update Fingerprint';
                    scanBtn.disabled = true;
                    scanBtn.classList.add('hidden'); 
                } else {
                    fpIcon.classList.remove('text-indigo-500', 'text-green-500');
                    fpIcon.classList.add('text-red-500');
                    showResult(false, message);
                    scanBtn.disabled = false;
                    scanBtn.innerHTML = '<i class="fas fa-redo"></i> Try Again';
                }
            };

            scanBtn.addEventListener('click', startScan);
        }
    }
};
