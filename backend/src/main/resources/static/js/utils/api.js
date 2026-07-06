const API_BASE_URL = (window.GLOBAL_API_BASE || "") + '/api/student';

// Get auth headers with JWT token
const getHeaders = () => {
    const token = localStorage.getItem('token') || localStorage.getItem('jwtToken');
    return {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {})
    };
};

// Central handler: if a response is 401/403, the token is invalid — force re-login
const handleAuthError = (response) => {
    if (response.status === 401 || response.status === 403) {
        console.warn('⚠️ Session expired or unauthorized. Redirecting to login...');
        localStorage.clear();
        window.location.href = 'index.html';
        return true;
    }
    return false;
};

export const API = {
    // Exams
    getLocalExams: async () => {
        const token = localStorage.getItem('token') || localStorage.getItem('jwtToken');

        if (!token) throw new Error('No authentication token found');

        // Real API call
        try {
            const response = await fetch(`${API_BASE_URL}/exams`, { headers: getHeaders() });
            if (!response.ok) throw new Error('Failed to fetch exams');
            return await response.json();
        } catch (error) {
            console.error('Error fetching exams:', error);
            // Return empty array or throw based on preference
            return [];
        }
    },

    // Registrations
    registerForExam: async (registrationData) => {
        const token = localStorage.getItem('token') || localStorage.getItem('jwtToken');

        if (!token) throw new Error('No authentication token found');

        // Real API call for production
        const response = await fetch(`${API_BASE_URL}/registrations`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(registrationData)
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || 'Registration failed');
        }
        return await response.json();
    },

    getRegistrations: async (studentId) => {
        try {
            const response = await fetch(`${API_BASE_URL}/registrations?studentId=${studentId}`, { headers: getHeaders() });
            if (!response.ok) throw new Error('Failed to fetch registrations');
            return await response.json();
        } catch (error) {
            console.error('Error fetching registrations:', error);
            return [];
        }
    },

    // Notifications
    getNotifications: async () => {
        try {
            const response = await fetch(`${API_BASE_URL}/notifications`, { headers: getHeaders() });
            if (!response.ok) throw new Error('Failed to fetch notifications');
            return await response.json();
        } catch (error) {
            console.error('Error fetching notifications:', error);
            return [];
        }
    },

    // Profile
    getProfile: async () => {
        try {
            const response = await fetch(`${API_BASE_URL.replace('/student', '/profile')}/info`, { headers: getHeaders() });
            if (handleAuthError(response)) return null; // Will redirect to login
            if (!response.ok) throw new Error('Failed to fetch profile');
            return await response.json();
        } catch (error) {
            console.error('Error fetching profile:', error);
            return null;
        }
    },

    // Update editable profile info
    updateProfileInfo: async (payload) => {
        const token = localStorage.getItem('token') || localStorage.getItem('jwtToken');
        try {
            const response = await fetch(`${API_BASE_URL.replace('/student', '/profile')}/update-info`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...(token ? { 'Authorization': `Bearer ${token}` } : {})
                },
                body: JSON.stringify(payload)
            });
            if (!response.ok) {
                const err = await response.json().catch(() => ({}));
                throw new Error(err.error || err.message || 'Failed to update profile');
            }
            return await response.json();
        } catch (error) {
            console.error('Error updating profile:', error);
            throw error;
        }
    },

    // Update semester CGPA records
    updateSemesterCgpa: async (payload) => {
        const token = localStorage.getItem('token') || localStorage.getItem('jwtToken');
        try {
            const response = await fetch(`${API_BASE_URL.replace('/student', '/profile')}/update-cgpa`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...(token ? { 'Authorization': `Bearer ${token}` } : {})
                },
                body: JSON.stringify(payload)
            });
            if (!response.ok) {
                const err = await response.json().catch(() => ({}));
                throw new Error(err.error || err.message || 'Failed to update CGPA');
            }
            return await response.json();
        } catch (error) {
            console.error('Error updating CGPA:', error);
            throw error;
        }
    },

    // Student Stats
    getStudentStats: async () => {
        try {
            const response = await fetch(`${API_BASE_URL}/stats`, { headers: getHeaders() });
            if (handleAuthError(response)) return { totalRegistered: 0, pendingVerifications: 0, approvedExams: 0, upcomingExams: 0 };
            if (!response.ok) throw new Error('Failed to fetch stats');
            return await response.json();
        } catch (error) {
            console.error('Error fetching stats:', error);
            return { totalRegistered: 0, pendingVerifications: 0, approvedExams: 0, upcomingExams: 0 };
        }
    },

    // Upload Documents (for Profile Photo and others)
    uploadDocuments: async (formData) => {
        const token = localStorage.getItem('token') || localStorage.getItem('jwtToken');
        try {
            const response = await fetch(`${API_BASE_URL.replace('/student', '/profile')}/upload`, {
                method: 'POST',
                headers: {
                    ...(token ? { 'Authorization': `Bearer ${token}` } : {})
                    // Content-Type is set automatically by browser for FormData
                },
                body: formData
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.error || errorData.message || 'Failed to upload document');
            }
            return await response.json();
        } catch (error) {
            console.error('Error uploading document:', error);
            throw error;
        }
    },

    // Delete Document
    deleteDocument: async (docType) => {
        const token = localStorage.getItem('token') || localStorage.getItem('jwtToken');
        try {
            const response = await fetch(`${API_BASE_URL.replace('/student', '/profile')}/document/${docType}`, {
                method: 'DELETE',
                headers: {
                    ...(token ? { 'Authorization': `Bearer ${token}` } : {})
                }
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.error || errorData.message || 'Failed to delete document');
            }
            return await response.json();
        } catch (error) {
            console.error('Error deleting document:', error);
            throw error;
        }
    },

    // Complete Profile Verification
    completeProfile: async () => {
        try {
            const response = await fetch(`${API_BASE_URL.replace('/student', '/profile')}/complete`, {
                method: 'POST',
                headers: getHeaders()
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.error || errorData.message || 'Failed to complete profile');
            }
            return await response.json();
        } catch (error) {
            console.error('Error completing profile:', error);
            throw error;
        }
    }
};
