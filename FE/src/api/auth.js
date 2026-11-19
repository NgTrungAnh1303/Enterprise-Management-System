import http from './http';

export const authApi = {
  login: (credentials) => http.post('/auth/login', credentials),
  register: (data) => http.post('/auth/register', data),
  logout: () => http.post('/auth/logout'),
  getMe: () => http.get('/auth/me'),
};

export default authApi;
