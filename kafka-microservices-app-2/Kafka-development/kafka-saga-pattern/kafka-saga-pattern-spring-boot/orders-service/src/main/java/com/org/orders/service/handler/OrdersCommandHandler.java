package com.org.orders.service.handler;

import com.org.common.dto.commands.ApproveOrderCommand;
import com.org.common.dto.commands.RejectOrderCommand;
import com.org.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@KafkaListener(topics = {
        "${orders.commands.topic.name}"
})
@Slf4j
@RequiredArgsConstructor
public class OrdersCommandHandler {

    private final OrderService orderService;

    @KafkaHandler
    public void handleCommand(@Payload ApproveOrderCommand approveOrderCommand) {
        orderService.approveOrder(approveOrderCommand.getOrderId());
    }

    @KafkaHandler
    public void handleCommand(@Payload RejectOrderCommand rejectOrderCommand) {
        orderService.rejectOrder(rejectOrderCommand.getOrderId());
    }
}
