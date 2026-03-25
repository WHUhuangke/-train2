package com.train.ticketing.service;

import com.train.ticketing.domain.SegmentInventory;

import java.time.Duration;

/**
 * 抢票主链路（同步阶段）：
 * 1) 幂等校验
 * 2) Redis 预占座位
 * 3) Redis 预扣库存
 * 4) 创建订单 + 投递 MQ
 * 任一步失败都走补偿。
 */
public class TicketingWorkflowService {
    private final IdempotencyStore idempotencyStore;

    public TicketingWorkflowService(IdempotencyStore idempotencyStore) {
        this.idempotencyStore = idempotencyStore;
    }

    public Result executeGrab(GrabCommand command,
                              SeatPreemptor seatPreemptor,
                              SegmentInventory inventory,
                              OrderGateway orderGateway,
                              MessageGateway messageGateway) {
        String idemKey = "grab:" + command.requestId();
        if (!idempotencyStore.markIfAbsent(idemKey, Duration.ofMinutes(5))) {
            return Result.rejected("重复请求");
        }

        boolean seatPreempted = false;
        boolean stockPreDeducted = false;

        try {
            seatPreempted = seatPreemptor.tryPreempt(command);
            if (!seatPreempted) {
                return Result.rejected("座位预占失败");
            }

            stockPreDeducted = inventory.tryPreDeduct(command.fromStationIndex(), command.toStationIndex());
            if (!stockPreDeducted) {
                seatPreemptor.release(command);
                return Result.rejected("余票不足");
            }

            String orderNo = orderGateway.create(command);
            messageGateway.publishTicketingEvent(orderNo, command);
            return Result.accepted(orderNo);
        } catch (Exception ex) {
            if (stockPreDeducted) {
                inventory.compensate(command.fromStationIndex(), command.toStationIndex());
            }
            if (seatPreempted) {
                seatPreemptor.release(command);
            }
            return Result.rejected("系统异常: " + ex.getMessage());
        }
    }

    public record GrabCommand(String requestId, String trainCode, String seatType,
                              int fromStationIndex, int toStationIndex, String passengerId) {
    }

    public interface SeatPreemptor {
        boolean tryPreempt(GrabCommand command);

        void release(GrabCommand command);
    }

    public interface OrderGateway {
        String create(GrabCommand command);
    }

    public interface MessageGateway {
        void publishTicketingEvent(String orderNo, GrabCommand command);
    }

    public record Result(boolean success, String orderNo, String reason) {
        static Result accepted(String orderNo) {
            return new Result(true, orderNo, null);
        }

        static Result rejected(String reason) {
            return new Result(false, null, reason);
        }
    }
}
