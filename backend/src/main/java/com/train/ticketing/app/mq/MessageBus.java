package com.train.ticketing.app.mq;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MessageBus {
    private final Queue<SeatFinalizeMessage> seatQueue = new ConcurrentLinkedQueue<>();
    private final Queue<InventoryFinalizeMessage> inventoryQueue = new ConcurrentLinkedQueue<>();
    private final Queue<Message> deadLetterQueue = new ConcurrentLinkedQueue<>();

    public void publishSeat(SeatFinalizeMessage message) {
        seatQueue.offer(message);
    }

    public void publishInventory(InventoryFinalizeMessage message) {
        inventoryQueue.offer(message);
    }

    public SeatFinalizeMessage pollSeat() {
        return seatQueue.poll();
    }

    public InventoryFinalizeMessage pollInventory() {
        return inventoryQueue.poll();
    }

    public void deadLetter(Message message) {
        deadLetterQueue.offer(message);
    }

    public List<Message> deadLetters() {
        return new ArrayList<>(deadLetterQueue);
    }
}
