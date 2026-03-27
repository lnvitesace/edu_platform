import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Load test: simulate normal production traffic
export const options = {
  stages: [
    { duration: '1m', target: 20 },   // ramp up to 20 users
    { duration: '3m', target: 20 },   // hold at 20 users
    { duration: '1m', target: 50 },   // ramp up to 50 users
    { duration: '3m', target: 50 },   // hold at 50 users
    { duration: '2m', target: 0 },    // ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000', 'p(99)<2000'],
    http_req_failed: ['rate<0.05'],
    'http_req_duration{name:login}': ['p(95)<800'],
    'http_req_duration{name:courses}': ['p(95)<600'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://gateway-service:8080';

const loginFailRate = new Rate('login_fail_rate');
const courseDuration = new Trend('course_list_duration');

export default function () {
  group('Public APIs', () => {
    // Course listing (most frequent)
    const coursesRes = http.get(`${BASE_URL}/api/courses`, {
      tags: { name: 'courses' },
    });
    check(coursesRes, {
      'courses 200': (r) => r.status === 200,
    });
    courseDuration.add(coursesRes.timings.duration);

    // Course categories
    const catRes = http.get(`${BASE_URL}/api/courses/categories`, {
      tags: { name: 'categories' },
    });
    check(catRes, {
      'categories 200': (r) => r.status === 200,
    });
  });

  group('Auth Flow', () => {
    // Login
    const loginPayload = JSON.stringify({
      usernameOrEmail: 'testuser2',
      password: 'Test123456',
    });
    const loginRes = http.post(`${BASE_URL}/api/auth/login`, loginPayload, {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'login' },
    });

    const loginOk = check(loginRes, {
      'login 200': (r) => r.status === 200,
      'login has token': (r) => {
        try {
          return JSON.parse(r.body).accessToken !== undefined;
        } catch {
          return false;
        }
      },
    });
    loginFailRate.add(!loginOk);

    // Authenticated requests
    if (loginOk) {
      const token = JSON.parse(loginRes.body).accessToken;
      const authHeaders = {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      };

      // Profile
      http.get(`${BASE_URL}/api/users/me`, {
        headers: authHeaders,
        tags: { name: 'profile' },
      });
    }
  });

  sleep(Math.random() * 2 + 1); // 1-3s between iterations
}
