import axios from 'axios';

export const API_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '');

const api = axios.create({ baseURL: `${API_URL}/api` });

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 && !err.config?.url?.includes('/auth/login')) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      if (window.location.pathname !== '/login') window.location.href = '/login';
    }
    return Promise.reject(err);
  },
);

/** Lay thong bao loi tieng Viet tu backend. */
export function errorMessage(err) {
  const data = err?.response?.data;
  if (data?.message) return data.message;
  if (err?.message === 'Network Error') return 'Không kết nối được máy chủ, kiểm tra mạng hoặc địa chỉ backend';
  return 'Đã xảy ra lỗi, vui lòng thử lại';
}

export function errorDetails(err) {
  return err?.response?.data?.details || [];
}

/** Mo file goc da luu tren may chu (can token nen tai bang axios). */
export async function openFile(path) {
  const res = await api.get('/files', { params: { path }, responseType: 'blob' });
  const url = URL.createObjectURL(res.data);
  window.open(url, '_blank');
  setTimeout(() => URL.revokeObjectURL(url), 60000);
}

export default api;
