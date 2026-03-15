/**
 * Theme Manager for SaaS Student Dashboard
 * Handles Light/Dark mode toggling and persistence via localStorage.
 */
export const ThemeManager = {
    init() {
        // Check for saved theme or system preference
        if (localStorage.theme === 'dark' || (!('theme' in localStorage) && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
            document.documentElement.classList.add('dark');
        } else {
            document.documentElement.classList.remove('dark');
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
