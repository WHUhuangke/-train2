# Railway Ticketing Backend Core (Java)

该目录提供后端核心算法与流程骨架，便于后续拆分到微服务：

- 会员服务（用户/乘车人）
- 业务服务（查询、抢票、座位、库存、订单、通知）
- 网关服务（鉴权、限流、防刷、路由）

## 已实现核心

1. **座位位图算法**
   - `SeatBitmap` 使用 BitSet 表示区间占用，支持可售判断、预占与释放。
   - `SeatAllocator` 按座位号排序进行首个可用座位分配。

2. **区间库存算法**
   - `SegmentInventory` 为每个区间段维护可售数。
   - 预扣时必须区间所有段均 > 0，再统一扣减，防止超卖。
   - 支持失败补偿回补。

3. **抢票同步链路（简化版）**
   - `TicketingWorkflowService` 完成：幂等 -> 预占座位 -> 预扣库存 -> 创建订单 -> 投递消息。
   - 任一步失败会执行补偿（释放座位、回补库存）。

4. **订单状态机**
   - `OrderStateMachine` 固化合法状态流转，阻止跳状态与逆向回退。

## 后续落地建议

- 将 `IdempotencyStore` 替换成 Redis SETNX + EX。
- `SeatPreemptor` 接入 Redis Lua 原子脚本 + 分布式锁。
- `OrderGateway` 接入 MySQL/MyBatis。
- `MessageGateway` 接入 RabbitMQ，并增加消息表/消费幂等表。
- 将库存与座位落库拆成两个消费者，失败进入死信并触发对账补偿。
