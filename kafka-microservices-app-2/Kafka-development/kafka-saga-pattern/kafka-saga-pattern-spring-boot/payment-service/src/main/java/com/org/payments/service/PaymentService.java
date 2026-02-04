package com.org.payments.service;

import com.org.common.dto.Payment;

import java.util.List;

public interface PaymentService {
    List<Payment> findAll();

    Payment process(Payment payment);
}
