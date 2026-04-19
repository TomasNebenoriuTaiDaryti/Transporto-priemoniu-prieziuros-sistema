import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, LOGIN_PAYLOAD, JSON_HEADERS } from './config.js';

export const options = {
  scenarios: {
    refresh_test: {
      executor: 'constant-vus',
      vus: 100,
      duration: '30s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.05'],
  },
};

function loginAndGetToken() {
  const loginRes = http.post(`${BASE_URL}/api/auth/login`, LOGIN_PAYLOAD, {
    headers: JSON_HEADERS,
  });

  check(loginRes, {
    'login status 200': (r) => r.status === 200,
  });

  return loginRes.json('accessToken');
}

export default function () {
  const token = loginAndGetToken();

  const authHeaders = {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  };

  const vehiclesRes = http.get(`${BASE_URL}/api/vehicles/accessible`, authHeaders);
  check(vehiclesRes, {
    'vehicles status 200': (r) => r.status === 200,
    'vehicles refresh < 2s': (r) => r.timings.duration < 2000,
  });

  const docsRes = http.get(`${BASE_URL}/api/vehicles/8/documents`, authHeaders);
  check(docsRes, {
    'documents refresh status 200': (r) => r.status === 200,
    'documents refresh < 2s': (r) => r.timings.duration < 2000,
  });

  const recordsRes = http.get(`${BASE_URL}/api/vehicles/8/records`, authHeaders);
  check(recordsRes, {
    'records refresh status 200': (r) => r.status === 200,
    'records refresh < 2s': (r) => r.timings.duration < 2000,
  });

  sleep(1);
}