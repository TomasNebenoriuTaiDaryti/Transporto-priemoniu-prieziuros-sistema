import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, LOGIN_PAYLOAD, JSON_HEADERS } from './config.js';

export const options = {
  scenarios: {
    ai_test: {
      executor: 'constant-vus',
      vus: 20,
      duration: '30s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<5000'],
    http_req_failed: ['rate<0.10'],
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

  const body = JSON.stringify({
    vehicleId: 8,
    message: 'Kada reikėtų keisti tepalus?',
    history: []
  });

  const aiRes = http.post(`${BASE_URL}/api/ai/chat`, body, {
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  });

  check(aiRes, {
    'ai status 200': (r) => r.status === 200,
    'ai < 5s': (r) => r.timings.duration < 5000,
  });

  sleep(1);
}