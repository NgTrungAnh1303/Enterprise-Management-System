import http from './http';

export const statusRequestsApi = {
  decideStatusRequest: (id, data) => http.post(`/status-requests/${id}/decision`, data),
};

export default statusRequestsApi;

