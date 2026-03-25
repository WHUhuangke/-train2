const state = {
  token: localStorage.getItem('authToken') || '',
  currentOrderId: '',
  orders: [],
  sse: null,
};

export function getState() {
  return state;
}

export function setToken(token) {
  state.token = token;
  if (token) {
    localStorage.setItem('authToken', token);
  } else {
    localStorage.removeItem('authToken');
  }
}

export function setOrders(orders) {
  state.orders = orders;
}

export function upsertOrder(order) {
  const idx = state.orders.findIndex((item) => item.orderId === order.orderId);
  if (idx === -1) {
    state.orders.unshift(order);
  } else {
    state.orders[idx] = { ...state.orders[idx], ...order };
  }
}

export function setCurrentOrderId(orderId) {
  state.currentOrderId = orderId;
}

export function setSseInstance(sse) {
  state.sse = sse;
}
