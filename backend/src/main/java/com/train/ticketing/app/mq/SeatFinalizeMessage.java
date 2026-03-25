package com.train.ticketing.app.mq;

import com.train.ticketing.app.model.TicketTaskContext;

public record SeatFinalizeMessage(String messageId, TicketTaskContext context) implements Message {
}
