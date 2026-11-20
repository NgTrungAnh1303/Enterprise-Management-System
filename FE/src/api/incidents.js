import http from './http';

export const incidentsApi = {
  getIncidents: (params) => http.get('/incidents', { params }),
  getIncidentById: (id) => http.get(`/incidents/${id}`),
  createIncident: (data) => http.post('/incidents', data),
  classifyIncident: (id, data) => http.post(`/incidents/${id}/classify`, data),
  progressIncident: (id, data) => http.patch(`/incidents/${id}/progress`, data),
  closeIncident: (id) => http.post(`/incidents/${id}/close`),
};

export default incidentsApi;

