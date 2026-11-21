import http from './http';

export const workOrdersApi = {
  getWorkOrders: (params) => http.get('/work-orders', { params }),
  getWorkOrderById: (id) => http.get(`/work-orders/${id}`),
  createWorkOrder: (data) => http.post('/work-orders', data),
  assignWorkOrder: (id, data) => http.post(`/work-orders/${id}/assignment`, data),
  startWorkOrder: (id) => http.post(`/work-orders/${id}/start`),
  updateResults: (id, data) => http.put(`/work-orders/${id}/results`, data),
  submitWorkOrder: (id) => http.post(`/work-orders/${id}/submit`),
  acceptWorkOrder: (id, data) => http.post(`/work-orders/${id}/accept`, data),
  adjustWorkOrder: (id, data) => http.post(`/work-orders/${id}/adjustments`, data),
  getAdjustments: (id) => http.get(`/work-orders/${id}/adjustments`),
};

export default workOrdersApi;

