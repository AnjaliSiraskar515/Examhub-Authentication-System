import { API } from '../utils/api.js';

export const Profile = {
    userData: null,

    async init() {
        try {
            const data = await API.getProfile();
            // Map backend data to frontend structure if necessary
            this.userData = {
                ...data,
                // Defaults/Fallbacks for fields not yet in backend
                university: data.university || "Pune University (SPPU)",
                admissionYear: data.year ? (new Date().getFullYear() - parseInt(data.year.match(/\d+/) || 0)) : "2023",
                totalExams: data.totalExams || 0,
                upcomingExams: data.upcomingExams || 0,
                lastExam: data.lastExam || "N/A",
                hallTicket: data.upcomingExams > 0 ? "Generated" : "N/A",
                phone: data.phoneNumber || "N/A",
                status: data.profileCompleted ? "Active" : "Incomplete",
                isVerified: !!data.verified && !!data.profileLocked,
                documents: data.documents || { aadhar: false, marks10: false, marks12: false, ug: false, pg: false, biometric: false, passportPhoto: false },
                profileCompleted: data.profileCompleted || false
            };
            console.log("Profile Data Loaded:", this.userData);
        } catch (error) {
            console.error("Failed to load profile:", error);
            // Fallback to dummy data if API fails (for demo purposes)
            this.userData = this.dummyData;
        }
    },

    // Dummy Data for fallback
    dummyData: {
        name: "Rahul Sharma",
        rollNumber: "CS-2023-045",
        university: "Pune University (SPPU)",
        course: "Bachelor of Technology",
        branch: "Computer Engineering",
        year: "Final Year",
        semester: "8th Semester",
        status: "Active",

        email: "rahul.sharma@example.com",
        phone: "+91 98765 43210",
        regNumber: "REG20230045",
        dob: "15 Aug 2002",
        gender: "Male",

        enrollmentNo: "EN20230089",
        admissionYear: "2023",
        cgpa: "8.5",
        academicStatus: "Regular",

        totalExams: 24,
        upcomingExams: 2,
        lastExam: "Database Management Systems (Dec 2025)",
        hallTicket: "Generated",
        isVerified: false,
        documents: { aadhar: true, marks10: true, marks12: false, ug: false, pg: false, biometric: false, passportPhoto: true },
        profileCompleted: false
    },

    render() {
        const data = this.userData || this.dummyData;

        // Check if all mandatory documents are uploaded
        const mandatoryDocs = ['aadhar', 'marks10', 'marks12', 'passportPhoto'];
        const allMandatoryUploaded = mandatoryDocs.every(doc => data.documents && data.documents[doc]);

        // Define document labels
        const docLabels = {
            aadhar: "Aadhar Card",
            marks10: "10th Marksheet",
            marks12: "12th Marksheet",
            sem1Marksheet: "Semester 1 Marksheet (Optional)",
            sem2Marksheet: "Semester 2 Marksheet (Optional)",
            sem3Marksheet: "Semester 3 Marksheet (Optional)",
            sem4Marksheet: "Semester 4 Marksheet (Optional)",
            sem5Marksheet: "Semester 5 Marksheet (Optional)",
            sem6Marksheet: "Semester 6 Marksheet (Optional)",
            sem7Marksheet: "Semester 7 Marksheet (Optional)",
            sem8Marksheet: "Semester 8 Marksheet (Optional)"
        };

        // Determine Avatar URL
        // Use passportPhotoPath (uploaded) or photoPath (Google/Auth), else avatar generator
        const avatarPath = data.passportPhotoPath || data.photoPath;
        const avatarUrl = avatarPath
            ? `/uploads/${avatarPath}`
            : `https://ui-avatars.com/api/?name=${encodeURIComponent(data.name || 'User')}&background=random&size=150`;

        return `
            <div class="container-fluid p-4">
                <!-- 1️⃣ Profile Header Card -->
                <div class="card shadow mb-4 border-0 rounded-4 overflow-hidden">
                    <div class="card-body p-0">

                        <!-- ✨ Beautiful Cover Banner -->
                        <div class="position-relative overflow-hidden" style="
                            height: 160px;
                            background: linear-gradient(135deg, #1a1a2e 0%, #16213e 40%, #0f3460 70%, #533483 100%);
                        ">
                            <!-- Decorative blurred circles -->
                            <div style="position:absolute;width:220px;height:220px;border-radius:50%;background:rgba(255,255,255,0.05);top:-60px;left:-40px;"></div>
                            <div style="position:absolute;width:160px;height:160px;border-radius:50%;background:rgba(83,52,131,0.35);top:-30px;right:80px;"></div>
                            <div style="position:absolute;width:100px;height:100px;border-radius:50%;background:rgba(255,255,255,0.06);bottom:-20px;right:200px;"></div>
                            <div style="position:absolute;width:60px;height:60px;border-radius:50%;background:rgba(255,255,255,0.08);top:20px;right:40px;"></div>

                            <!-- Banner Content: name + details on the right side -->
                            <div class="position-absolute w-100 h-100 d-flex align-items-center px-5" style="top:0;left:0;">
                                <!-- Left spacer for the avatar overlap -->
                                <div style="width:140px; flex-shrink:0;"></div>
                                <div class="ms-3">
                                    <h3 class="fw-bold text-white mb-1" style="font-size:1.5rem;text-shadow:0 2px 8px rgba(0,0,0,0.4);">
                                        ${data.name || 'Student'}
                                    </h3>
                                    <div class="d-flex flex-wrap gap-2 align-items-center" style="font-size:0.85rem;">
                                        <span class="text-white text-opacity-75">
                                            <i class="fas fa-id-badge me-1"></i>PRN: <strong class="text-white">${data.prn || 'N/A'}</strong>
                                        </span>
                                        <span class="text-white text-opacity-50">|</span>
                                        <span class="text-white text-opacity-75">
                                            <i class="fas fa-university me-1"></i>${data.university || 'Pune University (SPPU)'}
                                        </span>
                                    </div>
                                    <div class="d-flex flex-wrap gap-2 mt-1" style="font-size:0.82rem;">
                                        <span class="badge rounded-pill px-3 py-1" style="background:rgba(255,255,255,0.15);color:#fff;backdrop-filter:blur(4px);">
                                            <i class="fas fa-graduation-cap me-1"></i>${data.course || 'N/A'}
                                        </span>
                                        <span class="badge rounded-pill px-3 py-1" style="background:rgba(255,255,255,0.15);color:#fff;backdrop-filter:blur(4px);">
                                            <i class="fas fa-code-branch me-1"></i>${data.branch || 'N/A'}
                                        </span>
                                        <span class="badge rounded-pill px-3 py-1" style="background:rgba(255,255,255,0.15);color:#fff;backdrop-filter:blur(4px);">
                                            <i class="fas fa-calendar me-1"></i>${data.year || 'N/A'} · ${data.semester || ''}
                                        </span>
                                        <span class="badge rounded-pill px-3 py-1 ${data.status === 'Active' ? '' : ''}" style="background:${data.status === 'Active' ? 'rgba(34,197,94,0.75)' : 'rgba(251,191,36,0.75)'};color:#fff;backdrop-filter:blur(4px);">
                                            <i class="fas fa-circle me-1" style="font-size:7px;vertical-align:middle;"></i>${data.status || 'Incomplete'}
                                        </span>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Bottom strip: avatar + verification button -->
                        <div class="px-4 pb-4">
                            <div class="d-flex align-items-end gap-4" style="margin-top:-60px;">
                                <!-- Avatar -->
                                <div class="position-relative flex-shrink-0">
                                    <img src="${avatarUrl}"
                                        class="rounded-circle border border-4 border-white shadow-lg bg-white"
                                        alt="Profile Photo"
                                        style="width:120px;height:120px;object-fit:cover;">
                                    <input type="file" id="profileUpload" accept="image/*" class="d-none">
                                    <button onclick="document.getElementById('profileUpload').click()"
                                        class="btn btn-sm btn-dark position-absolute bottom-0 end-0 rounded-circle shadow"
                                        style="width:32px;height:32px;" title="Change Photo">
                                        <i class="fas fa-camera text-white" style="font-size:12px;"></i>
                                    </button>
                                </div>
                                <!-- Verification action -->
                                <div class="pb-1 pt-5">
                                    ${data.isVerified
                ? `<span class="badge bg-success py-2 px-3 rounded-pill fs-6 shadow-sm"><i class="fas fa-check-circle me-1"></i> Profile Verified &amp; Locked</span>`
                : `<button onclick="window.dispatchEvent(new CustomEvent('navigate',{detail:{page:'face-verification'}}))"
                                                class="btn btn-warning fw-bold text-dark rounded-pill px-4 shadow-sm">
                                                <i class="fas fa-camera me-2"></i> Complete Face Verification
                                           </button>`
            }
                                </div>
                            </div>
                        </div>

                    </div>
                </div>

                <div class="row g-4">

                    <!-- 2️⃣ Personal Information Card -->
                    <div class="col-lg-6">
                        <div class="card shadow-sm h-100 border-0 rounded-4 hover-shadow transition">
                            <div class="card-header bg-white border-0 py-3 d-flex justify-content-between align-items-center">
                                <h5 class="card-title fw-bold text-primary mb-0">
                                    <i class="fas fa-user-shield me-2"></i> Personal Details
                                </h5>
                                <button id="btnEditProfile" class="btn btn-sm btn-outline-primary rounded-pill px-3">
                                    <i class="fas fa-pen me-1"></i> Edit Profile
                                </button>
                            </div>
                            <div class="card-body pt-0">
                                <div class="row g-3">
                                    <div class="col-12">
                                        <div class="d-flex align-items-center p-2 rounded-3 bg-primary bg-opacity-10 border border-primary border-opacity-25">
                                            <i class="fas fa-id-badge text-primary me-2 fs-5"></i>
                                            <div>
                                                <small class="text-muted" style="font-size:0.72rem;font-weight:700;">PRN (Permanent Registration No.) — Read Only</small>
                                                <div class="fw-bold text-primary fs-6">${data.prn || 'N/A'}</div>
                                            </div>
                                        </div>
                                    </div>
                                    <div class="col-md-6">
                                        <small class="text-muted d-block" style="font-size:0.72rem;font-weight:700;">FULL NAME</small>
                                        <span class="fs-6 text-dark fw-semibold">${data.name || 'N/A'}</span>
                                    </div>
                                    <div class="col-md-6">
                                        <small class="text-muted d-block" style="font-size:0.72rem;font-weight:700;">EMAIL ADDRESS</small>
                                        <span class="fs-6 text-dark">${data.email || 'N/A'}</span>
                                    </div>
                                    <div class="col-md-6">
                                        <small class="text-muted d-block" style="font-size:0.72rem;font-weight:700;">PHONE NUMBER</small>
                                        <span class="fs-6 text-dark">${data.phone || data.phoneNumber || 'N/A'}</span>
                                    </div>
                                    <div class="col-md-6">
                                        <small class="text-muted d-block" style="font-size:0.72rem;font-weight:700;">REGISTRATION NO</small>
                                        <span class="fs-6 text-dark">${data.regNumber || 'N/A'}</span>
                                    </div>
                                    <div class="col-md-6">
                                        <small class="text-muted d-block" style="font-size:0.72rem;font-weight:700;">DATE OF BIRTH</small>
                                        <span class="fs-6 text-dark">${data.dob || 'N/A'}</span>
                                    </div>
                                    <div class="col-md-6">
                                        <small class="text-muted d-block" style="font-size:0.72rem;font-weight:700;">GENDER</small>
                                        <span class="fs-6 text-dark">${data.gender || 'N/A'}</span>
                                    </div>
                                    <div class="col-md-6">
                                        <small class="text-muted d-block" style="font-size:0.72rem;font-weight:700;">UNIVERSITY</small>
                                        <span class="fs-6 text-dark">${data.university || 'Pune University (SPPU)'}</span>
                                    </div>
                                    <div class="col-md-6">
                                        <small class="text-muted d-block" style="font-size:0.72rem;font-weight:700;">ROLE</small>
                                        <span class="badge bg-secondary rounded-pill">${data.role || 'STUDENT'}</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- 3️⃣ Academic Information Card -->
                    <div class="col-lg-6">
                        <div class="card shadow-sm h-100 border-0 rounded-4 hover-shadow transition">
                            <div class="card-header bg-white border-0 py-3 d-flex justify-content-between align-items-center">
                                <h5 class="card-title fw-bold text-primary mb-0">
                                    <i class="fas fa-graduation-cap me-2"></i> Academic Info
                                </h5>
                                <span class="badge bg-${data.academicStatus === 'Regular' ? 'success' : 'warning'} rounded-pill">${data.academicStatus || 'Regular'}</span>
                            </div>
                            <div class="card-body pt-0">
                                <div class="row g-3">
                                    <div class="col-md-6">
                                        <small class="text-muted d-block" style="font-size:0.72rem;font-weight:700;">COURSE / DEGREE</small>
                                        <span class="fs-6 text-dark fw-semibold">${data.course || 'N/A'}</span>
                                    </div>
                                    <div class="col-md-6">
                                        <small class="text-muted d-block" style="font-size:0.72rem;font-weight:700;">BRANCH / DEPARTMENT</small>
                                        <span class="fs-6 text-dark">${data.branch || 'N/A'}</span>
                                    </div>
                                    <div class="col-md-6">
                                        <small class="text-muted d-block" style="font-size:0.72rem;font-weight:700;">YEAR</small>
                                        <span class="fs-6 text-dark">${data.year || 'N/A'}</span>
                                    </div>
                                    <div class="col-md-6">
                                        <small class="text-muted d-block" style="font-size:0.72rem;font-weight:700;">CURRENT SEMESTER</small>
                                        <span class="fs-6 text-dark">${data.semester || 'N/A'}</span>
                                    </div>
                                    <div class="col-md-6">
                                        <small class="text-muted d-block" style="font-size:0.72rem;font-weight:700;">ENROLLMENT NO</small>
                                        <span class="fs-6 text-dark">${data.enrollmentNo || 'N/A'}</span>
                                    </div>
                                    <div class="col-md-6">
                                        <small class="text-muted d-block" style="font-size:0.72rem;font-weight:700;">CGPA</small>
                                        <span class="badge bg-info text-dark rounded-pill fs-6">${data.cgpa || 'N/A'}</span>
                                    </div>
                                    <div class="col-12">
                                        <div class="mt-2 p-3 bg-light rounded-3 border">
                                            <small class="text-muted d-block mb-1">Performance (Last Sem)</small>
                                            <div class="progress" style="height: 10px;">
                                                <div class="progress-bar bg-primary" role="progressbar" style="width: ${data.cgpa ? Math.min(parseFloat(data.cgpa) * 10, 100) : 85}%"></div>
                                            </div>
                                            <small class="text-primary fw-bold mt-1 d-block text-end">${data.cgpa ? (parseFloat(data.cgpa) * 10).toFixed(0) + '%' : '85%'}</small>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                </div>

                <!-- Edit Profile Modal -->
                <div class="modal fade" id="editProfileModal" tabindex="-1" aria-hidden="true">
                    <div class="modal-dialog modal-lg modal-dialog-centered">
                        <div class="modal-content border-0 rounded-4 shadow-lg">
                            <div class="modal-header bg-primary text-white rounded-top-4">
                                <h5 class="modal-title"><i class="fas fa-user-edit me-2"></i>Edit Profile Information</h5>
                                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                            </div>
                            <div class="modal-body p-4">
                                <div class="alert alert-info py-2 px-3 mb-4 rounded-3" style="font-size:0.85rem;">
                                    <i class="fas fa-lock me-1"></i> <strong>PRN:</strong> ${data.prn || 'N/A'} &nbsp;—&nbsp; This field is permanent and cannot be changed.
                                </div>
                                <h6 class="text-primary fw-bold mb-3 border-bottom pb-2"><i class="fas fa-user me-2"></i>Personal Information</h6>
                                <div class="row g-3 mb-4">
                                    <div class="col-md-6">
                                        <label class="form-label fw-semibold" style="font-size:0.82rem;">Full Name</label>
                                        <input type="text" id="editName" class="form-control rounded-3 ${data.name ? 'bg-light text-muted' : ''}" value="${data.name || ''}" placeholder="Your full name" ${data.name ? 'readonly' : ''}>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label fw-semibold" style="font-size:0.82rem;">Phone Number</label>
                                        <input type="tel" id="editPhone" class="form-control rounded-3" value="${data.phone || data.phoneNumber || ''}" placeholder="+91 XXXXXXXXXX">
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label fw-semibold" style="font-size:0.82rem;">Date of Birth</label>
                                        <input type="date" id="editDob" class="form-control rounded-3" value="${data.dob || ''}">
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label fw-semibold" style="font-size:0.82rem;">Gender</label>
                                        <select id="editGender" class="form-select rounded-3">
                                            <option value="" ${!data.gender ? 'selected' : ''}>-- Select Gender --</option>
                                            <option value="Male" ${data.gender === 'Male' ? 'selected' : ''}>Male</option>
                                            <option value="Female" ${data.gender === 'Female' ? 'selected' : ''}>Female</option>
                                            <option value="Other" ${data.gender === 'Other' ? 'selected' : ''}>Other</option>
                                        </select>
                                    </div>
                                </div>
                                <h6 class="text-primary fw-bold mb-3 border-bottom pb-2"><i class="fas fa-graduation-cap me-2"></i>Academic Information</h6>
                                <div class="row g-3">
                                    <div class="col-md-6">
                                        <label class="form-label fw-semibold" style="font-size:0.82rem;">Course / Degree</label>
                                        <input type="text" id="editCourse" class="form-control rounded-3 ${data.course ? 'bg-light text-muted' : ''}" value="${data.course || ''}" placeholder="e.g. B.Tech, B.E." ${data.course ? 'readonly' : ''}>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label fw-semibold" style="font-size:0.82rem;">Branch / Department</label>
                                        <input type="text" id="editBranch" class="form-control rounded-3 ${data.branch ? 'bg-light text-muted' : ''}" value="${data.branch || ''}" placeholder="e.g. Computer Engineering" ${data.branch ? 'readonly' : ''}>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label fw-semibold" style="font-size:0.82rem;">Year</label>
                                        <select id="editYear" class="form-select rounded-3 ${data.year ? 'bg-light text-muted' : ''}" ${data.year ? 'disabled' : ''}>
                                            <option value="" ${!data.year ? 'selected' : ''}>-- Select Year --</option>
                                            <option value="First Year" ${data.year === 'First Year' ? 'selected' : ''}>1st Year</option>
                                            <option value="Second Year" ${data.year === 'Second Year' ? 'selected' : ''}>2nd Year</option>
                                            <option value="Third Year" ${data.year === 'Third Year' ? 'selected' : ''}>3rd Year</option>
                                            <option value="Final Year" ${data.year === 'Final Year' ? 'selected' : ''}>Final Year (4th)</option>
                                        </select>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label fw-semibold" style="font-size:0.82rem;">Current Semester</label>
                                        <select id="editSemester" class="form-select rounded-3 ${data.semester ? 'bg-light text-muted' : ''}" ${data.semester ? 'disabled' : ''}>
                                            <option value="" ${!data.semester ? 'selected' : ''}>-- Select Semester --</option>
                                            ${[1, 2, 3, 4, 5, 6, 7, 8].map(n => `<option value="Semester ${n}" ${data.semester === `Semester ${n}` ? 'selected' : ''}>${n}${n === 1 ? 'st' : n === 2 ? 'nd' : n === 3 ? 'rd' : 'th'} Semester</option>`).join('')}
                                        </select>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label fw-semibold" style="font-size:0.82rem;">Enrollment No</label>
                                        <input type="text" id="editEnrollmentNo" class="form-control rounded-3 ${data.enrollmentNo ? 'bg-light text-muted' : ''}" value="${data.enrollmentNo || ''}" placeholder="Your enrollment number" ${data.enrollmentNo ? 'readonly' : ''}>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label fw-semibold" style="font-size:0.82rem;">CGPA</label>
                                        <input type="number" id="editCgpa" class="form-control rounded-3 ${data.cgpa ? 'bg-light text-muted' : ''}" min="0" max="10" step="0.1" value="${data.cgpa || ''}" placeholder="e.g. 8.5" ${data.cgpa ? 'readonly' : ''}>
                                    </div>
                                </div>
                            </div>
                            <div class="modal-footer border-0 px-4 pb-4">
                                <button type="button" class="btn btn-light rounded-pill px-4" data-bs-dismiss="modal">Cancel</button>
                                <button type="button" id="btnSaveProfile" class="btn btn-primary rounded-pill px-5 fw-bold">
                                    <i class="fas fa-save me-1"></i> Save Profile
                                </button>
                            </div>
                        </div>
                    </div>
                </div>

                </div>
        `;
    },

    afterRender() {
        // Initialize Bootstrap tooltips
        const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
        tooltipTriggerList.map(el => new bootstrap.Tooltip(el));

        // ── Edit Profile Button → open Bootstrap modal ──────────────────────
        const btnEditProfile = document.getElementById('btnEditProfile');
        if (btnEditProfile) {
            btnEditProfile.addEventListener('click', () => {
                const modal = new bootstrap.Modal(document.getElementById('editProfileModal'));
                modal.show();
            });
        }

        // ── Save Profile Button ──────────────────────────────────────────────
        const btnSaveProfile = document.getElementById('btnSaveProfile');
        if (btnSaveProfile) {
            btnSaveProfile.addEventListener('click', async () => {
                const originalHtml = btnSaveProfile.innerHTML;
                btnSaveProfile.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i> Saving...';
                btnSaveProfile.disabled = true;

                const payload = {
                    name: document.getElementById('editName')?.value.trim() || '',
                    phoneNumber: document.getElementById('editPhone')?.value.trim() || '',
                    dob: document.getElementById('editDob')?.value || '',
                    gender: document.getElementById('editGender')?.value || '',
                    major: document.getElementById('editCourse')?.value.trim() || '',
                    department: document.getElementById('editBranch')?.value.trim() || '',
                    year: document.getElementById('editYear')?.value || '',
                    semester: document.getElementById('editSemester')?.value || '',
                    enrollmentNo: document.getElementById('editEnrollmentNo')?.value.trim() || '',
                    cgpa: document.getElementById('editCgpa')?.value || '',
                };

                try {
                    await API.updateProfileInfo(payload);
                    // Close modal
                    bootstrap.Modal.getInstance(document.getElementById('editProfileModal'))?.hide();
                    // Reload data and re-render
                    await this.init();
                    const container = document.getElementById('page-container');
                    if (container) container.innerHTML = this.render();
                    this.afterRender();
                    // Small toast-style notification
                    const toast = document.createElement('div');
                    toast.className = 'position-fixed bottom-0 end-0 m-4 alert alert-success shadow rounded-3 py-2 px-4';
                    toast.style.zIndex = '9999';
                    toast.innerHTML = '<i class="fas fa-check-circle me-2"></i> Profile saved successfully!';
                    document.body.appendChild(toast);
                    setTimeout(() => toast.remove(), 3000);
                } catch (error) {
                    alert(`❌ Failed to save profile: ${error.message}`);
                    console.error('Save failed:', error);
                } finally {
                    if (document.body.contains(btnSaveProfile)) {
                        btnSaveProfile.innerHTML = originalHtml;
                        btnSaveProfile.disabled = false;
                    }
                }
            });
        }



        // Attach listener for file upload
        const input = document.getElementById('profileUpload');
        if (input) {
            input.addEventListener('change', async (e) => {
                const file = e.target.files[0];
                if (!file) return;

                // Validate file type
                if (!file.type.startsWith("image/")) {
                    alert("Please select a valid image file.");
                    return;
                }

                // Create FormData for backend (expects 'passportPhoto')
                const formData = new FormData();
                formData.append('passportPhoto', file);

                // Show loading state
                const btn = input.nextElementSibling; // The button
                const icon = btn.querySelector('i');
                const originalClass = icon.className;

                icon.className = "fas fa-spinner fa-spin text-white";
                btn.disabled = true;

                try {
                    await API.uploadDocuments(formData);
                    // Refresh profile data to show new image
                    await this.init();
                    // Re-render
                    const container = document.getElementById('page-container');
                    if (container) container.innerHTML = this.render();
                    this.afterRender(); // Re-attach listeners

                    alert("✅ Profile photo updated successfully!");
                } catch (error) {
                    alert(`❌ Failed to update profile photo: ${error.message}`);
                    console.error("Upload failed:", error);
                } finally {
                    icon.className = originalClass;
                    btn.disabled = false;
                }
            });
        }

        // Attach listeners for document uploads
        const docUploadBtns = document.querySelectorAll('.btn-upload-doc');
        docUploadBtns.forEach(btn => {
            btn.addEventListener('click', (e) => {
                const targetId = e.currentTarget.getAttribute('data-target');
                document.getElementById(targetId).click();
            });
        });

        const docInputs = document.querySelectorAll('.doc-upload-input');
        docInputs.forEach(input => {
            input.addEventListener('change', async (e) => {
                const file = e.target.files[0];
                if (!file) return;

                const docType = e.target.getAttribute('data-doctype');
                const formData = new FormData();
                formData.append(docType, file);

                // Show loading on the corresponding button
                const btn = document.querySelector(`button[data-target="${e.target.id}"]`);
                const originalHtml = btn.innerHTML;
                btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
                btn.disabled = true;

                try {
                    await API.uploadDocuments(formData);
                    await this.init();
                    const container = document.getElementById('page-container');
                    if (container) container.innerHTML = this.render();
                    this.afterRender();
                    alert(`✅ ${file.name} uploaded successfully!`);
                } catch (error) {
                    alert(`❌ Failed to upload document: ${error.message}`);
                    console.error("Upload failed:", error);
                } finally {
                    if (document.body.contains(btn)) {
                        btn.innerHTML = originalHtml;
                        btn.disabled = false;
                    }
                }
            });
        });

        const docDeleteBtns = document.querySelectorAll('.btn-delete-doc');
        docDeleteBtns.forEach(btn => {
            btn.addEventListener('click', async (e) => {
                const docType = e.currentTarget.getAttribute('data-doctype');
                if (!confirm('Are you sure you want to delete this document?')) return;

                const originalHtml = btn.innerHTML;
                btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
                btn.disabled = true;

                try {
                    await API.deleteDocument(docType);
                    await this.init();
                    const container = document.getElementById('page-container');
                    if (container) container.innerHTML = this.render();
                    this.afterRender();
                    alert('✅ Document deleted successfully!');
                } catch (error) {
                    alert(`❌ Failed to delete document: ${error.message}`);
                    console.error("Delete failed:", error);
                } finally {
                    if (document.body.contains(btn)) {
                        btn.innerHTML = originalHtml;
                        btn.disabled = false;
                    }
                }
            });
        });

        // Attach listener for profile submission
        const submitBtn = document.getElementById('btnSubmitProfile');
        if (submitBtn) {
            submitBtn.addEventListener('click', async () => {
                const originalHtml = submitBtn.innerHTML;
                submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i> Submitting...';
                submitBtn.disabled = true;

                try {
                    const res = await API.completeProfile();
                    alert(`✅ ${res.message || 'Profile submitted for verification successfully!'}`);
                    await this.init();
                    const container = document.getElementById('page-container');
                    if (container) container.innerHTML = this.render();
                    this.afterRender();
                } catch (error) {
                    alert(`❌ Profile submission failed: ${error.message}`);
                    console.error("Submission failed:", error);
                } finally {
                    if (document.body.contains(submitBtn)) {
                        submitBtn.innerHTML = originalHtml;
                        submitBtn.disabled = false;
                    }
                }
            });
        }
    }
};
