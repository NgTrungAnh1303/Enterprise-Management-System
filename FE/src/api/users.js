import http from './http';

export const usersApi = {
  getUsers: (params) => http.get('/users', { params }),
  getActiveTechnicians: () => http.get('/users/technicians'),
  createUser: (data) => http.post('/users', data),
  updateUser: (id, data) => http.patch(`/users/${id}`, data),
  approveUser: (id) => http.post(`/users/${id}/approve`),
  rejectUser: (id) => http.post(`/users/${id}/reject`),
};

export default usersApi;
