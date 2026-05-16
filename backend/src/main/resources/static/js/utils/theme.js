/**
 * Theme Manager for SaaS Student Dashboard
 * Handles Light/Dark mode toggling and persistence via localStorage.
 */
export const ThemeManager = {
    init() {
        // Default is LIGHT theme. Only apply dark if user explicitly chose it.
        if (localStorage.theme === 'dark') {
            document.documentElement.classList.add('dark');
        } else {
            document.documentElement.classList.remove('dark');
            // Ensure the preference is explicitly set to light for new sessions
            if (!localStorage.theme) {
                localStorage.theme = 'light';
            }
        }
    },

    toggle() {
        if (document.documentElement.classList.contains('dark')) {
            document.documentElement.classList.remove('dark');
            localStorage.theme = 'light';
        } else {
            document.documentElement.classList.add('dark');
            localStorage.theme = 'dark';
        }
        return localStorage.theme;
    },

    isDark() {
        return document.documentElement.classList.contains('dark');
    }
};
