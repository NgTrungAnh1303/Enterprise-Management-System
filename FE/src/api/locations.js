import http from './http';

export const locationsApi = {
  getLocations: () => http.get('/locations'),
  createLocation: (data) => http.post('/locations', data),
  updateLocation: (id, data) => http.put(`/locations/${id}`, data),
};

export default locationsApi;

