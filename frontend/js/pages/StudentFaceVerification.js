export const StudentFaceVerification = {
    render() {
        return `
            <div class="max-w-4xl mx-auto">
                <div class="glass-card p-6 md:p-8 rounded-2xl animate-fade-in-up">
                    <h1 class="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white font-display text-center">
                        Complete Face Verification
                    </h1>

                    <div class="mt-8 space-y-8">
                        <!-- ID Card Upload -->
                        <div>
                            <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">
                                Upload ID Card Image
                            </label>
                            <input id="id-card-input" type="file" accept="image/*"
                                class="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-200 focus:ring-2 focus:ring-indigo-500">
                            <img id="id-card-preview" class="mt-4 w-full max-h-56 object-cover rounded-xl border border-gray-100 dark:border-gray-700 hidden" />
                        </div>

                        <!-- Webcam Capture -->
                        <div>
                            <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">
                                Live Webcam Capture
                            </label>
                            <div class="rounded-xl overflow-hidden border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800">
                                <video id="webcam-preview" class="w-full hidden" autoplay playsinline></video>
                                <img id="capture-preview" class="w-full hidden" />
                                <div id="webcam-placeholder" class="p-8 text-center text-sm text-gray-500 dark:text-gray-400">
                                    Webcam preview will appear here
                                </div>
                            </div>

                            <div class="mt-4 flex flex-wrap gap-3">
                                <button id="webcam-start"
                                    class="px-4 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white font-medium">
                                    Open Webcam
                                </button>
                                <button id="webcam-capture"
                                    class="px-4 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white font-medium hidden">
                                    Capture Photo
                                </button>
                                <button id="webcam-retake"
                                    class="px-4 py-2 rounded-lg bg-gray-200 hover:bg-gray-300 text-gray-700 font-medium hidden">
                                    Retake
                                </button>
                            </div>
                        </div>

                        <!-- Verify Button -->
                        <div>
                            <button id="verify-btn"
                                class="w-full py-3 rounded-xl bg-green-600 hover:bg-green-700 text-white font-semibold flex items-center justify-center gap-2">
                                Verify Identity
                            </button>
                            <div id="verify-loading" class="mt-3 text-sm text-gray-500 dark:text-gray-400 hidden">
                                Verifying... please wait.
                            </div>
                        </div>

                        <!-- Result -->
                        <div id="verification-result" class="hidden rounded-xl border p-4 text-sm">
                            <div id="result-title" class="font-semibold"></div>
                            <div id="result-confidence" class="mt-1"></div>
                            <div id="result-message" class="mt-1"></div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    },

    afterRender() {
        const idCardInput = document.getElementById('id-card-input');
        const idCardPreview = document.getElementById('id-card-preview');
        const webcamPreview = document.getElementById('webcam-preview');
        const capturePreview = document.getElementById('capture-preview');
        const webcamPlaceholder = document.getElementById('webcam-placeholder');
        const webcamStart = document.getElementById('webcam-start');
        const webcamCapture = document.getElementById('webcam-capture');
        const webcamRetake = document.getElementById('webcam-retake');
        const verifyBtn = document.getElementById('verify-btn');
        const verifyLoading = document.getElementById('verify-loading');
        const resultBox = document.getElementById('verification-result');
        const resultTitle = document.getElementById('result-title');
        const resultConfidence = document.getElementById('result-confidence');
        const resultMessage = document.getElementById('result-message');

        let idCardBase64 = null;
        let liveBase64 = null;
        let stream = null;
        let verifiedPermanently = false;

        const showResult = (verified, confidence, message) => {
            resultBox.classList.remove('hidden');
            resultBox.classList.toggle('border-green-400', verified);
            resultBox.classList.toggle('bg-green-50', verified);
            resultBox.classList.toggle('text-green-700', verified);
            resultBox.classList.toggle('border-red-400', !verified);
            resultBox.classList.toggle('bg-red-50', !verified);
            resultBox.classList.toggle('text-red-700', !verified);

            resultTitle.textContent = verified ? 'Verification successful' : 'Verification failed';
            resultConfidence.textContent = `Confidence: ${confidence.toFixed(2)}%`;
            resultMessage.textContent = message;
        };

        const setLoading = (loading) => {
            verifyLoading.classList.toggle('hidden', !loading);
            verifyBtn.disabled = loading || verifiedPermanently;
            verifyBtn.classList.toggle('opacity-60', loading || verifiedPermanently);
        };

        idCardInput.addEventListener('change', () => {
            const file = idCardInput.files[0];
            if (!file) return;
            const reader = new FileReader();
            reader.onloadend = () => {
                idCardBase64 = reader.result;
                idCardPreview.src = idCardBase64;
                idCardPreview.classList.remove('hidden');
            };
            reader.readAsDataURL(file);
        });

        const startWebcam = async () => {
            try {
                stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user' }, audio: false });
                webcamPreview.srcObject = stream;
                webcamPreview.classList.remove('hidden');
                capturePreview.classList.add('hidden');
                webcamPlaceholder.classList.add('hidden');
                webcamCapture.classList.remove('hidden');
                webcamRetake.classList.add('hidden');
            } catch (err) {
                showResult(false, 0, `Webcam error: ${err.message}`);
            }
        };

        const stopWebcam = () => {
            if (stream) {
                stream.getTracks().forEach(track => track.stop());
                stream = null;
            }
        };

        const capturePhoto = () => {
            const canvas = document.createElement('canvas');
            canvas.width = webcamPreview.videoWidth;
            canvas.height = webcamPreview.videoHeight;
            const ctx = canvas.getContext('2d');
            ctx.drawImage(webcamPreview, 0, 0, canvas.width, canvas.height);
            liveBase64 = canvas.toDataURL('image/jpeg', 0.9);
            capturePreview.src = liveBase64;
            capturePreview.classList.remove('hidden');
            webcamPreview.classList.add('hidden');
            webcamCapture.classList.add('hidden');
            webcamRetake.classList.remove('hidden');
            stopWebcam();
        };

        webcamStart.addEventListener('click', startWebcam);
        webcamCapture.addEventListener('click', capturePhoto);
        webcamRetake.addEventListener('click', startWebcam);

        verifyBtn.addEventListener('click', async () => {
            if (verifiedPermanently) return;
            if (!idCardBase64 || !liveBase64) {
                showResult(false, 0, 'Please upload ID card and capture live photo.');
                return;
            }

            const profileId = parseInt(localStorage.getItem('userId'), 10);
            if (!profileId) {
                showResult(false, 0, 'Student profile ID not found. Please login again.');
                return;
            }

            setLoading(true);
            resultBox.classList.add('hidden');

            try {
                const response = await fetch('http://localhost:8081/api/student-profile/verify', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        profileId,
                        idCardImage: idCardBase64,
                        liveImage: liveBase64
                    })
                });

                const data = await response.json();
                const verified = !!data.verified;
                const confidence = typeof data.confidence === 'number'
                    ? data.confidence
                    : parseFloat(data.confidence) || 0;
                const message = data.message || (verified ? 'Profile Verified & Locked' : 'Face not matched');

                showResult(verified, confidence, message);

                if (verified) {
                    verifiedPermanently = true;
                    verifyBtn.textContent = 'Profile Verified & Locked';
                    verifyBtn.disabled = true;
                    verifyBtn.classList.add('opacity-60');
                    setTimeout(() => {
                        window.location.href = 'student_dashboard.html';
                    }, 3000);
                }
            } catch (err) {
                showResult(false, 0, 'Verification failed. Please try again.');
            } finally {
                setLoading(false);
            }
        });
    }
};
