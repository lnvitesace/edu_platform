import http from 'k6/http';
import { check, sleep } from 'k6';

// Stress test: find the breaking point
export const options = {
  stages: [
    { duration: '2m', target: 50 },   // normal load
    { duration: '2m', target: 100 },  // moderate stress
    { duration: '2m', target: 200 },  // high stress
    { duration: '2m', target: 300 },  // near breaking point
    { duration: '2m', target: 0 },    // recovery
  ],
  thresholds: {
    http_req_duration: ['p(95)<3000'],
    http_req_failed: ['rate<0.15'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://gateway-service:8080';

export default function () {
  // Mixed read workload simulating real traffic
  const endpoints = [
    { url: `${BASE_URL}/api/courses`, weight: 40 },
    { url: `${BASE_URL}/api/courses/categories`, weight: 20 },
    { url: `${BASE_URL}/actuator/health`, weight: 10 },
    { url: `${BASE_URL}/api/search?q=java`, weight: 30 },
  ];

  const rand = Math.random() * 100;
  let cumulative = 0;
  let selected = endpoints[0];
  for (const ep of endpoints) {
    cumulative += ep.weight;
    if (rand < cumulative) {
      selected = ep;
      break;
    }
  }

  const res = http.get(selected.url);
  check(res, {
    'status not 5xx': (r) => r.status < 500,
    'response time < 5s': (r) => r.timings.duration < 5000,
  });

  sleep(Math.random() * 0.5);
}
