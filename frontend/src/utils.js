export function normalizeStationCode(input) {
  return String(input || '').trim().toUpperCase();
}

export function parsePassengerIds(input) {
  return String(input || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

export function extractOrders(payload) {
  if (Array.isArray(payload)) {
    return payload;
  }
  if (!payload || typeof payload !== 'object') {
    return [];
  }
  if (Array.isArray(payload.records)) {
    return payload.records;
  }
  if (Array.isArray(payload.orders)) {
    return payload.orders;
  }
  return [];
}

export function isFinalOrderStatus(status) {
  return [4, 5, 6].includes(Number(status));
}
