package com.train.ticketing.app.mq;

import com.train.ticketing.app.model.TicketTaskContext;

public record InventoryFinalizeMessage(String messageId, TicketTaskContext context, String seatNo) implements Message {
}
