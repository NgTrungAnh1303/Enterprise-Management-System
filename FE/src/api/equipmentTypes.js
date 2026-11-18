import http from './http';

export const equipmentTypesApi = {
  getEquipmentTypes: () => http.get('/equipment-types'),
  createEquipmentType: (data) => http.post('/equipment-types', data),
  updateEquipmentType: (id, data) => http.put(`/equipment-types/${id}`, data),
  getTypeChecklist: (typeId) => http.get(`/equipment-types/${typeId}/checklist`),
  updateTypeChecklist: (typeId, items) => http.put(`/equipment-types/${typeId}/checklist`, items),
};

export default equipmentTypesApi;

