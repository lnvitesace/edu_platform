import axios from 'axios';

const GATEWAY = process.env.API_URL || 'http://localhost:8080';
const USER_SERVICE = process.env.USER_SERVICE_URL || 'http://localhost:8001';
const COURSE_SERVICE = process.env.COURSE_SERVICE_URL || 'http://localhost:8002';

const gatewayApi = axios.create({ baseURL: GATEWAY, headers: { 'Content-Type': 'application/json' } });
const userApi = axios.create({ baseURL: USER_SERVICE, headers: { 'Content-Type': 'application/json' } });
const courseApi = axios.create({ baseURL: COURSE_SERVICE, headers: { 'Content-Type': 'application/json' } });

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

function parseJwtPayload(token) {
  const payload = token.split('.')[1];
  return JSON.parse(Buffer.from(payload, 'base64url').toString());
}

export async function registerUser(userData) {
  try {
    const { data } = await userApi.post('/api/auth/register', userData);
    return data;
  } catch (err) {
    // User already exists — fall back to login
    if (err.response?.status >= 400 && err.response?.status < 500) {
      return loginUser({ usernameOrEmail: userData.username, password: userData.password });
    }

    // Concurrent seeding can transiently hit deadlocks while another run updates the same seeded users.
    // Retry once and then fall back to login if the account is already usable.
    const message = String(err.response?.data?.message || err.message || '').toLowerCase();
    if (err.response?.status === 500 && message.includes('deadlock')) {
      await sleep(500);
      try {
        const { data } = await userApi.post('/api/auth/register', userData);
        return data;
      } catch (retryErr) {
        return loginUser({ usernameOrEmail: userData.username, password: userData.password });
      }
    }

    throw err;
  }
}

export async function loginUser(credentials) {
  try {
    const { data } = await userApi.post('/api/auth/login', credentials);
    return data;
  } catch (err) {
    const message = String(err.response?.data?.message || err.message || '').toLowerCase();
    if (err.response?.status === 500 && message.includes('deadlock')) {
      await sleep(500);
      const { data } = await userApi.post('/api/auth/login', credentials);
      return data;
    }
    throw err;
  }
}

// Course-service direct calls need X-User-Id and X-User-Role headers
// because gateway whitelist skips auth injection for some endpoints
function courseHeaders(token) {
  const claims = parseJwtPayload(token);
  return {
    Authorization: `Bearer ${token}`,
    'X-User-Id': claims.sub,
    'X-User-Role': claims.role || '',
  };
}

export async function getCategories() {
  const { data } = await courseApi.get('/api/courses/categories');
  return data;
}

export async function createCategory(token, categoryData) {
  const { data } = await courseApi.post('/api/courses/categories', categoryData, { headers: courseHeaders(token) });
  return data;
}

export async function getCourses() {
  const { data } = await courseApi.get('/api/courses?size=100');
  return data;
}

export async function createCourse(token, courseData) {
  const { data } = await courseApi.post('/api/courses', courseData, { headers: courseHeaders(token) });
  return data;
}

export async function createChapter(token, courseId, chapterData) {
  const { data } = await courseApi.post(`/api/courses/${courseId}/chapters`, chapterData, { headers: courseHeaders(token) });
  return data;
}

export async function createLesson(token, chapterId, lessonData) {
  const { data } = await courseApi.post(`/api/courses/chapters/${chapterId}/lessons`, lessonData, { headers: courseHeaders(token) });
  return data;
}

export async function publishCourse(token, courseId) {
  const { data } = await courseApi.put(`/api/courses/${courseId}/publish`, {}, { headers: courseHeaders(token) });
  return data;
}
