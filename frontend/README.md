# 前端原型（MVP）

## 启动方式

这是一个无构建依赖的静态原型，可直接本地打开或使用静态服务器运行。

推荐：

```bash
cd frontend
python3 -m http.server 5173
```

然后访问：`http://localhost:5173`

## 已实现能力

- 余票查询表单 + 列表渲染
- 抢票表单 + `X-Request-Id` 幂等请求头
- 订单列表与订单详情刷新
- 支付按钮（仅状态为“待支付”可点击）
- SSE 连接/断开 + 状态更新处理
- 轮询兜底（抢票后每 5 秒刷新当前订单）

## 后端接口约定

默认网关地址：`http://localhost:8080/api`

你可以在浏览器控制台执行以下命令覆盖：

```js
localStorage.setItem('apiBaseUrl', 'http://your-gateway-host/api')
```

刷新页面后生效。

## 注意

- SSE 在浏览器原生 `EventSource` 下不支持自定义 Header，本原型将 token 追加在 query 参数中（`?token=`）。
- 如果后端要求 `Authorization` Header，请让网关支持 cookie 会话，或改为 fetch-stream/短轮询方案。
