import http from './http';

export const dashboardApi = {
  getDashboard: () => http.get('/dashboard'),
  getAlerts: () => http.get('/alerts'),
};

export default dashboardApi;
