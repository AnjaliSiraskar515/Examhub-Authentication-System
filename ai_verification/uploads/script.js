// Common JS used across pages - minimal helpers

async function postJson(url, payload) {
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return res.json();
}

async function getJson(url) {
  const res = await fetch(url);
  return res.json();
}

// -----------------------
// AUTH PORTAL PAGE LOGIC
// -----------------------

/* ===========================================================
   ExamHub – Central Auth JS (Login + Registration)
   Supports: Student | Supervisor | Admin | Super Admin
   Handles: OTP Email/Phone, Password Toggle
   =========================================================== */

let verifiedEmail = false, verifiedPhone = false;

/* ---------- Tab Switch ---------- */
function switchTab(tab) {
  document.getElementById("loginTab").classList.toggle("active", tab === "login");
  document.getElementById("registerTab").classList.toggle("active", tab === "register");
  document.getElementById("loginForm").classList.toggle("hidden", tab !== "login");
  document.getElementById("registerForm").classList.toggle("hidden", tab !== "register");
}

/* ---------- Role-based Login ---------- */
document.getElementById("loginRole").addEventListener("change", function () {
  const role = this.value;
  const student = document.getElementById("studentLoginFields");
  const authority = document.getElementById("authoritySteps");
  if (role === "student") {
    student.classList.remove("hidden");
    authority.classList.add("hidden");
    document.getElementById("loginButton").disabled = false;
  } else if (role) {
    student.classList.add("hidden");
    authority.classList.remove("hidden");
    document.getElementById("loginButton").disabled = true;
  }
});

/* ===========================================================
   UNIVERSAL OTP SEND + VERIFY (Login & Registration)
   =========================================================== */
async function sendOTP(type) {
  let value, btn, msg, section;

  switch (type) {
    /* ---- Login (Authority) ---- */
    case "email":
      value = get("authEmail"); btn = get("sendEmailOtpBtn");
      msg = get("emailOtpMsg"); section = get("emailOtpSection"); break;
    case "phone":
      value = get("authPhone"); btn = get("sendPhoneOtpBtn");
      msg = get("phoneOtpMsg"); section = get("phoneOtpSection"); break;

    /* ---- Student Registration ---- */
    case "regEmailStudent":
      value = get("regEmailStudent"); btn = get("sendRegEmailOtpBtnStudent");
      msg = get("regEmailOtpMsgStudent"); section = get("regEmailOtpSectionStudent"); break;
    case "regPhoneStudent":
      value = get("regPhoneStudent"); btn = get("sendRegPhoneOtpBtnStudent");
      msg = get("regPhoneOtpMsgStudent"); section = get("regPhoneOtpSectionStudent"); break;

    /* ---- Admin/Supervisor Registration ---- */
    case "regEmailAuthority":
      value = get("regEmailAuthority"); btn = get("sendRegEmailOtpBtnAuthority");
      msg = get("regEmailOtpMsgAuthority"); section = get("regEmailOtpSectionAuthority"); break;
    case "regPhoneAuthority":
      value = get("regPhoneAuthority"); btn = get("sendRegPhoneOtpBtnAuthority");
      msg = get("regPhoneOtpMsgAuthority"); section = get("regPhoneOtpSectionAuthority"); break;
  }

  if (!value) return alert(`Enter your ${type.includes("Email") ? "email" : "phone"} first!`);
  msg.textContent = "⏳ Sending OTP...";
  msg.className = "text-xs text-gray-500 mt-1";

  try {
    const res = await fetch(`http://localhost:8080/api/otp/send/${type.includes("Email") ? "email" : "phone"}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ [type.includes("Email") ? "email" : "phone"]: value }),
    });

    if (res.ok) {
      msg.textContent = "✅ OTP sent successfully. Please check your " + (type.includes("Email") ? "email." : "phone.");
      msg.className = "text-xs text-green-600 mt-1";
      section.classList.remove("hidden");
      btn.disabled = true;
      btn.classList.add("opacity-50", "cursor-not-allowed");
    } else {
      msg.textContent = "❌ Failed to send OTP. Try again.";
      msg.className = "text-xs text-red-600 mt-1";
    }
  } catch (err) {
    console.error(err);
    msg.textContent = "⚠️ Server error. Please try again.";
    msg.className = "text-xs text-red-600 mt-1";
  }
}

async function verifyOTP(type) {
  let otpValue, identifier, msg;

  switch (type) {
    case "email":
      otpValue = get("emailOtp"); identifier = get("authEmail"); msg = get("emailOtpMsg"); break;
    case "phone":
      otpValue = get("phoneOtp"); identifier = get("authPhone"); msg = get("phoneOtpMsg"); break;
    case "regEmailStudent":
      otpValue = get("regEmailOtpStudent"); identifier = get("regEmailStudent"); msg = get("regEmailOtpMsgStudent"); break;
    case "regPhoneStudent":
      otpValue = get("regPhoneOtpStudent"); identifier = get("regPhoneStudent"); msg = get("regPhoneOtpMsgStudent"); break;
    case "regEmailAuthority":
      otpValue = get("regEmailOtpAuthority"); identifier = get("regEmailAuthority"); msg = get("regEmailOtpMsgAuthority"); break;
    case "regPhoneAuthority":
      otpValue = get("regPhoneOtpAuthority"); identifier = get("regPhoneAuthority"); msg = get("regPhoneOtpMsgAuthority"); break;
  }

  msg.textContent = "⏳ Verifying OTP...";
  msg.className = "text-xs text-gray-500 mt-1";

  try {
    const res = await fetch(`http://localhost:8080/api/otp/verify/${type.includes("Email") ? "email" : "phone"}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ [type.includes("Email") ? "email" : "phone"]: identifier, otp: otpValue }),
    });

    const data = await res.json();
    if (data.verified) {
      msg.textContent = "✅ Verified successfully!";
      msg.className = "text-xs text-green-600 mt-1";
      disable(`${type}Otp`);
    } else {
      msg.textContent = "❌ Invalid or expired OTP.";
      msg.className = "text-xs text-red-600 mt-1";
    }
  } catch (err) {
    console.error(err);
    msg.textContent = "⚠️ Error verifying OTP.";
    msg.className = "text-xs text-red-600 mt-1";
  }
}

/* ---------- Login Submit ---------- */
document.getElementById("signinForm").addEventListener("submit", (e) => {
  e.preventDefault();
  const role = get("loginRole").value;
  switch (role) {
    case "student": location.href = "student_dashboard.html"; break;
    case "supervisor": location.href = "supervisor_dashboard.html"; break;
    case "admin": location.href = "admin_dashboard.html"; break;
    case "superadmin": location.href = "super_admin_dashboard.html"; break;
  }
});

/* ---------- Registration Toggle ---------- */
function updateRegistration() {
  const role = get("regRole").value;
  get("studentReg").classList.toggle("hidden", role !== "student");
  get("authorityReg").classList.toggle("hidden", role === "student" || role === "");
}

/* ---------- Registration Submit ---------- */
document.getElementById("signupForm").addEventListener("submit", (e) => {
  e.preventDefault();

  // Check OTP verification
  const studentEmailOk = get("regEmailOtpStudent")?.disabled || false;
  const studentPhoneOk = get("regPhoneOtpStudent")?.disabled || false;
  const authEmailOk = get("regEmailOtpAuthority")?.disabled || false;
  const authPhoneOk = get("regPhoneOtpAuthority")?.disabled || false;

  if (!(studentEmailOk || authEmailOk) || !(studentPhoneOk || authPhoneOk)) {
    alert("Please verify both Email and Phone OTPs before registration!");
    return;
  }

  alert("Registration submitted successfully! Await verification from higher authority.");
  switchTab("login");
});

/* ---------- Password Toggle ---------- */
function togglePassword(id, btn) {
  const input = get(id);
  const icon = btn.querySelector("i");
  if (input.type === "password") {
    input.type = "text";
    icon.classList.replace("fa-eye", "fa-eye-slash");
  } else {
    input.type = "password";
    icon.classList.replace("fa-eye-slash", "fa-eye");
  }
}

/* ---------- Small Helper ---------- */
function get(id) { return document.getElementById(id); }
function disable(id) { const el = get(id); if (el) el.disabled = true; }
