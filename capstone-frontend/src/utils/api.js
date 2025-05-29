// API Configuration
export const API_BASE_URL = 'https://timed-utd9.onrender.com/api';

// Helper function to construct API URLs
export const getApiUrl = (endpoint) => {
  return `${API_BASE_URL}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;
}; 