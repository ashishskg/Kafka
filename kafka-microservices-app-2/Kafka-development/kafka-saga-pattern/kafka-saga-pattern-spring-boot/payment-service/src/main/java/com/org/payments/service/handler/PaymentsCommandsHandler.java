package com.org.payments.service.handler;

import com.org.common.dto.Payment;
import com.org.common.dto.commands.ProcessPaymentCommand;
import com.org.common.dto.event.PaymentFailedEvent;
import com.org.common.dto.event.PaymentProcessEvent;
import com.org.common.exceptions.CreditCardProcessorUnavailableException;
import com.org.payments.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@KafkaListener(topics = {
        "${payments.commands.topic.name}"
})
@RequiredArgsConstructor
@Slf4j
public class PaymentsCommandsHandler {

    private final PaymentService paymentService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${payments.events.topic.name}")
    private String paymentsEventsTopic;

    @KafkaHandler
    public void handleCommand(@Payload ProcessPaymentCommand paymentCommand) {
        try {
            Payment payment = new Payment(paymentCommand.getOrderId(), paymentCommand.getProductId(),
                    paymentCommand.getProductPrice(), paymentCommand.getProductQuantity());
            Payment processedPayment = paymentService.process(payment);

            // Publich payment processed event (not implemented here)
            PaymentProcessEvent paymentProcessEvent = new PaymentProcessEvent(processedPayment.getOrderId(),
                    processedPayment.getId());

            kafkaTemplate.send(paymentsEventsTopic, paymentProcessEvent);

        } catch (CreditCardProcessorUnavailableException e) {
            log.error(e.getLocalizedMessage(), e);
            PaymentFailedEvent paymentFailedEvent = new PaymentFailedEvent(
                    paymentCommand.getOrderId(),
                    paymentCommand.getProductId(),
                    paymentCommand.getProductQuantity());
            kafkaTemplate.send(paymentsEventsTopic, paymentFailedEvent);
        }
    }
}
