import axios from 'axios';

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export const http = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

http.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem('ems_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error.response ? error.response.status : 0;
    const data = error.response?.data || {};

    const normalizedError = {
      status,
      code: data.code || (status === 0 ? 'NETWORK_ERROR' : 'UNKNOWN_ERROR'),
      message: data.message || (status === 0 ? 'Không thể kết nối đến máy chủ.' : 'Đã có lỗi xảy ra.'),
      fieldErrors: data.fieldErrors || {},
      requestId: data.requestId || null,
      timestamp: data.timestamp || new Date().toISOString(),
      response: error.response,
      data: error.response?.data,
    };

    if (status === 401) {

      sessionStorage.removeItem('ems_token');
      sessionStorage.removeItem('ems_user');

      const path = window.location.pathname;
      const isPublicPath = path === '/' || path.startsWith('/login') || path.startsWith('/register') || path === '/401' || path === '/403' || path === '/404';

      if (!isPublicPath) {
        const currentPath = window.location.pathname + window.location.search;
        const safeReturnUrl = currentPath.startsWith('/') && !currentPath.startsWith('//')
          ? encodeURIComponent(currentPath)
          : '';
        const targetUrl = safeReturnUrl ? `/login?returnUrl=${safeReturnUrl}&sessionExpired=1` : '/login?sessionExpired=1';
        window.location.href = targetUrl;
      }
    }

    return Promise.reject(normalizedError);
  }
);

export default http;

