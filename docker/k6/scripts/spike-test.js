import http from 'k6/http';
import { check, sleep } from 'k6';

// Spike test: sudden traffic burst (e.g. flash sale, viral content)
export const options = {
  stages: [
    { duration: '30s', target: 10 },   // baseline
    { duration: '10s', target: 500 },  // spike!
    { duration: '1m', target: 500 },   // hold spike
    { duration: '10s', target: 10 },   // drop back
    { duration: '2m', target: 10 },    // recovery observation
    { duration: '30s', target: 0 },    // ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<5000'],
    http_req_failed: ['rate<0.30'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://gateway-service:8080';

export default function () {
  const res = http.get(`${BASE_URL}/api/courses`);
  check(res, {
    'status not 5xx': (r) => r.status < 500,
  });
  sleep(0.5);
}
