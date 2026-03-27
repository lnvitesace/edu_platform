/**
 * API 层：统一的 HTTP 客户端与服务封装
 * 所有请求走 /api 前缀，由 Vite 开发代理转发到网关服务 (localhost:8080)。
 * 认证采用 JWT access/refresh token 双 token 机制。
 */
import axios from 'axios';

const API_BASE_URL = '/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

// 请求拦截：自动注入 JWT token，所有业务请求无需手动传递认证头
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

/**
 * 响应拦截：透明 token 刷新机制
 * 当 access token 过期（401）时，自动用 refresh token 换取新 token 并重试原请求。
 * _retry 标记防止刷新失败后的无限重试循环。
 * 排除 /auth/ 请求避免登录/注册本身的 401 触发刷新逻辑。
 * 刷新失败则清除本地状态并强制跳转登录页（用 window.location 而非 navigate，
 * 因为拦截器在 React 组件树之外，无法访问路由 hook）。
 */
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    const isAuthRequest = originalRequest.url?.includes('/auth/');
    if (error.response?.status === 401 && !originalRequest._retry && !isAuthRequest) {
      originalRequest._retry = true;

      try {
        const refreshToken = localStorage.getItem('refreshToken');
        const { data } = await axios.post('/api/auth/refresh', { refreshToken });

        localStorage.setItem('accessToken', data.accessToken);
        localStorage.setItem('refreshToken', data.refreshToken);
        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
        return api(originalRequest);
      } catch {
        clearAuthStorage();
        window.location.href = '/login';
        return Promise.reject(error);
      }
    }
    return Promise.reject(error);
  }
);

const clearAuthStorage = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
};

const storeAuthResponse = (data) => {
  if (data.accessToken) {
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    localStorage.setItem('user', JSON.stringify(data.user));
  }
};

export const authService = {
  register: async (userData) => {
    const { data } = await api.post('/auth/register', userData);
    storeAuthResponse(data);
    return data;
  },

  login: async (credentials) => {
    const { data } = await api.post('/auth/login', credentials);
    storeAuthResponse(data);
    return data;
  },

  // 无论服务端注销是否成功，都必须清除本地凭证，确保客户端状态一致
  logout: async () => {
    try {
      await api.post('/auth/logout');
    } finally {
      clearAuthStorage();
    }
  },

  getCurrentUser: () => {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
  },
};

export const userService = {
  getProfile: async () => {
    const { data } = await api.get('/users/me');
    return data;
  },

  updateProfile: async (userData) => {
    const { data } = await api.put('/users/me', userData);
    localStorage.setItem('user', JSON.stringify(data));
    return data;
  },

  getUserById: async (id) => {
    const { data } = await api.get(`/users/${id}`);
    return data;
  },
};

export const courseService = {
  getCourses: async (page = 0, size = 12) => {
    const { data } = await api.get(`/courses?page=${page}&size=${size}`);
    return data;
  },

  getCoursesByCategory: async (categoryId, page = 0, size = 12) => {
    const { data } = await api.get(`/courses/category/${categoryId}?page=${page}&size=${size}`);
    return data;
  },

  searchCourses: async (keyword, page = 0, size = 12, categoryId = null) => {
    const params = new URLSearchParams({
      keyword,
      page: String(page),
      size: String(size),
    });
    if (categoryId) {
      params.set('categoryId', categoryId);
    }
    const { data } = await api.get(`/search/courses?${params.toString()}`);
    return data;
  },

  getCourseById: async (id) => {
    const { data } = await api.get(`/courses/${id}`);
    return data;
  },

  getCategories: async () => {
    const { data } = await api.get('/courses/categories');
    return data;
  },

  getLessonById: async (id) => {
    const { data } = await api.get(`/courses/lessons/${id}`);
    return data;
  },
};

export const progressService = {
  updateProgress: async (lessonId, watchedSeconds) => {
    const { data } = await api.post('/progress', { lessonId, watchedSeconds });
    return data;
  },
  getLessonProgress: async (lessonId) => {
    const { data } = await api.get(`/progress/lesson/${lessonId}`);
    return data;
  },
};

export const enrollmentService = {
  enroll: async (courseId) => {
    const { data } = await api.post('/enrollments', { courseId });
    return data;
  },

  checkEnrollment: async (courseId) => {
    const { data } = await api.get(`/enrollments/check?courseId=${courseId}`);
    return data.enrolled;
  },

  getMyEnrollments: async () => {
    const { data } = await api.get('/enrollments/my');
    return data;
  },

  cancelEnrollment: async (courseId) => {
    await api.delete(`/enrollments/${courseId}`);
  },
};

export default api;
