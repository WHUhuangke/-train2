export const appConfig = {
  apiBaseUrl: localStorage.getItem('apiBaseUrl') || 'http://localhost:8080/api',
  defaultPageSize: 20,
};

export const orderStatusText = {
  1: '创建中',
  2: '出票中',
  3: '待支付',
  4: '支付成功',
  5: '购票失败',
  6: '订单创建失败',
};
