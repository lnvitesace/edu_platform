import http from 'k6/http';
import { check, sleep } from 'k6';

// Smoke test: verify basic functionality under minimal load
export const options = {
  vus: 1,
  duration: '30s',
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://gateway-service:8080';

export default function () {
  // Health check
  const healthRes = http.get(`${BASE_URL}/actuator/health`);
  check(healthRes, {
    'health status 200': (r) => r.status === 200,
  });

  // Public course list
  const coursesRes = http.get(`${BASE_URL}/api/courses`);
  check(coursesRes, {
    'courses status 200': (r) => r.status === 200,
  });

  // Course categories
  const categoriesRes = http.get(`${BASE_URL}/api/courses/categories`);
  check(categoriesRes, {
    'categories status 200': (r) => r.status === 200,
  });

  sleep(1);
}
