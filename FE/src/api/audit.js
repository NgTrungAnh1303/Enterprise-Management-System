import http from './http';

export const auditApi = {
  getAuditLogs: (params) => http.get('/audit-logs', { params }),
};

export default auditApi;

