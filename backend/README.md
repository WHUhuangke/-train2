# Railway Ticketing Backend Core (Java)

该目录已经补齐了可运行的后端核心链路（内存实现版），用于后续替换为 Redis / RabbitMQ / MySQL 微服务实现。

## 1. 微服务边界映射

- 网关服务：接入层（此仓库暂未实现网关代码）
- 会员服务：用户与乘车人（此仓库暂未实现会员代码）
- 业务服务（本目录重点）：
  - 查询模块：`TicketQueryService`
  - 抢票模块：`GrabTicketService`
  - 座位模块：`SeatFinalizeConsumer` + `SeatBitmap` + `SeatAllocator`
  - 库存模块：`InventoryFinalizeConsumer` + `SegmentInventory`
  - 订单模块：`TicketOrder` + `OrderStateMachine` + `PaymentService`
  - 通知模块：`SseNotifier`

## 2. 已实现核心流程

### 2.1 余票查询
- 优先读取内存缓存，miss 后回源查询并回填缓存。

### 2.2 发起抢票（同步）
- 幂等校验（`IdempotencyStore`）
- Redis 预扣库存（用 `SegmentInventory` 模拟）
- 创建订单并置为出票中（`CREATING -> TICKETING`）
- 投递座位落库消息（`SeatFinalizeMessage`）

### 2.3 座位更新（异步消费者）
- 消息幂等（`ConsumeLogRepository`）
- 分布式锁（`DistributedLockManager`）
- 座位位图原子占用（`SeatDbRepository`）
- 成功后投递余票落库消息（`InventoryFinalizeMessage`）
- 失败时订单置失败并回补 Redis 预扣

### 2.4 库存更新（异步消费者）
- 消息幂等
- 扣减 DB 库存（`InventoryDbRepository`）
- 成功：订单 `TICKETING -> WAIT_PAY`，SSE 推送成功通知
- 失败：订单置失败、释放座位、回补 Redis 预扣、SSE 推送失败通知

### 2.5 支付与超时
- 支付回调幂等（`pay:{traceNo}`）
- 成功流转：`WAIT_PAY -> PAY_SUCCESS`
- 超时未支付：`WAIT_PAY -> TICKET_FAILED`

## 3. 关键算法

### 3.1 座位位图算法
- 用 BitSet 存储区间段占用。
- 区间 [from, to) 预占时，检查是否与已有占用重叠；无重叠才可占用。

### 3.2 区间库存算法
- 每段库存独立维护。
- 扣减时先检查区间每段都 > 0，再统一扣减，避免超卖。
- 支持补偿回补。

### 3.3 一致性补偿
- 座位失败：订单失败 + 回补预扣库存。
- 库存失败：订单失败 + 释放座位 + 回补预扣库存。
- 消费异常：写入死信队列。

## 4. 继续落地到真实微服务时替换点

- `InMemoryIdempotencyStore` -> Redis SETNX + EX + Lua
- `MessageBus` -> RabbitMQ（普通队列 + 重试队列 + 死信队列）
- `InMemorySeatDbRepository` / `InMemoryInventoryDbRepository` -> MySQL + MyBatis
- `InMemoryDistributedLockManager` -> Redis 分布式锁（含续期）
- `InMemorySseNotifier` -> 网关 SSE 会话管理
