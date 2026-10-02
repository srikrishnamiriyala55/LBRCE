import axios from 'axios';

const api = axios.create({
  // Production serves the React build and Spring API from the same Render host.
  // Keep /api as a safe default if a build environment omitted VITE_API_URL.
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: Number(import.meta.env.VITE_API_TIMEOUT_MS) || 30000,
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    // A stale/invalid token may receive either 401 or 403 after a deployment.
    // Clear it and ask the user to authenticate again instead of leaving a broken dashboard.
    if ([401, 403].includes(error.response?.status) && !error.config?.url?.includes('/auth/login') && window.location.pathname !== '/login') {
      // Save current location so login can redirect back after successful auth
      sessionStorage.setItem('redirectAfterLogin', window.location.pathname + window.location.search);
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
