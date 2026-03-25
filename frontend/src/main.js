import { getOrder, grabOrder, listOrders, openSse, payOrder, queryAvailability } from './api.js';
import { orderStatusText } from './config.js';
import { getState, setCurrentOrderId, setOrders, setSseInstance, setToken, upsertOrder } from './store.js';

const tokenInput = document.querySelector('#tokenInput');
const saveTokenBtn = document.querySelector('#saveTokenBtn');
const queryForm = document.querySelector('#queryForm');
const trainList = document.querySelector('#trainList');
const queryLoading = document.querySelector('#queryLoading');
const queryError = document.querySelector('#queryError');
const grabForm = document.querySelector('#grabForm');
const grabBtn = document.querySelector('#grabBtn');
const grabError = document.querySelector('#grabError');
const grabResult = document.querySelector('#grabResult');
const refreshOrdersBtn = document.querySelector('#refreshOrdersBtn');
const orderList = document.querySelector('#orderList');
const sseStatus = document.querySelector('#sseStatus');
const connectSseBtn = document.querySelector('#connectSseBtn');
const disconnectSseBtn = document.querySelector('#disconnectSseBtn');

const trainItemTemplate = document.querySelector('#trainItemTemplate');
const orderItemTemplate = document.querySelector('#orderItemTemplate');

let pollingTimer = null;

init();

function init() {
  tokenInput.value = getState().token;
  saveTokenBtn.addEventListener('click', onSaveToken);
  queryForm.addEventListener('submit', onQuery);
  grabForm.addEventListener('submit', onGrab);
  refreshOrdersBtn.addEventListener('click', refreshOrders);
  connectSseBtn.addEventListener('click', connectSse);
  disconnectSseBtn.addEventListener('click', disconnectSse);

  refreshOrders();
}

function onSaveToken() {
  setToken(tokenInput.value.trim());
  toast('Token 已保存');
}

async function onQuery(event) {
  event.preventDefault();
  hide(queryError);
  show(queryLoading);
  trainList.innerHTML = '';

  const formData = new FormData(queryForm);
  const params = Object.fromEntries(formData.entries());

  if (params.fromStationCode === params.toStationCode) {
    queryError.textContent = '起点站和终点站不能相同';
    show(queryError);
    hide(queryLoading);
    return;
  }

  try {
    const data = await queryAvailability(params);
    renderTrains(data.trains || []);
  } catch (error) {
    queryError.textContent = error.message;
    show(queryError);
  } finally {
    hide(queryLoading);
  }
}

function renderTrains(trains) {
  if (trains.length === 0) {
    trainList.innerHTML = '<li class="msg">暂无符合条件的车次</li>';
    return;
  }

  const fragment = document.createDocumentFragment();
  trains.forEach((train) => {
    const node = trainItemTemplate.content.firstElementChild.cloneNode(true);
    node.querySelector('.train-no').textContent = train.trainNo;
    node.querySelector('.time').textContent = `${train.departTime} -> ${train.arriveTime}`;
    node.querySelector('.duration').textContent = `${train.durationMinutes} 分钟`;
    node.querySelector('.seats').textContent = (train.seatInventories || [])
      .map((item) => `${item.seatType}: ${item.leftCount}`)
      .join(' | ');

    node.addEventListener('click', () => fillGrabFormByTrain(train));
    fragment.appendChild(node);
  });
  trainList.appendChild(fragment);
}

function fillGrabFormByTrain(train) {
  grabForm.trainNo.value = train.trainNo;
  grabForm.fromStationCode.value = train.fromStationCode;
  grabForm.toStationCode.value = train.toStationCode;
}

async function onGrab(event) {
  event.preventDefault();
  hide(grabError);
  hide(grabResult);

  const formData = new FormData(grabForm);
  const payload = Object.fromEntries(formData.entries());
  payload.passengerIds = String(payload.passengerIds)
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);

  if (payload.passengerIds.length === 0) {
    grabError.textContent = '请至少填写 1 个乘车人';
    show(grabError);
    return;
  }

  const requestId = crypto.randomUUID();
  grabBtn.disabled = true;

  try {
    const data = await grabOrder(payload, requestId);
    setCurrentOrderId(data.orderId);
    grabResult.textContent = `请求受理成功，订单号：${data.orderId}`;
    show(grabResult);
    await refreshOrders();
    connectSse();
    startOrderPolling();
  } catch (error) {
    grabError.textContent = error.message;
    show(grabError);
  } finally {
    setTimeout(() => {
      grabBtn.disabled = false;
    }, 3000);
  }
}

async function refreshOrders() {
  try {
    const data = await listOrders();
    const orders = data.records || data.orders || [];
    setOrders(orders);
    renderOrders();
  } catch (error) {
    toast(`刷新订单失败：${error.message}`, true);
  }
}

function renderOrders() {
  const { orders } = getState();
  orderList.innerHTML = '';

  if (orders.length === 0) {
    orderList.innerHTML = '<li class="msg">暂无订单</li>';
    return;
  }

  const fragment = document.createDocumentFragment();
  orders.forEach((order) => {
    const node = orderItemTemplate.content.firstElementChild.cloneNode(true);
    node.querySelector('.order-id').textContent = order.orderId;
    node.querySelector('.order-status').textContent = orderStatusText[order.status] || `状态${order.status}`;
    node.querySelector('.order-meta').textContent = `车次 ${order.trainNo || '-'}，席别 ${order.seatType || '-'}`;

    const payBtn = node.querySelector('.pay-btn');
    payBtn.disabled = order.status !== 3;
    payBtn.addEventListener('click', () => onPay(order.orderId));

    node.querySelector('.detail-btn').addEventListener('click', () => refreshOrderDetail(order.orderId));
    fragment.appendChild(node);
  });

  orderList.appendChild(fragment);
}

async function refreshOrderDetail(orderId) {
  try {
    const order = await getOrder(orderId);
    upsertOrder(order);
    renderOrders();
  } catch (error) {
    toast(`刷新详情失败：${error.message}`, true);
  }
}

async function onPay(orderId) {
  try {
    const result = await payOrder(orderId);
    upsertOrder({ orderId, status: result.orderStatus || 4 });
    renderOrders();
    toast(`订单 ${orderId} 支付成功`);
  } catch (error) {
    toast(`支付失败：${error.message}`, true);
  }
}

function connectSse() {
  if (getState().sse) {
    return;
  }

  const sse = openSse(
    (message) => {
      handleSseMessage(message);
    },
    () => {
      sseStatus.textContent = 'SSE 异常中断，将继续轮询订单状态';
      sseStatus.classList.add('error');
    },
  );

  setSseInstance(sse);
  sseStatus.textContent = 'SSE 已连接';
  sseStatus.classList.remove('error');
}

function disconnectSse() {
  if (getState().sse) {
    getState().sse.close();
    setSseInstance(null);
  }
  sseStatus.textContent = 'SSE 已断开';
}

function handleSseMessage(message) {
  if (message.orderId) {
    upsertOrder({ orderId: message.orderId, status: message.toStatus });
    renderOrders();

    const { currentOrderId } = getState();
    if (currentOrderId && currentOrderId === message.orderId) {
      grabResult.textContent = `订单 ${message.orderId} 状态更新为：${orderStatusText[message.toStatus] || message.toStatus}`;
      show(grabResult);
    }
  }
}

function startOrderPolling() {
  stopOrderPolling();
  pollingTimer = setInterval(async () => {
    const { currentOrderId } = getState();
    if (!currentOrderId) return;

    await refreshOrderDetail(currentOrderId);
    const target = getState().orders.find((item) => item.orderId === currentOrderId);
    if (target && [4, 5, 6].includes(target.status)) {
      stopOrderPolling();
    }
  }, 5000);
}

function stopOrderPolling() {
  if (pollingTimer) {
    clearInterval(pollingTimer);
    pollingTimer = null;
  }
}

function show(el) {
  el.classList.remove('hidden');
}

function hide(el) {
  el.classList.add('hidden');
}

function toast(message, isError = false) {
  sseStatus.textContent = message;
  sseStatus.classList.toggle('error', isError);
}
