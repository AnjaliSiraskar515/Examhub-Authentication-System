console.log("✅ script.js loaded successfully!");

// ===========================================================
//  GLOBAL CONSTANTS
// ===========================================================
const BACKEND_BASE = "http://localhost:8080/api";

let emailVerified = false;
let phoneVerified = false;
let studentEmailVerified = false;
let otpTimers = {}; // Track resend cooldowns
let studentLoginMethod = "email"; // Default login method for students

// ===========================================================
//  HELPER FUNCTIONS
// ===========================================================
function get(id) {
  return document.getElementById(id);
}

// ===========================================================
//  TAB SWITCHING
// ===========================================================
function switchTab(tab) {
  get("loginTab").classList.toggle("active", tab === "login");
  get("registerTab").classList.toggle("active", tab === "register");
  get("loginForm").classList.toggle("hidden", tab !== "login");
  get("registerForm").classList.toggle("hidden", tab !== "register");
}

// ===========================================================
//  ROLE-BASED LOGIN VISIBILITY
// ===========================================================
document.addEventListener("DOMContentLoaded", () => {
  const roleSelect = get("loginRole");
  const loginBtn = get("loginButton");

  if (roleSelect) {
    roleSelect.addEventListener("change", () => {
      const role = roleSelect.value;
      const student = get("studentLoginFields");
      const authority = get("authoritySteps");

      if (!role) {
        loginBtn.disabled = true;
        return;
      }

      student.classList.toggle("hidden", role !== "STUDENT");
      authority.classList.toggle("hidden", role === "STUDENT");

      // Helper to toggle disabled state
      const toggleInputs = (container, shouldEnable) => {
        container.querySelectorAll("input, select, button").forEach(el => {
          el.disabled = !shouldEnable;
        });
      };

      toggleInputs(student, role === "STUDENT");
      toggleInputs(authority, role !== "STUDENT");

      // Dynamic Label for Supervisor
      const authLabel = authority.querySelector("label");
      const authInput = get("authEmail");

      if (role === "SUPERVISOR") {
        authLabel.textContent = "Official Email or Mobile Number";
        authInput.placeholder = "Enter Email or Phone";
        authInput.type = "text"; // Allow Mobile Number (removes email validation)
      } else {
        authLabel.textContent = "Official Email";
        authInput.placeholder = "email@institution.edu";
        authInput.type = "email"; // Enforce Email Validation for others
      }

      // Hide or Show Institution Admin specific fields
      const instAdminFields = get("institutionAdminFields");
      const authorityPasswordField = get("authorityPasswordField");
      if (instAdminFields && authorityPasswordField) {
        if (role === "UNIVERSITY_ADMIN") {
          instAdminFields.classList.remove("hidden");
          authorityPasswordField.classList.add("hidden");
          // Enable inputs inside instAdminFields explicitly since toggleInputs might have disabled them if container parent was hidden
          toggleInputs(instAdminFields, true);
          // Disable password field so it doesn't get submitted
          toggleInputs(authorityPasswordField, false);
        } else {
          instAdminFields.classList.add("hidden");
          authorityPasswordField.classList.remove("hidden");
          toggleInputs(instAdminFields, false);
          toggleInputs(authorityPasswordField, role !== "STUDENT");
        }
      }

      loginBtn.disabled = false;
      loginBtn.classList.remove("bg-gray-400");
      loginBtn.classList.add("bg-indigo-600", "hover:bg-indigo-700");
    });
  }
});

// ===========================================================
//  TOGGLE STUDENT LOGIN METHOD (Email / Username)
// ===========================================================
function toggleStudentLoginMethod(method) {
  studentLoginMethod = method;
  const emailSection = get("studentEmailLogin");
  const usernameSection = get("studentUsernameLogin");

  if (method === "email") {
    emailSection.classList.remove("hidden");
    usernameSection.classList.add("hidden");
  } else {
    emailSection.classList.add("hidden");
    usernameSection.classList.remove("hidden");
  }
}

// ===========================================================
//  SEND OTP FUNCTION (Stable Final Version)
// ===========================================================
async function sendOTP(type) {
  const map = {
    email: ["authEmail", "emailOtpMsg", "emailOtpSection", "sendEmailOtpBtn", "email"],
    phone: ["authPhone", "phoneOtpMsg", "phoneOtpSection", "sendPhoneOtpBtn", "phone"],
    regEmailStudent: ["regEmailStudent", "regEmailOtpMsgStudent", "regEmailOtpSectionStudent", "sendRegEmailOtpBtnStudent", "email"],
    regPhoneStudent: ["regPhoneStudent", "regPhoneOtpMsgStudent", "regPhoneOtpSectionStudent", "sendRegPhoneOtpBtnStudent", "phone"],
    regEmailAuthority: ["regEmailAuthority", "regEmailOtpMsgAuthority", "regEmailOtpSectionAuthority", "sendRegEmailOtpBtnAuthority", "email"],
    regPhoneAuthority: ["regPhoneAuthority", "regPhoneOtpMsgAuthority", "regPhoneOtpSectionAuthority", "sendRegPhoneOtpBtnAuthority", "phone"],
    studentLoginEmail: ["studentLoginEmail", "studentEmailOtpMsg", "studentEmailOtpSection", "studentSendOtpBtn", "email"],
  };

  const cfg = map[type];
  if (!cfg) return alert("Unknown OTP type!");

  let [inputId, msgId, sectionId, btnId, key] = cfg;
  const value = get(inputId)?.value.trim();
  const msg = get(msgId);
  const btn = get(btnId);
  const section = get(sectionId);

  if (!value) return alert(`Enter your ${key} first!`);

  // ✅ HYBRID LOGIN FIX: Detect if "email" input is actually a Phone Number
  if (type === "email" && /^\d{10}$/.test(value)) {
    console.log("📲 Detected Phone Number for Login OTP");
    key = "phone"; // Switch key to phone
  }

  msg.textContent = "⏳ Sending OTP...";
  msg.className = "text-xs text-gray-500 mt-1";

  try {
    const res = await fetch(`${BACKEND_BASE}/otp/send/${key}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ [key]: value }),
    });

    const data = await res.json(); // Parse JSON first

    if (res.ok && data.success) {
      msg.textContent = `✅ OTP sent to your ${key}.`;
      msg.className = "text-xs text-green-600 mt-1";
      section.classList.remove("hidden");

      // Hide Send OTP button
      btn.classList.add("hidden");

      // Enable OTP field
      const otpInput = section.querySelector("input[type='text']");
      if (otpInput) otpInput.disabled = false;

      // Create Resend OTP button
      const resendBtnId = `${btnId}_resend`;
      let resendBtn = get(resendBtnId);
      if (!resendBtn) {
        resendBtn = document.createElement("button");
        resendBtn.textContent = "Resend OTP";
        resendBtn.id = resendBtnId;
        resendBtn.type = "button";
        resendBtn.className = "ml-2 px-3 py-2 text-sm bg-indigo-600 text-white rounded hover:bg-indigo-700 transition";
        resendBtn.onclick = () => {
          msg.textContent = "⏳ Resending OTP...";
          sendOTP(type);
        };
        btn.parentElement.appendChild(resendBtn);
      }
      resendBtn.classList.remove("hidden");
      msg.textContent = "✅ You can resend OTP anytime.";
    } else {
      msg.textContent = data.message || "❌ Failed to send OTP.";
      msg.className = "text-xs text-red-600 mt-1";
    }
  } catch (err) {
    console.error("⚠️ OTP send error:", err);
    msg.textContent = "⚠️ Network error sending OTP.";
    msg.className = "text-xs text-red-600 mt-1";
  }
}

// ===========================================================
//  VERIFY OTP FUNCTION
// ===========================================================
async function verifyOTP(type) {
  const map = {
    email: ["emailOtp", "authEmail", "emailOtpMsg", "email"],
    phone: ["phoneOtp", "authPhone", "phoneOtpMsg", "phone"],
    regEmailStudent: ["regEmailOtpStudent", "regEmailStudent", "regEmailOtpMsgStudent", "email"],
    regPhoneStudent: ["regPhoneOtpStudent", "regPhoneStudent", "regPhoneOtpMsgStudent", "phone"],
    regEmailAuthority: ["regEmailOtpAuthority", "regEmailAuthority", "regEmailOtpMsgAuthority", "email"],
    regPhoneAuthority: ["regPhoneOtpAuthority", "regPhoneAuthority", "regPhoneOtpMsgAuthority", "phone"],
    studentLoginEmail: ["studentEmailOtp", "studentLoginEmail", "studentEmailOtpMsg", "email"],
  };

  const cfg = map[type];
  if (!cfg) return alert("Unknown OTP type!");

  let [otpId, idInputId, msgId, key] = cfg;
  const otp = get(otpId)?.value.trim();
  const value = get(idInputId)?.value.trim();
  const msg = get(msgId);

  if (!otp || !value) return alert("Enter OTP and corresponding ID.");

  // ✅ HYBRID LOGIN FIX: Detect if "email" input is actually a Phone Number
  let isHybridPhone = false;
  if (type === "email" && /^\d{10}$/.test(value)) {
    console.log("📲 Detected Phone Number for Login Verification");
    key = "phone"; // Switch key to phone
    isHybridPhone = true;
  }

  msg.textContent = "⏳ Verifying OTP...";
  msg.className = "text-xs text-gray-500 mt-1";

  try {
    const res = await fetch(`${BACKEND_BASE}/otp/verify/${key}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ [key]: value, otp }),
    });
    const data = await res.json();

    if (data.verified) {
      msg.textContent = "✅ Verified successfully!";
      msg.className = "text-xs text-green-600 mt-1";
      if (type === "studentLoginEmail") studentEmailVerified = true;

      // ✅ Handle hybrid verification state
      if (key === "email") emailVerified = true;
      if (key === "phone" || isHybridPhone) {
        phoneVerified = true;
        // Also set emailVerified to true to pass the generic check in signup if needed,
        // but specifically for login, it doesn't matter much as login doesn't check this flag for supervisors.
        // However, consistency is good.
        emailVerified = true;
      }

      get(otpId).disabled = true;
    } else {
      msg.textContent = "❌ Invalid or expired OTP.";
      msg.className = "text-xs text-red-600 mt-1";
    }
  } catch (err) {
    console.error("⚠️ Verify error:", err);
    msg.textContent = "⚠️ Verification error.";
    msg.className = "text-xs text-red-600 mt-1";
  }
}

// ===========================================================
//  LOGIN HANDLER
// ===========================================================
document.addEventListener("DOMContentLoaded", () => {
  const form = get("signinForm");
  const loginBtn = get("loginButton");

  if (!form) {
    // console.warn("ℹ️ signinForm not found (expected on dashboard pages).");
    return;
  }

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const role = get("loginRole").value;

    // === STUDENT LOGIN ===
    if (role === "STUDENT") {
      const email = get("studentLoginEmail").value.trim();
      const prn = get("studentLoginPrn")?.value.trim();
      const password = get("studentLoginPassword").value.trim();

      if (!studentEmailVerified) {
        alert("Please verify your email OTP before login.");
        return;
      }
      if (!prn) {
        alert("Please enter your Institution Roll No / PRN.");
        return;
      }

      await loginRequest(email, password, role, prn);
      return;
    }

    // === NON-STUDENT LOGIN ===
    const email = get("authEmail")?.value.trim();

    // === INSTITUTION ADMIN LOGIN ===
    if (role === "UNIVERSITY_ADMIN") {
      const institutionCode = get("institutionCode")?.value.trim();
      const loginKey = get("institutionLoginKey")?.value.trim();

      if (!emailVerified) {
        alert("Please verify your email OTP before login.");
        return;
      }
      if (!institutionCode || !loginKey) {
        alert("Please enter Institution Code and Login Key.");
        return;
      }
      // Send loginKey as password so we only change one method signature, we will send instCode separately.
      await loginRequest(email, loginKey, role, null, institutionCode);
      return;
    }

    // === SUPERVISOR / GENERAL LOGIN ===
    const password = get("loginPassword")?.value.trim();
    await loginRequest(email, password, role);
  });
});

// ===========================================================
//  LOGIN REQUEST FUNCTION
// ===========================================================
async function loginRequest(identifier, password, role = "student", prn = null, institutionCode = null) {
  const loginBtn = get("loginButton");

  if (!identifier || !password) {
    alert("Please enter required details.");
    return;
  }

  try {
    loginBtn.textContent = "Logging in...";
    loginBtn.disabled = true;

    const payload = { email: identifier, password: password, role: role };
    if (role === "STUDENT" && prn) {
      payload.prn = prn;
    }
    if (role === "UNIVERSITY_ADMIN" && institutionCode) {
      payload.institutionCode = institutionCode;
    }

    const res = await fetch(`${BACKEND_BASE}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    const data = await res.json().catch(() => ({}));

    if (res.ok && data.role) {
      alert("✅ Login successful!");

      // Save Token
      if (data.token) {
        localStorage.setItem("token", data.token);
      }

      const userRole = (data.role || role).toUpperCase();
      switch (userRole) {
        case "STUDENT": location.href = "student_dashboard.html"; break;
        case "SUPERVISOR": location.href = "supervisor_dashboard.html"; break;
        case "UNIVERSITY_ADMIN":
        case "ADMIN":
          location.href = "university_dashboard.html"; break;
        case "SUPERADMIN":
        case "SUPER_ADMIN":
        case "SUPER ADMIN":
          location.href = "super_admin_dashboard.html"; break;
        default: alert("Unknown role: " + userRole);
      }
    } else {
      alert("❌ Login failed: " + (data.error || "Invalid credentials"));
    }
  } catch (err) {
    console.error("⚠️ Login error:", err);
    alert("⚠️ Server error during login.");
  } finally {
    loginBtn.textContent = "Login";
    loginBtn.disabled = false;
  }
}

// ===========================================================
//  REGISTRATION HANDLER (With OTP Check)
// ===========================================================
// ===========================================================
//  SAFE REGISTRATION HANDLER (final fix)
// ===========================================================
const signupForm = get("signupForm");

if (signupForm) {
  signupForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    if (!emailVerified || !phoneVerified) {
      alert("Please verify both Email and Mobile OTP before submitting.");
      return;
    }

    const role = get("regRole")?.value || "";
    let payload = { role };

    try {
      if (role === "STUDENT") {
        const nameEl = get("studentReg")?.querySelector("input[name='fullName']");
        const emailEl = get("regEmailStudent");
        const phoneEl = get("regPhoneStudent");
        const passEl = get("regPasswordStudent");
        const fileEl = get("regIdProofStudent");

        payload.name = nameEl?.value?.trim() || "";
        payload.email = emailEl?.value?.trim() || "";
        payload.phone = phoneEl?.value?.trim() || "";
        payload.password = passEl?.value || "";
        payload.idProofName = fileEl?.files?.[0]?.name || null;

      } else {
        const nameEl = get("authorityReg")?.querySelector("input[name='fullName']");
        const emailEl = get("regEmailAuthority");
        const phoneEl = get("regPhoneAuthority");
        const passEl = get("regPasswordAuthority");
        const fileEl = get("regIdProofAuthority");

        payload.name = nameEl?.value?.trim() || "";
        payload.email = emailEl?.value?.trim() || "";
        payload.phone = phoneEl?.value?.trim() || "";
        payload.password = passEl?.value || "";
        payload.idProofName = fileEl?.files?.[0]?.name || null;
      }

      console.log("📦 Registration payload:", payload);

      const res = await fetch(`${BACKEND_BASE}/auth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      const data = await res.json().catch(() => ({}));

      if (res.ok) {
        alert("✅ Registration successful! You can now login.");
        switchTab("login");
        emailVerified = phoneVerified = false;
      } else {
        alert("❌ Registration failed: " + (data.message || data.error || "Error occurred."));
        console.warn("Register failed:", data);
      }
    } catch (err) {
      console.error("⚠️ Registration error:", err);
      alert("⚠️ Something went wrong during registration.");
    }
  });

  // ✅ Registration Role Visibility Logic
  const regRoleSelect = get("regRole");
  if (regRoleSelect) {
    regRoleSelect.addEventListener("change", () => {
      const role = regRoleSelect.value;
      const student = get("studentReg");
      const authority = get("authorityReg");

      student.classList.toggle("hidden", role !== "STUDENT");
      authority.classList.toggle("hidden", role === "STUDENT" || role === "");

      const toggleRegInputs = (container, shouldEnable) => {
        if (!container) return;
        container.querySelectorAll("input, select, button").forEach(el => {
          el.disabled = !shouldEnable;
        });
      };

      toggleRegInputs(student, role === "STUDENT");
      toggleRegInputs(authority, role !== "STUDENT" && role !== "");
    });
  }
} else {
  console.warn("⚠️ signupForm not found in DOM.");
}


// ===========================================================
//  PASSWORD TOGGLE
// ===========================================================
function togglePassword(id, btn) {
  const input = get(id);
  const icon = btn.querySelector("i");
  input.type = input.type === "password" ? "text" : "password";
  icon.classList.toggle("fa-eye");
  icon.classList.toggle("fa-eye-slash");
}

// ✅ Make sure inline handlers in HTML can access functions

window.toggleStudentLoginMethod = toggleStudentLoginMethod;
window.togglePassword = togglePassword;
window.sendOTP = sendOTP;
window.verifyOTP = verifyOTP;
