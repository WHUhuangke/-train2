# 铁路抢票系统前端设计与后端接口对接文档

> 角色定位：前端工程设计稿（Web/H5 管理端可复用）
> 
> 目标：给前端落地页面、状态、交互与接口契约，降低前后端联调成本。

---

## 1. 前端总体设计

### 1.1 前端职责边界

前端负责：

- 用户输入与参数校验（日期、站点、席别、乘车人等）
- 页面状态管理（查询中、抢票中、出票中、待支付、成功/失败）
- 接口编排（调用网关 API）
- SSE 实时消息接收并驱动 UI 更新
- 防重复提交（按钮节流、请求幂等键传递）
- 错误提示与用户可恢复路径（重试、刷新订单状态、重新发起）

前端不负责：

- 座位分配算法
- 库存扣减、补偿逻辑
- 订单状态机的最终裁决（以后端为准）

### 1.2 推荐前端技术方案（可选）

- 框架：Vue 3 + TypeScript（或 React + TS，保持同等思想）
- 请求层：Axios（统一拦截器 + 错误码适配）
- 状态管理：Pinia/Redux（登录态、订单态、SSE 连接态）
- 实时通信：原生 `EventSource`（SSE）
- UI：Ant Design Vue / Element Plus（桌面端）
- 构建：Vite

### 1.3 页面信息架构（IA）

1. 登录页（可选，若已有统一 SSO 则跳过）
2. 车票查询页
3. 查询结果页（车次列表 + 席别余票）
4. 抢票确认页（乘车人、席别确认）
5. 抢票处理中页（轮询 + SSE）
6. 订单列表页
7. 订单详情页
8. 支付页（模拟一键支付）
9. 消息中心（可选，统一展示通知）

---

## 2. 核心前端交互流程

### 2.1 余票查询流程

1. 用户输入出发日期、起点、终点。
2. 前端做基础校验（必填、日期合法、起终点不同）。
3. 调用 `查询余票 API`。
4. 展示车次卡片：发/到时间、历时、席别余票、是否可抢。
5. 用户筛选（出发时段、车次类型、席别）。

前端重点：

- 热门查询参数可本地缓存（最近搜索）
- 查询按钮节流（避免快速重复请求）
- 列表骨架屏提升感知速度

### 2.2 发起抢票流程

1. 用户在结果页点击“抢票”。
2. 进入确认页，选择乘车人和席别。
3. 前端生成 `requestId`（UUID）并携带到后端，支持幂等。
4. 调用 `发起抢票 API`。
5. 后端同步返回：
   - 受理成功（进入异步出票）
   - 受理失败（库存不足/参数无效/重复请求）
6. 若受理成功，前端跳转“处理中”并：
   - 建立或复用 SSE 连接
   - 启动订单状态轮询兜底

### 2.3 异步出票与支付流程

1. 前端接收 SSE 事件（如 `ORDER_STATUS_CHANGED`）。
2. 根据状态刷新 UI：
   - `待支付`：展示支付按钮与倒计时
   - `购票失败`：展示失败原因与重试入口
3. 用户点击支付，调用 `支付 API`。
4. 支付成功后进入“支付成功”页。

兜底策略：

- SSE 断开时自动重连 + 指数退避
- 关键页面（处理中、待支付）每 5~10 秒轮询一次订单状态

---

## 3. 前端状态机（与后端状态机对齐）

后端状态定义：

- 1 创建中
- 2 出票中
- 3 待支付
- 4 支付成功
- 5 购票失败
- 6 订单创建失败

前端展示映射建议：

- 1/2：处理中（loading + “正在为您抢票”）
- 3：待支付（按钮可点 + 倒计时）
- 4：成功（票面摘要 + 行程提醒）
- 5/6：失败（失败原因 + 重试）

前端约束：

- 不自行“推断”状态跃迁，始终以后端返回为准
- 收到非法回退（例如 4 -> 3）时，提示异常并主动刷新订单详情

---

## 4. 与后端对接的必要接口清单（网关对前端）

以下为“最小可联调”接口集合。建议统一前缀：`/api`。

## 4.1 鉴权相关

### 4.1.1 获取当前用户信息

- `GET /api/member/me`
- 用途：页面初始化、头部用户信息展示

响应示例：

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "userId": "U10001",
    "name": "张三",
    "mobile": "138****8888"
  },
  "requestId": "...",
  "timestamp": "2026-03-25T10:00:00Z"
}
```

### 4.1.2 查询当前用户乘车人列表

- `GET /api/member/passengers`
- 用途：抢票确认页选择乘车人

---

## 4.2 查询链路

### 4.2.1 查询余票

- `GET /api/tickets/availability`
- Query 参数：
  - `travelDate`：`yyyy-MM-dd`
  - `fromStationCode`
  - `toStationCode`

响应 data 建议：

```json
{
  "trains": [
    {
      "trainNo": "G1234",
      "fromStationCode": "BJP",
      "toStationCode": "SHH",
      "departTime": "08:00",
      "arriveTime": "12:30",
      "durationMinutes": 270,
      "seatInventories": [
        {"seatType": "SECOND_CLASS", "leftCount": 23, "status": "AVAILABLE"},
        {"seatType": "FIRST_CLASS", "leftCount": 0, "status": "SOLD_OUT"}
      ]
    }
  ]
}
```

### 4.2.2 站点列表（可选但强烈建议）

- `GET /api/basic/stations`
- 用途：前端输入联想、减少硬编码

---

## 4.3 抢票链路

### 4.3.1 发起抢票（核心）

- `POST /api/orders/grab`
- Header：
  - `X-Request-Id`: 前端生成 UUID（幂等键）
- Body：

```json
{
  "travelDate": "2026-04-01",
  "trainNo": "G1234",
  "fromStationCode": "BJP",
  "toStationCode": "SHH",
  "seatType": "SECOND_CLASS",
  "passengerIds": ["P1001", "P1002"]
}
```

响应：

```json
{
  "code": 0,
  "message": "ACCEPTED",
  "data": {
    "orderId": "O202603250001",
    "orderStatus": 1,
    "accepted": true
  },
  "requestId": "same-as-x-request-id"
}
```

失败码建议：

- `ORDER_DUPLICATE_REQUEST`
- `INVENTORY_NOT_ENOUGH`
- `PASSENGER_NOT_BELONG_USER`
- `TRAIN_OR_SEAT_INVALID`

### 4.3.2 查询订单详情

- `GET /api/orders/{orderId}`
- 用途：状态展示、SSE 断线兜底

### 4.3.3 查询订单列表

- `GET /api/orders`
- Query：`pageNo,pageSize,status`

---

## 4.4 支付链路

### 4.4.1 发起支付（模拟）

- `POST /api/orders/{orderId}/pay`
- Body（可选）：

```json
{
  "payChannel": "MOCK",
  "clientTs": 1770000000000
}
```

响应：

```json
{
  "code": 0,
  "message": "PAY_SUCCESS",
  "data": {
    "orderId": "O202603250001",
    "orderStatus": 4,
    "paidAt": "2026-03-25T10:02:00Z"
  }
}
```

---

## 4.5 SSE 通知链路（必须）

### 4.5.1 订阅用户消息流

- `GET /api/notifications/sse/subscribe`
- Header：`Authorization`
- `Content-Type: text/event-stream`

事件类型建议：

1. `ORDER_STATUS_CHANGED`
2. `PAYMENT_REMINDER`
3. `ORDER_FAILED`
4. `HEARTBEAT`

事件 data 示例：

```json
{
  "eventType": "ORDER_STATUS_CHANGED",
  "userId": "U10001",
  "orderId": "O202603250001",
  "fromStatus": 2,
  "toStatus": 3,
  "reason": "SEAT_AND_INVENTORY_SUCCESS",
  "occurredAt": "2026-03-25T10:01:00Z"
}
```

前端处理建议：

- 根据 `orderId` 精准更新订单卡片
- 若本地无该订单，则触发订单列表刷新

---

## 5. 通用接口协议约定（必须先对齐）

### 5.1 统一响应结构

```json
{
  "code": 0,
  "message": "OK",
  "data": {},
  "requestId": "...",
  "timestamp": "2026-03-25T10:00:00Z"
}
```

- `code = 0` 表示成功
- 非 0 由业务错误码承载

### 5.2 统一错误码分层

- 网关层：鉴权失败、限流、签名错误
- 业务层：库存不足、状态非法、重复请求
- 系统层：超时、依赖服务不可用

### 5.3 幂等与去重约定

- 前端对“发起抢票”必须传 `X-Request-Id`
- 后端返回重复请求时应回传首次受理的 `orderId`（最佳实践）
- 前端在 3~5 秒内禁用重复提交按钮

### 5.4 时间与时区

- 所有时间戳统一 ISO 8601（UTC）
- 前端展示转换为用户本地时区

---

## 6. 前端数据模型（TypeScript）

```ts
export type OrderStatus = 1 | 2 | 3 | 4 | 5 | 6;

export interface TicketQueryParams {
  travelDate: string;
  fromStationCode: string;
  toStationCode: string;
}

export interface GrabOrderRequest {
  travelDate: string;
  trainNo: string;
  fromStationCode: string;
  toStationCode: string;
  seatType: string;
  passengerIds: string[];
}

export interface OrderDTO {
  orderId: string;
  userId: string;
  status: OrderStatus;
  statusText: string;
  failReason?: string;
  payExpireTime?: string;
}
```

---

## 7. 联调阶段建议（前后端协作清单）

### 7.1 第一阶段（最小闭环）

后端先提供：

1. 查询余票
2. 发起抢票
3. 查询订单详情
4. 支付接口

前端可先用轮询完成闭环（先不依赖 SSE）。

### 7.2 第二阶段（体验增强）

后端补齐：

1. SSE 订阅
2. 支付提醒
3. 失败原因标准化

前端切换为“SSE + 轮询兜底”。

### 7.3 第三阶段（稳定性）

共同压测与验证：

- 高频点击下幂等是否生效
- SSE 断连重连是否丢消息
- 订单状态是否存在逆向回退

---

## 8. 给后端同学的明确对接诉求（可直接发给对方）

1. 请先确定并冻结“统一响应体 + 错误码枚举 + 状态机枚举”。
2. 请在 `发起抢票` 接口支持 `X-Request-Id` 幂等键，并保证重复请求可返回历史 `orderId`。
3. 请提供 SSE 事件标准（事件名、字段、触发时机），至少覆盖状态变更与失败通知。
4. 请在订单详情接口返回：当前状态、失败原因、支付截止时间。
5. 请明确支付超时策略与超时时间，便于前端展示倒计时。

---

## 9. 前端验收标准（建议）

- 能完成从查询 -> 抢票 -> 待支付 -> 支付成功的主链路。
- 失败场景可见明确文案（库存不足、出票失败、超时未支付）。
- 抢票按钮防重复提交生效。
- SSE 正常接收状态变更；断线后可重连。
- 订单列表与订单详情状态一致。

