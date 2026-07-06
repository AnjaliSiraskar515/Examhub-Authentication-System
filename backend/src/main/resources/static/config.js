// ============================================================
// 🌐 GLOBAL BACKEND CONFIGURATION
// Auto-detects environment so you never have to change it!
// ============================================================

window.GLOBAL_API_BASE = (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1')
    ? "http://localhost:8081" 
    : "https://examhub-authentication-system.onrender.com";

// Keep API_BASE_URL for script.js backwards compatibility
const API_BASE_URL = window.GLOBAL_API_BASE;
