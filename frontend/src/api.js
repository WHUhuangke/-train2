import { appConfig } from './config.js';
import { getState } from './store.js';

function createHeaders(method, extraHeaders = {}) {
  const headers = { ...extraHeaders };
  const { token } = getState();
  if (token) {
    headers.Authorization = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
  }

  if (method !== 'GET' && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }
  return headers;
}

async function request(path, { method = 'GET', body, headers = {} } = {}) {
  const response = await fetch(`${appConfig.apiBaseUrl}${path}`, {
    method,
    headers: createHeaders(method, headers),
    body: body ? JSON.stringify(body) : undefined,
  });

  const data = await response.json().catch(() => ({}));
  if (!response.ok || data.code !== 0) {
    const message = data.message || `请求失败: ${response.status}`;
    const error = new Error(message);
    error.raw = data;
    throw error;
  }
  return data.data;
}

export function queryAvailability(params) {
  const queryString = new URLSearchParams(params).toString();
  return request(`/tickets/availability?${queryString}`);
}

export function grabOrder(payload, requestId) {
  return request('/orders/grab', {
    method: 'POST',
    headers: { 'X-Request-Id': requestId },
    body: payload,
  });
}

export function listOrders({ pageNo = 1, pageSize = appConfig.defaultPageSize, status } = {}) {
  const qs = new URLSearchParams(
    Object.entries({ pageNo, pageSize, status }).filter(([, value]) => value !== undefined && value !== ''),
  ).toString();
  return request(`/orders?${qs}`);
}

export function getOrder(orderId) {
  return request(`/orders/${orderId}`);
}

export function payOrder(orderId) {
  return request(`/orders/${orderId}/pay`, {
    method: 'POST',
    body: {
      payChannel: 'MOCK',
      clientTs: Date.now(),
    },
  });
}

export function openSse(onMessage, onError) {
  const { token } = getState();
  const url = new URL(`${appConfig.apiBaseUrl}/notifications/sse/subscribe`);
  if (token) {
    url.searchParams.set('token', token.replace(/^Bearer\s+/i, ''));
  }

  const source = new EventSource(url.toString());
  source.onmessage = (event) => {
    try {
      onMessage?.(JSON.parse(event.data));
    } catch {
      onMessage?.({ eventType: 'UNKNOWN', raw: event.data });
    }
  };
  source.onerror = (error) => {
    onError?.(error);
  };
  return source;
}
