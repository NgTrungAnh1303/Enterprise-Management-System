import http from './http';

export const equipmentApi = {
  getEquipment: (params) => http.get('/equipment', { params }),
  getEquipmentById: (id) => http.get(`/equipment/${id}`),
  createEquipment: (data) => http.post('/equipment', data),
  updateEquipment: (id, data) => http.put(`/equipment/${id}`, data),
  getEquipmentHistory: (id, params) => http.get(`/equipment/${id}/history`, { params }),
  getEquipmentPlan: (id) => http.get(`/equipment/${id}/plan`),
  updateEquipmentPlan: (id, data) => http.put(`/equipment/${id}/plan`, data),
  proposeOutOfService: (id, data) => http.post(`/equipment/${id}/status-requests`, data),
  getStatusRequestsByEquipment: (id) => http.get(`/equipment/${id}/status-requests`),
};

export default equipmentApi;

